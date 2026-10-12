package com.onlinemenu.ai.provider.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.onlinemenu.ai.config.AiProperties;
import com.onlinemenu.ai.provider.AiChatProvider;
import com.onlinemenu.ai.provider.AiChatRequest;
import com.onlinemenu.ai.provider.AiChatResponse;
import com.onlinemenu.ai.provider.AiFinishReason;
import com.onlinemenu.ai.provider.AiMessage;
import com.onlinemenu.ai.provider.AiProviderException;
import com.onlinemenu.ai.provider.AiRetry;
import com.onlinemenu.ai.provider.AiRole;
import com.onlinemenu.ai.provider.AiToolCall;
import com.onlinemenu.ai.provider.AiToolResult;
import com.onlinemenu.ai.provider.AiToolSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OpenAiChatProvider implements AiChatProvider {

  public static final String ID = "openai";
  private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

  private final ObjectMapper objectMapper;
  private final AiProperties.Provider config;
  private final RestClient restClient;

  public OpenAiChatProvider(AiProperties properties, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.config = properties.provider(ID);
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    requestFactory.setReadTimeout(Duration.ofSeconds(properties.requestTimeoutSeconds()));
    this.restClient = RestClient.builder()
        .baseUrl(StringUtils.hasText(config.baseUrl()) ? config.baseUrl() : DEFAULT_BASE_URL)
        .requestFactory(requestFactory)
        .build();
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String displayName() {
    return "OpenAI";
  }

  @Override
  public String defaultModel() {
    return "gpt-4.1-mini";
  }

  @Override
  public String defaultTitleModel() {
    return "gpt-4.1-mini";
  }

  @Override
  public boolean isConfigured() {
    return StringUtils.hasText(config.apiKey());
  }

  @Override
  public AiChatResponse chat(AiChatRequest request) {
    ObjectNode body = buildBody(request);
    JsonNode response = AiRetry.onUnavailable("OpenAI", () -> send(() -> restClient.post()
        .uri("/chat/completions")
        .header("Authorization", "Bearer " + config.apiKey())
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)));
    return parse(response);
  }

  @Override
  public AiChatResponse chatStream(AiChatRequest request,
      java.util.function.Consumer<String> onText) {
    ObjectNode body = buildBody(request);
    body.put("stream", true);
    return AiRetry.onUnavailable("OpenAI", () -> stream(body, onText));
  }

  private AiChatResponse stream(ObjectNode body, java.util.function.Consumer<String> onText) {
    try {
      return restClient.post()
          .uri("/chat/completions")
          .header("Authorization", "Bearer " + config.apiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .exchange((req, res) -> {
            if (res.getStatusCode().isError()) {
              int status = res.getStatusCode().value();
              String raw = new String(res.getBody().readAllBytes(),
                  java.nio.charset.StandardCharsets.UTF_8);
              throw new AiProviderException("OpenAI HTTP " + status + ": " + abbreviate(raw),
                  AiProviderException.kindOf(status));
            }
            StringBuilder content = new StringBuilder();
            String refusal = null;
            String finishReason = null;
            java.util.TreeMap<Integer, ObjectNode> calls = new java.util.TreeMap<>();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(res.getBody(),
                    java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
              if (!line.startsWith("data:")) {
                continue;
              }
              String json = line.substring("data:".length()).trim();
              if (json.isEmpty() || json.equals("[DONE]")) {
                continue;
              }
              JsonNode choice = objectMapper.readTree(json).path("choices").path(0);
              JsonNode delta = choice.path("delta");
              if (delta.path("content").isTextual()) {
                content.append(delta.get("content").asText());
                onText.accept(delta.get("content").asText());
              }
              if (delta.path("refusal").isTextual()) {
                refusal = (refusal == null ? "" : refusal) + delta.get("refusal").asText();
              }
              for (JsonNode fragment : delta.path("tool_calls")) {
                ObjectNode call = calls.computeIfAbsent(fragment.path("index").asInt(), index -> {
                  ObjectNode created = objectMapper.createObjectNode().put("type", "function");
                  created.putObject("function").put("name", "").put("arguments", "");
                  return created;
                });
                if (fragment.hasNonNull("id")) {
                  call.put("id", fragment.get("id").asText());
                }
                ObjectNode function = (ObjectNode) call.get("function");
                JsonNode fn = fragment.path("function");
                if (fn.hasNonNull("name")) {
                  function.put("name", function.get("name").asText() + fn.get("name").asText());
                }
                if (fn.hasNonNull("arguments")) {
                  function.put("arguments",
                      function.get("arguments").asText() + fn.get("arguments").asText());
                }
              }
              if (choice.hasNonNull("finish_reason")) {
                finishReason = choice.get("finish_reason").asText();
              }
            }
            ObjectNode combined = objectMapper.createObjectNode();
            ObjectNode choice = combined.putArray("choices").addObject();
            ObjectNode message = choice.putObject("message");
            if (!content.isEmpty()) {
              message.put("content", content.toString());
            }
            if (refusal != null) {
              message.put("refusal", refusal);
            }
            if (!calls.isEmpty()) {
              calls.values().forEach(message.putArray("tool_calls")::add);
            }
            if (finishReason != null) {
              choice.put("finish_reason", finishReason);
            }
            return parse(combined);
          });
    } catch (RestClientException ex) {
      throw new AiProviderException("OpenAI request failed: " + ex.getMessage(), ex);
    }
  }

  @Override
  public List<String> listModels() {
    JsonNode response = AiRetry.onUnavailable("OpenAI", () -> send(() -> restClient.get()
        .uri("/models")
        .header("Authorization", "Bearer " + config.apiKey())));
    List<String> models = new ArrayList<>();
    for (JsonNode model : response.path("data")) {
      String id = model.path("id").asText("");
      if (id.startsWith("gpt-") || id.matches("^o\\d.*")) {
        models.add(id);
      }
    }
    models.sort(java.util.Comparator.naturalOrder());
    return models;
  }

  private JsonNode send(java.util.function.Supplier<RestClient.RequestHeadersSpec<?>> request) {
    try {
      return request.get().exchange((req, res) -> {
        String raw = new String(res.getBody().readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8);
        if (res.getStatusCode().isError()) {
          int status = res.getStatusCode().value();
          throw new AiProviderException("OpenAI HTTP " + status + ": " + abbreviate(raw),
              AiProviderException.kindOf(status));
        }
        return objectMapper.readTree(raw);
      });
    } catch (RestClientException ex) {
      throw new AiProviderException("OpenAI request failed: " + ex.getMessage(), ex);
    }
  }

  private ObjectNode buildBody(AiChatRequest request) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", request.model());
    body.put("max_completion_tokens", request.maxOutputTokens());

    ArrayNode messages = body.putArray("messages");
    if (StringUtils.hasText(request.systemPrompt())) {
      messages.addObject().put("role", "system").put("content", request.systemPrompt());
    }
    for (AiMessage message : request.messages()) {
      appendMessage(messages, message);
    }

    if (!request.tools().isEmpty()) {
      ArrayNode tools = body.putArray("tools");
      for (AiToolSpec tool : request.tools()) {
        ObjectNode function = tools.addObject().put("type", "function").putObject("function");
        function.put("name", tool.name());
        function.put("description", tool.description());
        function.set("parameters", tool.inputSchema());
      }
    }
    return body;
  }

  private void appendMessage(ArrayNode messages, AiMessage message) {
    if (message.role() == AiRole.USER) {
      for (AiToolResult result : message.toolResults()) {
        messages.addObject()
            .put("role", "tool")
            .put("tool_call_id", result.toolCallId())
            .put("content", result.content());
      }
      if (message.hasText()) {
        messages.addObject().put("role", "user").put("content", message.text());
      }
      return;
    }
    ObjectNode assistant = messages.addObject().put("role", "assistant");
    if (message.hasText()) {
      assistant.put("content", message.text());
    } else {
      assistant.putNull("content");
    }
    if (!message.toolCalls().isEmpty()) {
      ArrayNode calls = assistant.putArray("tool_calls");
      for (AiToolCall call : message.toolCalls()) {
        ObjectNode function = calls.addObject().put("id", call.id()).put("type", "function")
            .putObject("function");
        function.put("name", call.name());
        function.put("arguments", call.arguments() == null ? "{}" : call.arguments().toString());
      }
    }
  }

  private AiChatResponse parse(JsonNode response) {
    JsonNode choice = response.path("choices").path(0);
    if (choice.isMissingNode()) {
      throw new AiProviderException(
          "OpenAI response has no choices: " + abbreviate(response.toString()));
    }
    JsonNode message = choice.path("message");

    List<AiToolCall> toolCalls = new ArrayList<>();
    for (JsonNode call : message.path("tool_calls")) {
      JsonNode function = call.path("function");
      toolCalls.add(new AiToolCall(call.path("id").asText(), function.path("name").asText(),
          parseArguments(function.path("arguments").asText("{}"))));
    }

    String refusal = message.path("refusal").isTextual() ? message.path("refusal").asText() : null;
    String text = message.path("content").isTextual() ? message.path("content").asText() : refusal;
    AiFinishReason finishReason = switch (choice.path("finish_reason").asText("")) {
      case "stop" -> AiFinishReason.END;
      case "tool_calls" -> AiFinishReason.TOOL_CALLS;
      case "length" -> AiFinishReason.MAX_TOKENS;
      case "content_filter" -> AiFinishReason.REFUSED;
      default -> toolCalls.isEmpty() ? AiFinishReason.OTHER : AiFinishReason.TOOL_CALLS;
    };
    if (refusal != null) {
      finishReason = AiFinishReason.REFUSED;
    }
    return new AiChatResponse(text, toolCalls, finishReason);
  }

  private JsonNode parseArguments(String arguments) {
    try {
      JsonNode node = objectMapper.readTree(arguments);
      return node != null && node.isObject() ? node : null;
    } catch (JsonProcessingException ex) {
      log.warn("OpenAI returned malformed tool arguments: {}", abbreviate(arguments));
      return null;
    }
  }

  private static String abbreviate(String value) {
    return value == null || value.length() <= 500 ? value : value.substring(0, 500) + "...";
  }
}
