package com.agentnexus.backend.task.api.dto.response;

import java.util.List;

/**
 * 评测任务得分汇总。
 *
 * @param dimensions 维度得分列表
 */
public record TaskMetricScoreSummary(List<TaskMetricDimensionScore> dimensions) {
}
