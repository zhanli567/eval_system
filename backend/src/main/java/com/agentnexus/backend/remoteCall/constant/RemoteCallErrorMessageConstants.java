package com.agentnexus.backend.remoteCall.constant;

/**
 * 远程调用异常提示常量。
 *
 * 创建日期：2026-08-10。
 */
public final class RemoteCallErrorMessageConstants {
  private static final String HTTP_STATUS_PREFIX = "IAM模型对话接口调用失败，HTTP ";
  private static final String CHINESE_COLON = "：";
  private static final String IAM_CHAT_FAILED_PREFIX = "IAM模型对话接口调用失败：";
  private static final String UNSUPPORTED_REMOTE_URL_PREFIX = "远程调用地址仅支持HTTP或HTTPS：";
  private static final String REMOTE_RESPONSE_FAILED_STATUS_PART = "返回失败，status=";
  private static final String REMOTE_RESPONSE_FAILED_SUCCESS_PART = "，success=";

  public static final String AGENT_ID_REQUIRED = "Agent ID cannot be blank";
  public static final String AGENT_BUNDLE_ID_REQUIRED = "Agent bundle ID cannot be blank";
  public static final String AGENT_DETAIL_EMPTY = "Agent detail API returned empty result";
  public static final String MODEL_ID_REQUIRED = "模型ID不能为空";
  public static final String MODEL_NAME_REQUIRED = "模型名称不能为空";
  public static final String IAM_URL_REQUIRED = "请配置IAM模型对话接口 remoteCall.iam.url";
  public static final String IAM_TOKEN_REQUIRED = "IAM token不能为空";
  public static final String IAM_CHAT_CHOICES_REQUIRED = "IAM模型对话接口返回缺少choices";
  public static final String IAM_CHAT_MESSAGE_CONTENT_REQUIRED = "IAM模型对话接口返回缺少message.content";
  public static final String AGENT_CHAT_URL_REQUIRED = "Please configure agent chat API remoteCall.agent-chat-url";
  public static final String REMOTE_URL_REQUIRED = "远程调用地址不能为空";
  public static final String MODEL_LIST_API_NAME = "模型列表接口";
  public static final String AGENT_LIST_API_NAME = "智能体列表接口";
  public static final String AGENT_BUNDLE_LIST_API_NAME = "Agent bundle list API";
  public static final String SPACE_LIST_API_NAME = "Space list API";
  public static final String AGENT_DETAIL_API_NAME = "Agent detail API";

  private RemoteCallErrorMessageConstants() {
  }

  /**
   * 返回IAM模型对话接口HTTP失败提示。
   *
   * @param statusCode 状态码
   * @param responseBody 响应体
   * @return 返回IAM模型对话接口HTTP失败提示
   */
  public static String iamChatHttpFailed(int statusCode, String responseBody) {
    return HTTP_STATUS_PREFIX + statusCode + CHINESE_COLON + responseBody;
  }

  /**
   * 返回IAM模型对话接口调用失败提示。
   *
   * @param message 异常信息
   * @return 返回IAM模型对话接口调用失败提示
   */
  public static String iamChatFailed(String message) {
    return IAM_CHAT_FAILED_PREFIX + message;
  }

  /**
   * 返回远程调用地址协议不支持提示。
   *
   * @param url 远程调用地址
   * @return 返回远程调用地址协议不支持提示
   */
  public static String unsupportedRemoteUrl(String url) {
    return UNSUPPORTED_REMOTE_URL_PREFIX + url;
  }

  /**
   * 返回远程接口响应失败提示。
   *
   * @param name 接口名称
   * @param status 响应状态
   * @param success 成功标识
   * @return 返回远程接口响应失败提示
   */
  public static String remoteResponseFailed(String name, String status, Boolean success) {
    return name + REMOTE_RESPONSE_FAILED_STATUS_PART + status + REMOTE_RESPONSE_FAILED_SUCCESS_PART + success;
  }
}
