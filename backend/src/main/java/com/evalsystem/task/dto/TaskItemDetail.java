package com.evalsystem.task.dto;

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
    List<TaskEvaluatorResultDto> evaluatorResults,
    List<TaskTagResultDto> tagResults,
    String createdAt,
    String updatedAt
) {
}
