package com.agentnexus.backend.task.api.dto.response;

import java.util.List;

/**
 * 评测任务数据项分布统计。
 *
 * @param dimensions 维度数据项分布列表
 */
public record TaskMetricItemDistribution(List<TaskMetricDimensionScore> dimensions) {
}
