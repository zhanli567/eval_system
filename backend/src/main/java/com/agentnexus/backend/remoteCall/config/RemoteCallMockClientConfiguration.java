package com.agentnexus.backend.remoteCall.config;

import com.agentnexus.backend.remoteCall.client.LocalMockRemoteCallServiceClient;
import com.agentnexus.backend.remoteCall.client.RemoteCallServiceClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "remote-call.mock", name = "enabled", havingValue = "true")
public class RemoteCallMockClientConfiguration {
  @Bean
  @ConditionalOnMissingBean(RemoteCallServiceClient.class)
  RemoteCallServiceClient remoteCallServiceClient() {
    return new LocalMockRemoteCallServiceClient();
  }
}
