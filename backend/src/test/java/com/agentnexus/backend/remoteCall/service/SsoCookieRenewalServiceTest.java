package com.agentnexus.backend.remoteCall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentnexus.backend.remoteCall.config.RemoteCallProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SsoCookieRenewalServiceTest {
  private static final long NOW = 1_800_000_000_000L;
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC);
  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void ensureCookiesValidReturnsOriginalCookiesBeforeRenewWindow() {
    String cookie = "sid=abc; hwssot3=" + octalMinutesAgo(19) + "; hwssotinter3=old-inter; theme=dark";

    String renewed = new SsoCookieRenewalService(properties(), clock).ensureCookiesValid(cookie);

    assertThat(renewed).isEqualTo(cookie);
  }

  @Test
  void ensureCookiesValidRenewsSsoCookiesDuringRenewWindow() throws Exception {
    AtomicReference<String> requestCookie = new AtomicReference<>("");
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/only4ssoTimeUpdate.do", exchange -> {
      requestCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
      exchange.getResponseHeaders().add("Set-Cookie", "hwssot3=7654321; Path=/; Domain=example.com");
      exchange.getResponseHeaders().add("Set-Cookie", "hwssotinter3=inter-new; Path=/; HttpOnly");
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
    });
    server.start();
    String cookie = "sid=abc; hwssot3=" + octalMinutesAgo(25) + "; theme=dark; hwssotinter3=old-inter";
    RemoteCallProperties properties = properties();
    properties.setSsoCookieRenewalUrl("http://localhost:" + server.getAddress().getPort() + "/only4ssoTimeUpdate.do");

    String renewed = new SsoCookieRenewalService(properties, clock).ensureCookiesValid(cookie);

    assertThat(requestCookie).hasValue(cookie);
    assertThat(renewed).isEqualTo("sid=abc; hwssot3=7654321; theme=dark; hwssotinter3=inter-new");
  }

  @Test
  void ensureCookiesValidThrowsWhenCookieIsExpired() {
    String cookie = "sid=abc; hwssot3=" + octalMinutesAgo(31) + "; hwssotinter3=old-inter";

    assertThatThrownBy(() -> new SsoCookieRenewalService(properties(), clock).ensureCookiesValid(cookie))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("cookie");
  }

  private RemoteCallProperties properties() {
    RemoteCallProperties properties = new RemoteCallProperties();
    properties.setAgentChatUrl("http://runtime.example.com/v1/chat/completions");
    return properties;
  }

  private String octalMinutesAgo(long minutes) {
    long timestamp = NOW - TimeUnit.MINUTES.toMillis(minutes);
    return Long.toOctalString(timestamp);
  }
}
