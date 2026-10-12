package com.onlinemenu.ai.chat.controller;

import com.onlinemenu.ai.chat.dto.AiActionDecisionRequestDto;
import com.onlinemenu.ai.chat.dto.AiConversationDetailDto;
import com.onlinemenu.ai.chat.dto.AiConversationSummaryDto;
import com.onlinemenu.ai.chat.dto.AiConversationUpdateRequestDto;
import com.onlinemenu.ai.chat.dto.AiSendMessageRequestDto;
import com.onlinemenu.ai.chat.dto.AiStatusDto;
import com.onlinemenu.ai.chat.service.AiChatContext;
import com.onlinemenu.ai.chat.service.AiChatListener;
import com.onlinemenu.ai.chat.service.AiChatService;
import com.onlinemenu.billing.service.PlanLimitExceededException;
import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.SecurityConstants;
import com.onlinemenu.common.dto.BaseResponse;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.util.ExceptionUtils;
import com.onlinemenu.common.util.LanguageContextHolder;
import com.onlinemenu.security.UserPrincipal;
import com.onlinemenu.user.domain.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@PreAuthorize("""
    hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_SUPER_ADMIN)
    or hasRole(T(com.onlinemenu.common.constant.SecurityConstants).ROLE_CAFE_ADMIN)
    """)
public class AiChatController {

  private static final String DEFAULT_LANGUAGE = "uz";
  private static final long STREAM_TIMEOUT_MS = 5 * 60 * 1000L;

  private final AiChatService chatService;
  private final ExecutorService aiStreamExecutor;
  private final ExceptionUtils exceptionUtils;

  @GetMapping("/status")
  public BaseResponse<AiStatusDto> status(@AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest request) {
    return BaseResponse.success(chatService.status(context(principal, request)));
  }

  @GetMapping("/conversations")
  public BaseResponse<List<AiConversationSummaryDto>> get(
      @RequestParam(defaultValue = "false") boolean archived,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest request) {
    return BaseResponse.success(chatService.get(context(principal, request), archived));
  }

  @GetMapping("/conversations/{slug}")
  public BaseResponse<AiConversationDetailDto> get(@PathVariable String slug,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest request) {
    return BaseResponse.success(chatService.get(context(principal, request), slug));
  }

  @PostMapping("/conversations")
  public BaseResponse<AiConversationDetailDto> create(
      @Valid @RequestBody AiSendMessageRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest request) {
    return BaseResponse.success(chatService.create(context(principal, request), dto.text()));
  }

  @PostMapping("/conversations/{slug}/messages")
  public BaseResponse<AiConversationDetailDto> send(@PathVariable String slug,
      @Valid @RequestBody AiSendMessageRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest request) {
    return BaseResponse.success(chatService.send(context(principal, request), slug, dto.text()));
  }

  @PostMapping("/conversations/{slug}/actions/{toolCallId}")
  public BaseResponse<AiConversationDetailDto> decide(@PathVariable String slug,
      @PathVariable String toolCallId,
      @RequestBody AiActionDecisionRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest request) {
    return BaseResponse.success(
        chatService.decide(context(principal, request), slug, toolCallId, dto.approved(),
            dto.arguments()));
  }

  @PostMapping("/conversations/{slug}/actions")
  public BaseResponse<AiConversationDetailDto> decideAll(@PathVariable String slug,
      @RequestBody AiActionDecisionRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest request) {
    return BaseResponse.success(
        chatService.decideAll(context(principal, request), slug, dto.approved()));
  }

  @PostMapping(value = "/conversations/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter createStream(@Valid @RequestBody AiSendMessageRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal, HttpServletRequest request) {
    AiChatContext context = context(principal, request);
    return stream(context, listener -> chatService.create(context, dto.text(), listener));
  }

  @PostMapping(value = "/conversations/{slug}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter sendStream(@PathVariable String slug,
      @Valid @RequestBody AiSendMessageRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal, HttpServletRequest request) {
    AiChatContext context = context(principal, request);
    return stream(context, listener -> chatService.send(context, slug, dto.text(), listener));
  }

  @PostMapping(value = "/conversations/{slug}/actions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter decideAllStream(@PathVariable String slug,
      @RequestBody AiActionDecisionRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal, HttpServletRequest request) {
    AiChatContext context = context(principal, request);
    return stream(context,
        listener -> chatService.decideAll(context, slug, dto.approved(), listener));
  }

  @PostMapping(value = "/conversations/{slug}/actions/{toolCallId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter decideStream(@PathVariable String slug, @PathVariable String toolCallId,
      @RequestBody AiActionDecisionRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal, HttpServletRequest request) {
    AiChatContext context = context(principal, request);
    return stream(context, listener ->
        chatService.decide(context, slug, toolCallId, dto.approved(), dto.arguments(), listener));
  }

  private SseEmitter stream(AiChatContext context,
      Function<AiChatListener, AiConversationDetailDto> turn) {
    SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
    AiChatListener listener = new AiChatListener() {
      @Override
      public void onConversation(String slug) {
        event(emitter, "conversation", Map.of("slug", slug));
      }

      @Override
      public void onText(String delta) {
        event(emitter, "delta", Map.of("text", delta));
      }

      @Override
      public void onTool(String label) {
        event(emitter, "tool", label == null ? Map.of() : Map.of("label", label));
      }
    };
    aiStreamExecutor.execute(() -> {
      LanguageContextHolder.set(context.language());
      try {
        event(emitter, "done", turn.apply(listener));
      } catch (PlanLimitExceededException ex) {
        event(emitter, "error", Map.of("message", exceptionUtils.resolveMessage(ex),
            "status", ex.getStatus().value(), "limit", ex.getDetails()));
      } catch (BusinessException ex) {
        event(emitter, "error",
            Map.of("message", exceptionUtils.resolveMessage(ex), "status", ex.getStatus().value()));
      } catch (RuntimeException ex) {
        log.error("AI chat stream failed", ex);
        event(emitter, "error",
            Map.of("message", exceptionUtils.resolveMessage(ExceptionMessageCodes.GENERIC_ERROR),
                "status", 500));
      } finally {
        LanguageContextHolder.clear();
        emitter.complete();
      }
    });
    return emitter;
  }

  private static void event(SseEmitter emitter, String name, Object data) {
    try {
      emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
    } catch (IOException | IllegalStateException ex) {
      log.error("AI chat stream failed", ex);
    }
  }

  @PatchMapping("/conversations/{slug}")
  public BaseResponse<AiConversationSummaryDto> update(@PathVariable String slug,
      @Valid @RequestBody AiConversationUpdateRequestDto dto,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest request) {
    return BaseResponse.success(chatService.update(context(principal, request), slug, dto));
  }

  @DeleteMapping("/conversations/{slug}")
  public BaseResponse<Void> delete(@PathVariable String slug,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest request) {
    chatService.delete(context(principal, request), slug);
    return BaseResponse.success(null);
  }

  private AiChatContext context(UserPrincipal principal, HttpServletRequest request) {
    String header = request.getHeader(SecurityConstants.AUTH_HEADER);
    String token = header != null && header.startsWith(SecurityConstants.TOKEN_PREFIX)
        ? header.substring(SecurityConstants.TOKEN_PREFIX.length())
        : "";
    String language = request.getHeader(AppConstants.LANGUAGE_HEADER);
    return new AiChatContext(principal.getId(), Role.valueOf(principal.getRole()), token,
        StringUtils.hasText(language) ? language : DEFAULT_LANGUAGE);
  }
}
