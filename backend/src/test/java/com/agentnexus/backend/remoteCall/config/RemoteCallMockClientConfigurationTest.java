package com.agentnexus.backend.remoteCall.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentnexus.backend.remoteCall.client.RemoteCallServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RemoteCallMockClientConfigurationTest {
  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(RemoteCallMockClientConfiguration.class)
      .withPropertyValues("remote-call.mock.enabled=true");

  @Test
  void providesMockClientWhenLocalMockIsEnabled() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(RemoteCallServiceClient.class);

      RemoteCallServiceClient client = context.getBean(RemoteCallServiceClient.class);
      var response = client.listModels(10, 1, "local-space");

      assertThat(response.status()).isEqualTo("200");
      assertThat(response.success()).isTrue();
      assertThat(response.resultObjVO().result())
          .isNotEmpty()
          .allMatch(model -> "IAM".equals(model.authType()));
    });
  }
}
