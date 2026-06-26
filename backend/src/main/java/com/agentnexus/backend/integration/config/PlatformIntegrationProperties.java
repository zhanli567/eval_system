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
  private String modelChatUrl = "";
  private String agentChatUrl = "";
  private boolean trustAllSsl = true;
  private int connectTimeoutMs = 5000;
  private int readTimeoutMs = 60000;
  private Login login = new Login();
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

  public String getModelChatUrl() {
    return modelChatUrl;
  }

  public void setModelChatUrl(String modelChatUrl) {
    this.modelChatUrl = clean(modelChatUrl);
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

  public Login getLogin() {
    return login;
  }

  public void setLogin(Login login) {
    this.login = login == null ? new Login() : login;
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

  public static class Login {
    private String url = "";
    private String loginAccount = "";
    private String uid = "";
    private String password = "";
    private String lang = "";
    private Boolean rememberAccountName = Boolean.TRUE;
    private String appId = "";
    private String encryptedPasswordSwitch = "";

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = clean(url);
    }

    public String getLoginAccount() {
      return loginAccount;
    }

    public void setLoginAccount(String loginAccount) {
      this.loginAccount = clean(loginAccount);
    }

    public String getUid() {
      return uid;
    }

    public void setUid(String uid) {
      this.uid = clean(uid);
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = clean(password);
    }

    public String getLang() {
      return lang;
    }

    public void setLang(String lang) {
      this.lang = clean(lang);
    }

    public Boolean getRememberAccountName() {
      return rememberAccountName;
    }

    public void setRememberAccountName(Boolean rememberAccountName) {
      this.rememberAccountName = rememberAccountName;
    }

    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = clean(appId);
    }

    public String getEncryptedPasswordSwitch() {
      return encryptedPasswordSwitch;
    }

    public void setEncryptedPasswordSwitch(String encryptedPasswordSwitch) {
      this.encryptedPasswordSwitch = clean(encryptedPasswordSwitch);
    }

    private String clean(String value) {
      return value == null ? "" : value.trim();
    }
  }

  public static class Iam {
    private boolean enabled = false;
    private String url = "";
    private String authorization = "";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

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
