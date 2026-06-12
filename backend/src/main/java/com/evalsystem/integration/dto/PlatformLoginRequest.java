package com.evalsystem.integration.dto;

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
