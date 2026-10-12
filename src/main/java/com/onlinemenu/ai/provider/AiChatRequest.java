package com.onlinemenu.ai.provider;

import java.util.List;

public record AiChatRequest(
    String model,
    String systemPrompt,
    List<AiMessage> messages,
    List<AiToolSpec> tools,
    int maxOutputTokens
) {

}
