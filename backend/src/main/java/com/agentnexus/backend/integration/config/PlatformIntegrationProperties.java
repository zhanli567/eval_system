package com.agentnexus.backend.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "integration.platform")
public class PlatformIntegrationProperties {
  private String xSpaceId = "";
  private String modelListUrl = "";
  private String agentListUrl = "";
  private String agentDetailUrl = "";
  private String agentBundleListUrl = "";
  private String agentChatUrl = "";
  private boolean trustAllSsl = true;
  private int connectTimeoutMs = 5000;
  private int readTimeoutMs = 60000;
  private Iam iam = new Iam();

  public String getXSpaceId() {
    return xSpaceId;
  }

  public void setXSpaceId(String xSpaceId) {
    this.xSpaceId = clean(xSpaceId);
  }

  public String getModelListUrl() {
    return modelListUrl;
  }

  public void setModelListUrl(String modelListUrl) {
    this.modelListUrl = clean(modelListUrl);
  }

  public String getAgentListUrl() {
    return agentListUrl;
  }

  public void setAgentListUrl(String agentListUrl) {
    this.agentListUrl = clean(agentListUrl);
  }

  public String getAgentDetailUrl() {
    return agentDetailUrl;
  }

  public void setAgentDetailUrl(String agentDetailUrl) {
    this.agentDetailUrl = clean(agentDetailUrl);
  }

  public String getAgentBundleListUrl() {
    return agentBundleListUrl;
  }

  public void setAgentBundleListUrl(String agentBundleListUrl) {
    this.agentBundleListUrl = clean(agentBundleListUrl);
  }

  public String getAgentChatUrl() {
    return agentChatUrl;
  }

  public void setAgentChatUrl(String agentChatUrl) {
    this.agentChatUrl = clean(agentChatUrl);
  }

  public boolean isTrustAllSsl() {
    return trustAllSsl;
  }

  public void setTrustAllSsl(boolean trustAllSsl) {
    this.trustAllSsl = trustAllSsl;
  }

  public int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  public void setConnectTimeoutMs(int connectTimeoutMs) {
    this.connectTimeoutMs = connectTimeoutMs;
  }

  public int getReadTimeoutMs() {
    return readTimeoutMs;
  }

  public void setReadTimeoutMs(int readTimeoutMs) {
    this.readTimeoutMs = readTimeoutMs;
  }

  public Iam getIam() {
    return iam;
  }

  public void setIam(Iam iam) {
    this.iam = iam == null ? new Iam() : iam;
  }

  private String clean(String value) {
    return value == null ? "" : value.trim();
  }

  public static class Iam {
    private String url = "";
    private String authorization = "";

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = clean(url);
    }

    public String getAuthorization() {
      return authorization;
    }

    public void setAuthorization(String authorization) {
      this.authorization = clean(authorization);
    }

    private String clean(String value) {
      return value == null ? "" : value.trim();
    }
  }
}
