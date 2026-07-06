package com.agentnexus.backend.iam;

import com.agentnexus.backend.remoteCall.config.RemoteCallProperties;
import org.springframework.stereotype.Component;

@Component
public class IamTokenService {
  private final RemoteCallProperties properties;

  public IamTokenService(RemoteCallProperties properties) {
    this.properties = properties;
  }

  public String getToken() {
    return properties.getIam().getAuthorization();
  }
}
