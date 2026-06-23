package com.evalsystem.task.api.dto.response;

import java.util.List;
import java.util.Map;

public record TaskItemDetail(
    String id,
    String datasetItemId,
    Integer rowNo,
    String status,
    Map<String, String> values,
    String appOutput,
    String appOutputStatus,
    String appErrorMessage,
    List<TaskEvaluatorResultDto> evaluatorResults,
    List<TaskTagResultDto> tagResults,
    java.time.LocalDateTime createdDate,
    java.time.LocalDateTime lastUpdatedDate
) {
}
