package com.agentnexus.backend.remoteCall.api.dto.response;

import com.agentnexus.backend.remoteCall.api.dto.response.ChatCompletionChunk.Choice;
import java.util.List;
import java.util.Map;

/**
 * Aggregated agent chat response after stream parsing.
 * Created on 2026-08-04.
 *
 * @param id response id
 * @param conversationId conversation id
 * @param masterAgent master agent payload
 * @param metaAgent meta agent payload
 * @param userId user id
 * @param object response object type
 * @param created created timestamp
 * @param model model name
 * @param choices merged streamed choices
 * @param status invocation status
 * @param outputs extracted agent output fields
 * @param latencyMs invocation latency
 * @param errorMessage error message
 * @param rawOutput raw stream output
 */
public record AgentChatResponse(
    String id,
    String conversationId,
    String masterAgent,
    String metaAgent,
    String userId,
    String object,
    Long created,
    String model,
    List<Choice> choices,
    String status,
    Map<String, String> outputs,
    Long latencyMs,
    String errorMessage,
    String rawOutput
) {
}
