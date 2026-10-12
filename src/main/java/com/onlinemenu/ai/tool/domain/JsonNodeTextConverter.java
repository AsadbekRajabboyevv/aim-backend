package com.onlinemenu.ai.tool.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JsonNodeTextConverter implements AttributeConverter<JsonNode, String> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(JsonNode value) {
    return value == null ? null : value.toString();
  }

  @Override
  public JsonNode convertToEntityAttribute(String value) {
    try {
      return value == null ? null : MAPPER.readTree(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Stored JSON is not valid", ex);
    }
  }
}
