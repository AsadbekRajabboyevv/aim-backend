package com.onlinemenu.ai.tool.domain;

public enum AiToolHttpMethod {
  GET,
  POST,
  PUT,
  PATCH,
  DELETE;

  public boolean hasBody() {
    return this == POST || this == PUT || this == PATCH;
  }
}
