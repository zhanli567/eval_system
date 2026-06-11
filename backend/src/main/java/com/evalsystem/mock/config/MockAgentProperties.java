package com.evalsystem.mock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mock.agent")
public class MockAgentProperties {
  private String url = "";
  private int connectTimeoutMs = 5000;
  private int readTimeoutMs = 60000;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url == null ? "" : url.trim();
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
}
