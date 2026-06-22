package com.evalsystem.integration.api.dto.request;

public record PlatformLoginRequest(
    String loginAccount,
    String uid,
    String password,
    String lang,
    Boolean rememberAccountName,
    String appId,
    String encryptedPasswordSwitch
) {
}
