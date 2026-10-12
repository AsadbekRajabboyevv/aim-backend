package com.onlinemenu.ai.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlinemenu.ai.chat.domain.AiChatMessage;
import com.onlinemenu.ai.chat.domain.AiConversation;
import com.onlinemenu.ai.chat.dto.AiConversationDetailDto;
import com.onlinemenu.ai.chat.dto.AiConversationSummaryDto;
import com.onlinemenu.ai.chat.dto.AiConversationUpdateRequestDto;
import com.onlinemenu.ai.chat.dto.AiStatusDto;
import com.onlinemenu.ai.chat.repository.AiChatMessageRepository;
import com.onlinemenu.ai.chat.repository.AiConversationRepository;
import com.onlinemenu.ai.provider.AiChatProvider;
import com.onlinemenu.ai.provider.AiChatRequest;
import com.onlinemenu.ai.provider.AiChatResponse;
import com.onlinemenu.ai.provider.AiMessage;
import com.onlinemenu.ai.provider.AiProviderException;
import com.onlinemenu.ai.provider.AiProviderRegistry;
import com.onlinemenu.ai.provider.AiRole;
import com.onlinemenu.ai.provider.AiToolCall;
import com.onlinemenu.ai.provider.AiToolOutcome;
import com.onlinemenu.ai.provider.AiToolResult;
import com.onlinemenu.ai.provider.AiToolSpec;
import com.onlinemenu.ai.tool.domain.AiTool;
import com.onlinemenu.ai.tool.domain.AiToolHttpMethod;
import com.onlinemenu.ai.tool.repository.AiToolRepository;
import com.onlinemenu.ai.tool.service.AiToolExecution;
import com.onlinemenu.ai.tool.service.AiToolExecutor;
import com.onlinemenu.ai.tool.service.AiToolSchemas;
import com.onlinemenu.ai.tool.service.AiToolService;
import com.onlinemenu.billing.service.EntitlementService;
import com.onlinemenu.billing.service.LimitKeys;
import com.onlinemenu.common.constant.ExceptionMessageCodes;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.exception.BusinessException;
import com.onlinemenu.common.exception.ResourceNotFoundException;
import com.onlinemenu.common.util.SettingProvider;
import com.onlinemenu.user.domain.User;
import com.onlinemenu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

  private static final int MAX_STEPS = 10;
  private static final int CHAT_MAX_OUTPUT_TOKENS = 4096;
  private static final int TITLE_MAX_OUTPUT_TOKENS = 60;
  private static final int HISTORY_LIMIT = 200;
  private static final int FALLBACK_TITLE_LENGTH = 60;

  private static final String CANCELLED_BY_USER = "The user cancelled this action in the form. Nothing was changed.";
  private static final String SKIPPED_BY_NEW_MESSAGE = "Not performed: the user sent a new message instead of answering. Nothing was changed.";
  private static final String ERROR_ARGUMENTS = "Error: the arguments were not valid JSON. Call the tool again.";
  private static final String ACTIONS_LIMIT_REACHED = "Not performed: the user's plan has used up its AI actions for this month. Nothing was changed. Tell the user they can do it themselves in the panel, or upgrade the plan on the Billing page (Tarif va to'lov).";
  private final AiConversationRepository conversationRepository;
  private final AiChatMessageRepository messageRepository;
  private final AiToolService toolService;
  private final AiToolRepository toolRepository;
  private final AiToolExecutor toolExecutor;
  private final AiProviderRegistry providerRegistry;
  private final SettingProvider settingProvider;
  private final UserRepository userRepository;
  private final AiChatItemMapper itemMapper;
  private final EntitlementService entitlementService;

  private final Map<UUID, Object> locks = new ConcurrentHashMap<>();

  @Override
  public AiStatusDto status(AiChatContext context) {
    return new AiStatusDto(settingProvider.getBoolean(SettingKeys.AI_ENABLED, true),
        providerRegistry.isActiveConfigured(),
        entitlementService.usage(context.userId(), LimitKeys.AI_MESSAGES));
  }

  @Override
  public List<AiConversationSummaryDto> get(AiChatContext context, boolean archived) {
    return conversationRepository
        .findAllByOwnerIdAndArchivedOrderByLastMessageAtDesc(context.userId(), archived,
            PageRequest.of(0, HISTORY_LIMIT))
        .stream().map(this::toSummary).toList();
  }

  @Override
  public AiConversationDetailDto get(AiChatContext context, String slug) {
    var conversation = findOrThrow(context, slug);
    return toDetail(context, conversation, history(conversation));
  }

  @Override
  public AiConversationDetailDto create(AiChatContext context, String text,
      AiChatListener listener) {
    ensureAvailable();
    entitlementService.consume(context.userId(), LimitKeys.AI_MESSAGES);
    var conversation = AiConversation.builder()
        .ownerId(context.userId())
        .lastMessageAt(Instant.now())
        .build();
    conversation.setSlug(newSlug());
    conversation = conversationRepository.save(conversation);
    listener.onConversation(conversation.getSlug());
    return sendMessage(context, conversation.getSlug(), text, listener);
  }

  @Override
  public AiConversationDetailDto send(AiChatContext context, String slug, String text,
      AiChatListener listener) {
    ensureAvailable();
    entitlementService.consume(context.userId(), LimitKeys.AI_MESSAGES);
    return sendMessage(context, slug, text, listener);
  }

  private AiConversationDetailDto sendMessage(AiChatContext context, String slug, String text,
      AiChatListener listener) {
    var conversation = findOrThrow(context, slug);
    synchronized (lock(conversation)) {
      conversation = reload(conversation);
      var messages = history(conversation);

      var results = new ArrayList<>(conversation.getPendingResults());
      for (var call : unresolvedCalls(conversation, messages)) {
        results.add(new AiToolResult(call.id(), call.name(), SKIPPED_BY_NEW_MESSAGE, false,
            AiToolOutcome.CANCELLED, null));
      }
      conversation.setPendingResults(new ArrayList<>());
      append(conversation, messages, AiMessage.user(text.trim(), results));

      runAgent(context, conversation, messages, listener);
      titleIfMissing(conversation, messages);
      return toDetail(context, conversation, messages);
    }
  }

  @Override
  public AiConversationDetailDto decide(AiChatContext context, String slug, String toolCallId,
      boolean approved,
      JsonNode arguments, AiChatListener listener) {
    ensureAvailable();
    var conversation = findOrThrow(context, slug);
    synchronized (lock(conversation)) {
      conversation = reload(conversation);
      var messages = history(conversation);
      var unresolved = unresolvedCalls(conversation, messages);
      var call = unresolved.stream().filter(c -> c.id().equals(toolCallId)).findFirst()
          .orElseThrow(() -> new BusinessException(ExceptionMessageCodes.AI_ACTION_NOT_PENDING,
              HttpStatus.CONFLICT));

      var results = new ArrayList<>(conversation.getPendingResults());
      results.add(resolve(context, availableTools(context), call, approved, arguments));
      if (unresolved.size() > 1) {
        conversation.setPendingResults(results);
        conversationRepository.save(conversation);
        return toDetail(context, conversation, messages);
      }
      conversation.setPendingResults(new ArrayList<>());
      append(conversation, messages, AiMessage.user(null, results));
      runAgent(context, conversation, messages, listener);
      return toDetail(context, conversation, messages);
    }
  }

  @Override
  public AiConversationDetailDto decideAll(AiChatContext context, String slug, boolean approved,
      AiChatListener listener) {
    ensureAvailable();
    var conversation = findOrThrow(context, slug);
    synchronized (lock(conversation)) {
      conversation = reload(conversation);
      var messages = history(conversation);
      var unresolved = unresolvedCalls(conversation, messages);
      if (unresolved.isEmpty()) {
        throw new BusinessException(ExceptionMessageCodes.AI_ACTION_NOT_PENDING,
            HttpStatus.CONFLICT);
      }
      var tools = availableTools(context);
      var results = new ArrayList<>(conversation.getPendingResults());
      for (var call : unresolved) {
        results.add(resolve(context, tools, call, approved, null));
      }
      conversation.setPendingResults(new ArrayList<>());
      append(conversation, messages, AiMessage.user(null, results));
      runAgent(context, conversation, messages, listener);
      return toDetail(context, conversation, messages);
    }
  }

  private AiToolResult resolve(AiChatContext context, Map<String, AiTool> tools, AiToolCall call,
      boolean approved, JsonNode arguments) {
    var tool = tools.get(call.name());
    if (tool == null) {
      return AiToolResult.failed(call, "Error: this tool is no longer available.");
    }
    if (!approved) {
      return new AiToolResult(call.id(), call.name(), CANCELLED_BY_USER, false,
          AiToolOutcome.CANCELLED, call.arguments());
    }
    if (!withinActionLimit(context, tool)) {
      return AiToolResult.failed(call, ACTIONS_LIMIT_REACHED);
    }
    var confirmed = arguments != null && arguments.isObject() ? arguments : call.arguments();
    var execution = toolExecutor.execute(tool, confirmed, context.toolContext());
    return new AiToolResult(call.id(), call.name(), execution.content(), !execution.success(),
        execution.success() ? AiToolOutcome.EXECUTED : AiToolOutcome.FAILED, confirmed);
  }

  @Override
  public AiConversationSummaryDto update(AiChatContext context, String slug,
      AiConversationUpdateRequestDto dto) {
    var conversation = findOrThrow(context, slug);
    if (StringUtils.hasText(dto.title())) {
      conversation.setTitle(dto.title().trim());
    }
    if (dto.archived() != null) {
      conversation.setArchived(dto.archived());
    }
    return toSummary(conversationRepository.save(conversation));
  }

  @Override
  public void delete(AiChatContext context, String slug) {
    var conversation = findOrThrow(context, slug);
    conversationRepository.delete(conversation);
    locks.remove(conversation.getId());
  }


  private void runAgent(AiChatContext context, AiConversation conversation,
      List<AiChatMessage> messages,
      AiChatListener listener) {
    var provider = providerRegistry.active();
    var model = providerRegistry.model(provider);
    var fullName = userRepository.findById(context.userId()).map(User::getFullName).orElse("");
    var systemPrompt = AiPrompts.system(fullName, context.role(), LocalDate.now(AiPrompts.ZONE));
    var tools = availableTools(context);
    var specs = tools.values().stream()
        .map(tool -> new AiToolSpec(tool.getName(), tool.getDescription(),
            AiToolSchemas.forModel(tool.getInputSchema())))
        .toList();

    for (var step = 0; step < MAX_STEPS; step++) {
      var response = call(provider, listener, new AiChatRequest(
          model, systemPrompt, contents(messages), specs, CHAT_MAX_OUTPUT_TOKENS));
      var assistant = AiMessage.assistant(response.text(), response.toolCalls());
      if (!assistant.hasText() && assistant.toolCalls().isEmpty()) {
        return;
      }
      append(conversation, messages, assistant);
      if (assistant.toolCalls().isEmpty()) {
        return;
      }

      List<AiToolResult> results = new ArrayList<>();
      var awaitingUser = false;
      for (var call : assistant.toolCalls()) {
        var tool = tools.get(call.name());
        if (tool == null) {
          results.add(AiToolResult.failed(call, STR."Error: unknown tool '\{call.name()}'."));
        } else if (call.arguments() == null) {
          results.add(AiToolResult.failed(call, ERROR_ARGUMENTS));
        } else if (tool.isRequiresConfirmation()) {
          awaitingUser = true;
        } else if (!withinActionLimit(context, tool)) {
          results.add(AiToolResult.failed(call, ACTIONS_LIMIT_REACHED));
        } else {
          listener.onTool(context.isSuperAdmin() ? STR."\{tool.getTitle()} (\{tool.getName()})"
              : null);
          var execution = toolExecutor.execute(tool, call.arguments(), context.toolContext());
          results.add(
              new AiToolResult(call.id(), call.name(), execution.content(), !execution.success(),
                  execution.success() ? AiToolOutcome.EXECUTED : AiToolOutcome.FAILED, null));
        }
      }
      if (awaitingUser) {
        conversation.setPendingResults(results);
        conversationRepository.save(conversation);
        return;
      }
      append(conversation, messages, AiMessage.user(null, results));
    }
    log.warn("AI conversation {} stopped after {} tool steps", conversation.getSlug(), MAX_STEPS);
  }

  private AiChatResponse call(AiChatProvider provider, AiChatListener listener,
      AiChatRequest request) {
    try {
      return provider.chatStream(request, listener::onText);
    } catch (AiProviderException ex) {
      log.error("AI provider {} failed: {}", provider.id(), ex.getMessage());
      throw switch (ex.getKind()) {
        case RATE_LIMITED -> new BusinessException(ExceptionMessageCodes.AI_RATE_LIMITED,
            HttpStatus.TOO_MANY_REQUESTS);
        case MODEL_NOT_FOUND -> new BusinessException(ExceptionMessageCodes.AI_MODEL_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            providerRegistry.model(provider));
        case UNAVAILABLE -> new BusinessException(ExceptionMessageCodes.AI_PROVIDER_BUSY,
            HttpStatus.SERVICE_UNAVAILABLE);
        case OTHER ->
            new BusinessException(ExceptionMessageCodes.AI_PROVIDER_FAILED, HttpStatus.BAD_GATEWAY);
      };
    }
  }

  private void titleIfMissing(AiConversation conversation, List<AiChatMessage> messages) {
    if (conversation.getTitle() != null) {
      return;
    }
    var firstUser = messages.stream().map(AiChatMessage::getContent)
        .filter(m -> m.role() == AiRole.USER && m.hasText()).map(AiMessage::text).findFirst()
        .orElse(null);
    if (firstUser == null) {
      return;
    }
    var firstReply = messages.stream().map(AiChatMessage::getContent)
        .filter(m -> m.role() == AiRole.ASSISTANT && m.hasText()).map(AiMessage::text).findFirst()
        .orElse("");
    String title;
    try {
      var provider = providerRegistry.active();
      var response = provider.chat(new AiChatRequest(
          providerRegistry.titleModel(provider),
          AiPrompts.TITLE_SYSTEM,
          List.of(AiMessage.user("User: " + firstUser + "\n\nAssistant: " + firstReply)),
          List.of(),
          TITLE_MAX_OUTPUT_TOKENS));
      title = clean(response.text());
    } catch (RuntimeException ex) {
      log.warn("AI title generation failed: {}", ex.getMessage());
      title = null;
    }
    conversation.setTitle(title != null ? title : abbreviate(firstUser));
    conversationRepository.save(conversation);
  }

  /** Counts a data-changing tool call against the plan's monthly AI actions; reads are free. */
  private boolean withinActionLimit(AiChatContext context, AiTool tool) {
    return tool.getHttpMethod() == AiToolHttpMethod.GET
        || entitlementService.tryConsume(context.userId(), LimitKeys.AI_ACTIONS);
  }

  private void ensureAvailable() {
    if (!settingProvider.getBoolean(SettingKeys.AI_ENABLED, true)) {
      throw new BusinessException(ExceptionMessageCodes.AI_DISABLED,
          HttpStatus.SERVICE_UNAVAILABLE);
    }
    providerRegistry.active();
  }

  private Map<String, AiTool> availableTools(AiChatContext context) {
    return toolService.getAvailable(context.role()).stream()
        .collect(Collectors.toMap(AiTool::getName, Function.identity(), (a, b) -> a,
            java.util.LinkedHashMap::new));
  }

  private List<AiToolCall> unresolvedCalls(AiConversation conversation,
      List<AiChatMessage> messages) {
    if (messages.isEmpty()) {
      return List.of();
    }
    var last = messages.get(messages.size() - 1).getContent();
    if (last.role() != AiRole.ASSISTANT) {
      return List.of();
    }
    var answered = conversation.getPendingResults().stream().map(AiToolResult::toolCallId)
        .collect(Collectors.toSet());
    return last.toolCalls().stream().filter(call -> !answered.contains(call.id())).toList();
  }

  private void append(AiConversation conversation, List<AiChatMessage> messages,
      AiMessage content) {
    var seq = messages.isEmpty() ? 0 : messages.get(messages.size() - 1).getSeq() + 1;
    messages.add(messageRepository.save(AiChatMessage.builder()
        .conversationId(conversation.getId())
        .seq(seq)
        .content(content)
        .build()));
    conversation.setLastMessageAt(Instant.now());
    conversationRepository.save(conversation);
  }

  private List<AiChatMessage> history(AiConversation conversation) {
    return new ArrayList<>(
        messageRepository.findAllByConversationIdOrderBySeqAsc(conversation.getId()));
  }

  private static List<AiMessage> contents(List<AiChatMessage> messages) {
    return messages.stream().map(AiChatMessage::getContent).toList();
  }

  private AiConversationDetailDto toDetail(AiChatContext context, AiConversation conversation,
      List<AiChatMessage> messages) {
    var allTools = toolRepository.findAll().stream()
        .collect(Collectors.toMap(AiTool::getName, Function.identity()));
    var awaiting = unresolvedCalls(conversation, messages).stream()
        .anyMatch(call -> allTools.containsKey(call.name()) && allTools.get(call.name())
            .isRequiresConfirmation());
    return new AiConversationDetailDto(conversation.getSlug(), conversation.getTitle(),
        conversation.isArchived(),
        conversation.getLastMessageAt(),
        itemMapper.toItems(conversation, messages, allTools, context.isSuperAdmin()),
        awaiting);
  }

  private AiConversationSummaryDto toSummary(AiConversation conversation) {
    return new AiConversationSummaryDto(conversation.getSlug(), conversation.getTitle(),
        conversation.isArchived(),
        conversation.getLastMessageAt(), conversation.getCreatedAt());
  }

  private AiConversation findOrThrow(AiChatContext context, String slug) {
    return conversationRepository.findBySlugAndOwnerId(slug, context.userId())
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.AI_CONVERSATION_NOT_FOUND,
                slug));
  }

  private AiConversation reload(AiConversation conversation) {
    return conversationRepository.findById(conversation.getId())
        .orElseThrow(
            () -> new ResourceNotFoundException(ExceptionMessageCodes.AI_CONVERSATION_NOT_FOUND,
                conversation.getSlug()));
  }

  private Object lock(AiConversation conversation) {
    return locks.computeIfAbsent(conversation.getId(), id -> new Object());
  }

  private String newSlug() {
    String slug;
    do {
      slug = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    } while (conversationRepository.existsBySlug(slug));
    return slug;
  }

  private static String clean(String title) {
    if (title == null) {
      return null;
    }
    var cleaned = title.lines().findFirst().orElse("").replaceAll("^[\"'«“]+|[\"'»”.]+$", "")
        .trim();
    return cleaned.isEmpty() ? null
        : (cleaned.length() > 200 ? cleaned.substring(0, 200) : cleaned);
  }

  private static String abbreviate(String text) {
    var single = text.replaceAll("\\s+", " ").trim();
    return single.length() <= FALLBACK_TITLE_LENGTH ? single
        : single.substring(0, FALLBACK_TITLE_LENGTH).trim() + "…";
  }
}
