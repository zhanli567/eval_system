package com.agentnexus.backend.remoteCall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "remote-call")
public class RemoteCallProperties {
  private String agentChatUrl = "";
  private String ssoCookieRenewalUrl = "";
  private boolean trustAllSsl = true;
  private int connectTimeoutMs = 500000;
  private int readTimeoutMs = 6000000;
  private Iam iam = new Iam();

  @Data
  public static class Iam {
    private String url = "";
  }
}
