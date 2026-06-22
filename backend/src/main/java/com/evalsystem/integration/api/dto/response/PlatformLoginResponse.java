package com.evalsystem.integration.api.dto.response;

import java.util.Map;

public record PlatformLoginResponse(
    Integer statusCode,
    String statusText,
    Map<String, Object> accountInfo,
    Object result,
    Object mfaAccountInfoRespVo
) {
}
