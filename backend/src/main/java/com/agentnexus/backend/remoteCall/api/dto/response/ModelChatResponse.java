package com.agentnexus.backend.remoteCall.api.dto.response;

public record ModelChatResponse(
    String status,
    String url,
    ModelChatResult resultObjVO,
    Boolean success
) {
}
