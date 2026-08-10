package com.agentnexus.backend.remoteCall.constant;

/**
 * 远程调用协议常量。
 *
 * 创建日期：2026-08-10。
 */
public final class RemoteCallProtocolConstants {
  private RemoteCallProtocolConstants() {
  }

  public static final String HTTP = "http";
  public static final String HTTPS = "https";
  public static final String POST = "POST";
  public static final String SSE_DATA_PREFIX = "data:";
  public static final String SSE_EVENT_PREFIX = "event:";
  public static final String SSE_ID_PREFIX = "id:";
  public static final String SSE_RETRY_PREFIX = "retry:";
  public static final String SSE_COMMENT_PREFIX = ":";
  public static final String SSE_DONE_PAYLOAD = "[DONE]";
  public static final String THINK_END_TAG = "</think>";
}
