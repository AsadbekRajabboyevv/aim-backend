package com.onlinemenu.ai.provider.gemini;

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
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;


@Component
public class GeminiChatProvider implements AiChatProvider {

  public static final String ID = "gemini";
  private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
  private static final String LOCAL_ID_PREFIX = "gemini-local-";

  private static final int MIN_OUTPUT_TOKENS = 8192;
  private static final Set<String> SCHEMA_KEYS = Set.of(
      "type", "format", "title", "description", "nullable", "enum", "items", "properties",
      "required",
      "minimum", "maximum", "minItems", "maxItems", "minLength", "maxLength", "pattern", "anyOf");


  private static final List<String> NON_CHAT_MODEL_MARKERS = List.of(
      "tts", "image", "lyria", "transcribe", "robotics", "computer-use", "nano-banana",
      "deep-research", "antigravity");

  private final ObjectMapper objectMapper;
  private final AiProperties.Provider config;
  private final RestClient restClient;

  public GeminiChatProvider(AiProperties properties, ObjectMapper objectMapper) {
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
    return "Google Gemini";
  }

  @Override
  public String defaultModel() {
    return "gemini-3.6-flash";
  }

  @Override
  public String defaultTitleModel() {
    return "gemini-3.6-flash";
  }

  @Override
  public boolean isConfigured() {
    return StringUtils.hasText(config.apiKey());
  }

  @Override
  public AiChatResponse chat(AiChatRequest request) {
    String model =
        request.model().startsWith("models/") ? request.model().substring("models/".length())
            : request.model();
    ObjectNode body = buildBody(request);
    JsonNode response = AiRetry.onUnavailable("Gemini", () -> send(() -> restClient.post()
        .uri("/models/{model}:generateContent", model)
        .header("x-goog-api-key", config.apiKey())
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)));
    return parse(response);
  }

  @Override
  public AiChatResponse chatStream(AiChatRequest request, Consumer<String> onText) {
    String model =
        request.model().startsWith("models/") ? request.model().substring("models/".length())
            : request.model();
    ObjectNode body = buildBody(request);
    return AiRetry.onUnavailable("Gemini", () -> stream(model, body, onText));
  }

  private AiChatResponse stream(String model, ObjectNode body, Consumer<String> onText) {
    try {
      return restClient.post()
          .uri("/models/{model}:streamGenerateContent?alt=sse", model)
          .header("x-goog-api-key", config.apiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .exchange((req, res) -> {
            if (res.getStatusCode().isError()) {
              int status = res.getStatusCode().value();
              String raw = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
              throw new AiProviderException("Gemini HTTP " + status + ": " + abbreviate(raw),
                  AiProviderException.kindOf(status));
            }
            ArrayNode parts = objectMapper.createArrayNode();
            String finishReason = null;
            boolean blocked = false;
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(res.getBody(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
              if (!line.startsWith("data:")) {
                continue;
              }
              String json = line.substring("data:".length()).trim();
              if (json.isEmpty()) {
                continue;
              }
              JsonNode chunk = objectMapper.readTree(json);
              blocked |= chunk.path("promptFeedback").hasNonNull("blockReason");
              JsonNode candidate = chunk.path("candidates").path(0);
              for (JsonNode part : candidate.path("content").path("parts")) {
                parts.add(part);
                if (!part.path("thought").asBoolean(false) && part.hasNonNull("text") && !part.has(
                    "functionCall")) {
                  onText.accept(part.get("text").asText());
                }
              }
              if (candidate.hasNonNull("finishReason")) {
                finishReason = candidate.get("finishReason").asText();
              }
            }
            ObjectNode combined = objectMapper.createObjectNode();
            if (blocked) {
              combined.putObject("promptFeedback").put("blockReason", "BLOCKED");
            }
            ObjectNode candidate = combined.putArray("candidates").addObject();
            candidate.putObject("content").set("parts", parts);
            if (finishReason != null) {
              candidate.put("finishReason", finishReason);
            }
            return parse(combined);
          });
    } catch (RestClientException ex) {
      throw new AiProviderException("Gemini request failed: " + ex.getMessage(), ex);
    }
  }

  @Override
  public List<String> listModels() {
    JsonNode response = AiRetry.onUnavailable("Gemini", () -> send(() -> restClient.get()
        .uri("/models?pageSize=1000")
        .header("x-goog-api-key", config.apiKey())));
    List<String> models = new ArrayList<>();
    for (JsonNode model : response.path("models")) {
      boolean generates = false;
      for (JsonNode method : model.path("supportedGenerationMethods")) {
        generates |= "generateContent".equals(method.asText());
      }
      String name = model.path("name").asText("");
      if (generates && name.startsWith("models/")) {
        String id = name.substring("models/".length());
        if (NON_CHAT_MODEL_MARKERS.stream().noneMatch(id::contains)) {
          models.add(id);
        }
      }
    }
    models.sort(java.util.Comparator.reverseOrder());
    return models;
  }

  private JsonNode send(java.util.function.Supplier<RestClient.RequestHeadersSpec<?>> request) {
    try {
      return request.get().exchange((req, res) -> {
        String raw = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
        if (res.getStatusCode().isError()) {
          int status = res.getStatusCode().value();
          throw new AiProviderException("Gemini HTTP " + status + ": " + abbreviate(raw),
              AiProviderException.kindOf(status));
        }
        return objectMapper.readTree(raw);
      });
    } catch (RestClientException ex) {
      throw new AiProviderException("Gemini request failed: " + ex.getMessage(), ex);
    }
  }

  private ObjectNode buildBody(AiChatRequest request) {
    ObjectNode body = objectMapper.createObjectNode();
    if (StringUtils.hasText(request.systemPrompt())) {
      body.putObject("systemInstruction").putArray("parts").addObject()
          .put("text", request.systemPrompt());
    }

    ArrayNode contents = body.putArray("contents");
    Map<String, AiToolCall> callsById = new HashMap<>();
    String lastRole = null;
    ArrayNode parts = null;
    for (AiMessage message : request.messages()) {
      String role = message.role() == AiRole.ASSISTANT ? "model" : "user";
      if (!role.equals(lastRole)) {
        ObjectNode content = contents.addObject().put("role", role);
        parts = content.putArray("parts");
        lastRole = role;
      }
      appendParts(parts, message, callsById);
    }
    for (Iterator<JsonNode> it = contents.elements(); it.hasNext(); ) {
      if (it.next().path("parts").isEmpty()) {
        it.remove();
      }
    }

    if (!request.tools().isEmpty()) {
      ArrayNode declarations = body.putArray("tools").addObject().putArray("functionDeclarations");
      for (AiToolSpec tool : request.tools()) {
        ObjectNode declaration = declarations.addObject()
            .put("name", tool.name())
            .put("description", tool.description());
        JsonNode parameters = sanitize(tool.inputSchema());
        if (parameters.path("properties").size() > 0) {
          declaration.set("parameters", parameters);
        }
      }
    }
    body.putObject("generationConfig")
        .put("maxOutputTokens", Math.max(request.maxOutputTokens(), MIN_OUTPUT_TOKENS));
    return body;
  }

  private void appendParts(ArrayNode parts, AiMessage message, Map<String, AiToolCall> callsById) {
    if (message.role() == AiRole.USER) {
      for (AiToolResult result : message.toolResults()) {
        ObjectNode response = objectMapper.createObjectNode();
        if (result.error()) {
          response.put("error", result.content());
        } else {
          response.set("result", asJson(result.content()));
        }
        ObjectNode functionResponse = parts.addObject().putObject("functionResponse");
        AiToolCall call = callsById.get(result.toolCallId());
        if (call != null && isNativeId(call.id())) {
          functionResponse.put("id", call.id());
        }
        functionResponse.put("name", result.toolName());
        functionResponse.set("response", response);
      }
      if (message.hasText()) {
        parts.addObject().put("text", message.text());
      }
      return;
    }
    if (message.hasText()) {
      parts.addObject().put("text", message.text());
    }
    for (AiToolCall call : message.toolCalls()) {
      callsById.put(call.id(), call);
      ObjectNode part = parts.addObject();
      ObjectNode functionCall = part.putObject("functionCall");
      if (isNativeId(call.id())) {
        functionCall.put("id", call.id());
      }
      functionCall.put("name", call.name());
      functionCall.set("args",
          call.arguments() != null ? call.arguments() : objectMapper.createObjectNode());
      JsonNode signature =
          call.providerData() == null ? null : call.providerData().get("thoughtSignature");
      if (signature != null && signature.isTextual() && ID.equals(
          call.providerData().path("provider").asText())) {
        part.put("thoughtSignature", signature.asText());
      }
    }
  }

  private JsonNode asJson(String content) {
    try {
      JsonNode node = objectMapper.readTree(content);
      if (node != null && (node.isObject() || node.isArray())) {
        return node;
      }
    } catch (Exception ignored) {

    }
    return objectMapper.getNodeFactory().textNode(content);
  }

  private JsonNode sanitize(JsonNode schema) {
    ObjectNode clean = objectMapper.createObjectNode();
    if (schema == null || !schema.isObject()) {
      return clean;
    }
    schema.fields().forEachRemaining(field -> {
      String key = field.getKey();
      JsonNode value = field.getValue();
      if (!SCHEMA_KEYS.contains(key)) {
        return;
      }
      switch (key) {
        case "properties" -> {
          ObjectNode properties = objectMapper.createObjectNode();
          value.fields().forEachRemaining(
              property -> properties.set(property.getKey(), sanitize(property.getValue())));
          if (!properties.isEmpty()) {
            clean.set(key, properties);
          }
        }
        case "items" -> clean.set(key, sanitize(value));
        case "anyOf" -> {
          ArrayNode options = clean.putArray(key);
          value.forEach(option -> options.add(sanitize(option)));
        }
        default -> clean.set(key, value);
      }
    });
    if (clean.has("required")) {
      ArrayNode required = objectMapper.createArrayNode();
      clean.get("required").forEach(name -> {
        if (clean.path("properties").has(name.asText())) {
          required.add(name);
        }
      });
      if (required.isEmpty()) {
        clean.remove("required");
      } else {
        clean.set("required", required);
      }
    }
    return clean;
  }


  private AiChatResponse parse(JsonNode response) {
    if (response.path("promptFeedback").hasNonNull("blockReason")) {
      return new AiChatResponse(null, List.of(), AiFinishReason.REFUSED);
    }
    JsonNode candidate = response.path("candidates").path(0);
    if (candidate.isMissingNode()) {
      throw new AiProviderException(
          "Gemini response has no candidates: " + abbreviate(response.toString()));
    }

    StringBuilder text = new StringBuilder();
    List<AiToolCall> toolCalls = new ArrayList<>();
    for (JsonNode part : candidate.path("content").path("parts")) {
      if (part.path("thought").asBoolean(false)) {
        continue;
      }
      if (part.has("functionCall")) {
        JsonNode call = part.get("functionCall");
        String id =
            call.hasNonNull("id") ? call.get("id").asText() : LOCAL_ID_PREFIX + UUID.randomUUID();
        JsonNode args = call.path("args");
        ObjectNode providerData = objectMapper.createObjectNode().put("provider", ID);
        if (part.hasNonNull("thoughtSignature")) {
          providerData.put("thoughtSignature", part.get("thoughtSignature").asText());
        }
        toolCalls.add(new AiToolCall(id, call.path("name").asText(),
            args.isObject() ? args : objectMapper.createObjectNode(), providerData));
      } else if (part.hasNonNull("text")) {
        text.append(part.get("text").asText());
      }
    }

    AiFinishReason finishReason = switch (candidate.path("finishReason").asText("")) {
      case "STOP", "" -> toolCalls.isEmpty() ? AiFinishReason.END : AiFinishReason.TOOL_CALLS;
      case "MAX_TOKENS" -> AiFinishReason.MAX_TOKENS;
      case "SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY" ->
          AiFinishReason.REFUSED;
      default -> toolCalls.isEmpty() ? AiFinishReason.OTHER : AiFinishReason.TOOL_CALLS;
    };
    return new AiChatResponse(text.isEmpty() ? null : text.toString(), toolCalls, finishReason);
  }

  private static boolean isNativeId(String id) {
    return id != null && !id.startsWith(LOCAL_ID_PREFIX);
  }

  private static String abbreviate(String value) {
    return value == null || value.length() <= 500 ? value : value.substring(0, 500) + "...";
  }
}
