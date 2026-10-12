package com.onlinemenu.ai.tool.service;

public record AiToolExecution(
    boolean success,
    String content,
    String userMessage
) {

}
