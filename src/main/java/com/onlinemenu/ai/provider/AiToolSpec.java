package com.onlinemenu.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;

public record AiToolSpec(
    String name,
    String description,
    JsonNode inputSchema
) {

}
