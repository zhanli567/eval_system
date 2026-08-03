package com.agentnexus.backend.remoteCall.service;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 为远程调用提供忽略证书校验的 HTTPS 支持，供 trust-all-ssl 配置开启时复用。
 * 创建日期：2026-08-03
 */
final class TrustAllSslSupport {
  private static final HostnameVerifier TRUST_ALL_HOSTNAME_VERIFIER = (hostname, session) -> true;
  private static final TrustManager[] TRUST_ALL_MANAGERS = {
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
  private static volatile SSLSocketFactory trustAllSocketFactory;

  private TrustAllSslSupport() {
  }

  static void configure(HttpsURLConnection connection) {
    connection.setSSLSocketFactory(trustAllSocketFactory());
    connection.setHostnameVerifier(TRUST_ALL_HOSTNAME_VERIFIER);
  }

  private static SSLSocketFactory trustAllSocketFactory() {
    SSLSocketFactory current = trustAllSocketFactory;
    if (current != null) {
      return current;
    }
    synchronized (TrustAllSslSupport.class) {
      if (trustAllSocketFactory == null) {
        trustAllSocketFactory = createTrustAllSocketFactory();
      }
      return trustAllSocketFactory;
    }
  }

  private static SSLSocketFactory createTrustAllSocketFactory() {
    try {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, TRUST_ALL_MANAGERS, new SecureRandom());
      return context.getSocketFactory();
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new IllegalStateException("初始化HTTPS证书信任配置失败：" + e.getMessage(), e);
    }
  }
}
