package com.hackathon.seoul.llm;

/** role: "user" 또는 "assistant" */
public record LlmMessage(String role, String content) {}
