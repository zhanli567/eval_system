package com.agentnexus.backend.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "integration.platform")
public class PlatformIntegrationProperties {
  private String baseUrl = "";
  private boolean trustAllSsl = true;
  private int connectTimeoutMs = 500000;
  private int readTimeoutMs = 6000000;
  private Iam iam = new Iam();

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = clean(baseUrl);
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
