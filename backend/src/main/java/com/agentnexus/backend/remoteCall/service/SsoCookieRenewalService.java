package com.agentnexus.backend.remoteCall.service;

import com.agentnexus.backend.remoteCall.config.RemoteCallProperties;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SsoCookieRenewalService {
  private static final String HWSSOT3 = "hwssot3";
  private static final String HWSSOTINTER3 = "hwssotinter3";
  private static final long RENEW_AFTER_MINUTES = 20;
  private static final long EXPIRE_AFTER_MINUTES = 30;
  private static final String DEFAULT_RENEWAL_PATH = "/only4ssoTimeUpdate.do";
  private static final Pattern SSO_COOKIE_PATTERN = Pattern.compile("(hwssot3|hwssotinter3)=([^;,\\s]+)");
  private static final HostnameVerifier TRUST_ALL_HOSTNAME_VERIFIER = (hostname, session) -> true;
  private static volatile SSLSocketFactory trustAllSocketFactory;

  private final RemoteCallProperties properties;
  private final Clock clock;

  public SsoCookieRenewalService(RemoteCallProperties properties) {
    this(properties, Clock.systemUTC());
  }

  SsoCookieRenewalService(RemoteCallProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public String ensureCookiesValid(String cookies) {
    if (!StringUtils.hasText(cookies)) {
      return cookies;
    }
    String hwssot3 = cookieValue(cookies, HWSSOT3);
    if (!StringUtils.hasText(hwssot3)) {
      throw new IllegalStateException("SSO cookie缺少hwssot3，无法判断cookie有效期");
    }
    long elapsedMinutes = elapsedMinutes(hwssot3);
    if (elapsedMinutes < RENEW_AFTER_MINUTES) {
      return cookies;
    }
    if (elapsedMinutes > EXPIRE_AFTER_MINUTES) {
      throw new IllegalStateException("SSO cookie已过期，请重新登录后再启动评测任务");
    }
    return renewCookies(cookies);
  }

  private long elapsedMinutes(String hwssot3) {
    try {
      long timestamp = Long.parseLong(hwssot3, 8);
      return (clock.millis() - timestamp) / 60000;
    } catch (NumberFormatException e) {
      throw new IllegalStateException("SSO cookie中的hwssot3不是合法八进制时间戳", e);
    }
  }

  private String renewCookies(String cookies) {
    HttpURLConnection connection = null;
    try {
      connection = openRenewalConnection();
      connection.setRequestProperty("Cookie", cookies);
      int statusCode = connection.getResponseCode();
      if (statusCode < 200 || statusCode >= 300) {
        throw new IllegalStateException("SSO cookie续期失败，HTTP " + statusCode);
      }
      Map<String, String> renewedCookies = extractRenewedCookies(connection.getHeaderFields());
      if (renewedCookies.isEmpty()) {
        throw new IllegalStateException("SSO cookie续期响应缺少hwssot3/hwssotinter3");
      }
      return replaceCookies(cookies, renewedCookies);
    } catch (IOException e) {
      throw new IllegalStateException("SSO cookie续期失败：" + e.getMessage(), e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private HttpURLConnection openRenewalConnection() throws IOException {
    HttpURLConnection connection = (HttpURLConnection) URI.create(renewalUrl()).toURL().openConnection();
    if (connection instanceof HttpsURLConnection httpsConnection) {
      configureHttps(httpsConnection);
    }
    connection.setRequestMethod("GET");
    connection.setConnectTimeout(Math.max(properties.getConnectTimeoutMs(), 1));
    connection.setReadTimeout(Math.max(properties.getReadTimeoutMs(), 1));
    return connection;
  }

  private String renewalUrl() {
    if (StringUtils.hasText(properties.getSsoCookieRenewalUrl())) {
      return properties.getSsoCookieRenewalUrl();
    }
    String agentChatUrl = requireText(properties.getAgentChatUrl(), "请配置SSO cookie续期接口或Agent chat接口");
    return URI.create(agentChatUrl).resolve(DEFAULT_RENEWAL_PATH).toString();
  }

  private void configureHttps(HttpsURLConnection connection) {
    if (!properties.isTrustAllSsl()) {
      return;
    }
    connection.setSSLSocketFactory(trustAllSocketFactory());
    connection.setHostnameVerifier(TRUST_ALL_HOSTNAME_VERIFIER);
  }

  private SSLSocketFactory trustAllSocketFactory() {
    SSLSocketFactory current = trustAllSocketFactory;
    if (current != null) {
      return current;
    }
    synchronized (SsoCookieRenewalService.class) {
      if (trustAllSocketFactory == null) {
        try {
          TrustManager[] trustManagers = new TrustManager[] {
              new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                  return new X509Certificate[0];
                }
              }
          };
          SSLContext context = SSLContext.getInstance("TLS");
          context.init(null, trustManagers, new SecureRandom());
          trustAllSocketFactory = context.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
          throw new IllegalStateException("初始化HTTPS证书信任配置失败：" + e.getMessage(), e);
        }
      }
      return trustAllSocketFactory;
    }
  }

  private Map<String, String> extractRenewedCookies(Map<String, List<String>> headers) {
    Map<String, String> cookies = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> header : headers.entrySet()) {
      if (!"Set-Cookie".equalsIgnoreCase(header.getKey()) || header.getValue() == null) {
        continue;
      }
      for (String value : header.getValue()) {
        Matcher matcher = SSO_COOKIE_PATTERN.matcher(value == null ? "" : value);
        while (matcher.find()) {
          cookies.put(matcher.group(1), matcher.group(2));
        }
      }
    }
    return cookies;
  }

  private String replaceCookies(String originalCookies, Map<String, String> renewedCookies) {
    Map<String, Boolean> replaced = new LinkedHashMap<>();
    renewedCookies.keySet().forEach(name -> replaced.put(name, Boolean.FALSE));
    List<String> parts = new ArrayList<>();
    for (String part : originalCookies.split(";")) {
      String trimmed = part.trim();
      if (!StringUtils.hasText(trimmed)) {
        continue;
      }
      String name = cookieName(trimmed);
      if (renewedCookies.containsKey(name)) {
        parts.add(name + "=" + renewedCookies.get(name));
        replaced.put(name, Boolean.TRUE);
      } else {
        parts.add(trimmed);
      }
    }
    for (Map.Entry<String, Boolean> entry : replaced.entrySet()) {
      if (!entry.getValue()) {
        parts.add(entry.getKey() + "=" + renewedCookies.get(entry.getKey()));
      }
    }
    return String.join("; ", parts);
  }

  private String cookieValue(String cookies, String name) {
    for (String part : cookies.split(";")) {
      String trimmed = part.trim();
      if (name.equals(cookieName(trimmed))) {
        int split = trimmed.indexOf('=');
        return split >= 0 ? trimmed.substring(split + 1) : "";
      }
    }
    return "";
  }

  private String cookieName(String cookie) {
    int split = cookie.indexOf('=');
    return split < 0 ? cookie : cookie.substring(0, split).trim();
  }

  private String requireText(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException(message);
    }
    return value.trim();
  }
}
