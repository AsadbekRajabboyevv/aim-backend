package com.onlinemenu.ai.tool.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.onlinemenu.ai.tool.domain.AiTool;
import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.constant.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AiToolExecutor {

  private static final int MAX_CONTENT_CHARS = 20_000;
  private static final Pattern SAFE_PATH_VALUE = Pattern.compile("^[A-Za-z0-9_-]{1,180}$");

  private final ObjectMapper objectMapper;
  private final Environment environment;
  private final RestClient restClient;

  public AiToolExecutor(ObjectMapper objectMapper, Environment environment) {
    this.objectMapper = objectMapper;
    this.environment = environment;
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    requestFactory.setReadTimeout(Duration.ofSeconds(30));
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
  }

  public AiToolExecution execute(AiTool tool, JsonNode arguments, AiToolExecutionContext context) {
    ObjectNode remaining = arguments != null && arguments.isObject()
        ? ((ObjectNode) arguments).deepCopy()
        : objectMapper.createObjectNode();

    String path = tool.getPathTemplate();
    for (String placeholder : AiToolSchemas.placeholders(path)) {
      JsonNode value = remaining.remove(placeholder);
      if (value == null || value.isNull() || !SAFE_PATH_VALUE.matcher(value.asText()).matches()) {
        return new AiToolExecution(false,
            "Error: argument '" + placeholder + "' is missing or not a valid slug.", null);
      }
      path = path.replace("{" + placeholder + "}", value.asText());
    }

    UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(baseUrl()).path(path);
    boolean hasBody = tool.getHttpMethod().hasBody();
    if (!hasBody) {
      Iterator<Map.Entry<String, JsonNode>> fields = remaining.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        if (field.getValue().isValueNode() && !field.getValue().isNull()) {
          uri.queryParam(field.getKey(), field.getValue().asText());
        }
      }
    }

    try {
      RestClient.RequestBodySpec request = restClient.method(
              HttpMethod.valueOf(tool.getHttpMethod().name()))
          .uri(uri.encode().build().toUri())
          .header(SecurityConstants.AUTH_HEADER,
              SecurityConstants.TOKEN_PREFIX + context.bearerToken())
          .header(AppConstants.LANGUAGE_HEADER, context.language())
          .accept(MediaType.APPLICATION_JSON);
      if (hasBody) {
        request.contentType(MediaType.APPLICATION_JSON).body(remaining);
      }
      return request.exchange((req, res) -> {
        String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
        int status = res.getStatusCode().value();
        if (res.getStatusCode().isError()) {
          String message = errorMessage(body);
          return new AiToolExecution(false,
              "Error (HTTP " + status + "): " + truncate(message != null ? message : body),
              message);
        }
        return new AiToolExecution(true, truncate(body), null);
      });
    } catch (RestClientException ex) {
      log.warn("AI tool {} failed: {}", tool.getName(), ex.getMessage());
      return new AiToolExecution(false, "Error: the tool could not be reached.", null);
    }
  }

  private String baseUrl() {
    return "http://localhost:" + environment.getProperty("local.server.port",
        environment.getProperty("server.port", "8080"));
  }

  private String errorMessage(String body) {
    try {
      JsonNode node = objectMapper.readTree(body);
      return node.path("message").isTextual() ? node.path("message").asText() : null;
    } catch (Exception ex) {
      return null;
    }
  }

  private static String truncate(String value) {
    return value.length() <= MAX_CONTENT_CHARS ? value
        : value.substring(0, MAX_CONTENT_CHARS) + "\n[truncated]";
  }
}
