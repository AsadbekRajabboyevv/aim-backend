package com.onlinemenu.ai.tool.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiToolSchemas {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_]+)}");

  private AiToolSchemas() {
  }

  public static JsonNode forModel(JsonNode schema) {
    JsonNode copy = schema.deepCopy();
    strip(copy);
    if (copy instanceof ObjectNode object && !object.has("properties")) {
      object.putObject("properties");
    }
    return copy;
  }

  public static Set<String> placeholders(String pathTemplate) {
    Set<String> names = new LinkedHashSet<>();
    Matcher matcher = PLACEHOLDER.matcher(pathTemplate);
    while (matcher.find()) {
      names.add(matcher.group(1));
    }
    return names;
  }

  private static void strip(JsonNode node) {
    if (node instanceof ObjectNode object) {
      Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        if (field.getKey().startsWith("x-")) {
          fields.remove();
        } else {
          strip(field.getValue());
        }
      }
    } else if (node instanceof ArrayNode array) {
      array.forEach(AiToolSchemas::strip);
    }
  }
}
