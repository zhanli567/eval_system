package com.agentnexus.backend.remoteCall.constant;

/**
 * 智能体流式消息内容类型常量。
 *
 * 创建日期：2026-08-10。
 */
public final class ChatContentTypeConstants {
  private ChatContentTypeConstants() {
  }

  public static final String DEBUG = "debug";
  public static final String ERROR = "error";
  public static final String GEN_UI = "gen_ui";
  public static final String GEN_UI_CAMEL = "genUi";
  public static final String REASONING = "reasoning";
  public static final String REFERENCES = "references";
  public static final String SKILL_TRIGGER = "skill_trigger";
  public static final String SKILL_TRIGGER_CAMEL = "skillTrigger";
  public static final String TEXT = "text";
  public static final String TOOL_CALL = "tool_call";
  public static final String TOOL_CALL_CAMEL = "toolCall";
  public static final String TOOL_RESPONSE = "tool_response";
  public static final String TOOL_RESPONSE_CAMEL = "toolResponse";
}
