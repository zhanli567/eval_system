package com.agentnexus.backend.task.api.dto.response;

/**
 * 评测任务指标统计概览。
 *
 * @param overallScore 综合得分
 * @param scoredDimensionCount 参与综合得分计算的维度数量
 * @param progress 评测进度统计
 */
public record TaskMetricOverview(
    Double overallScore,
    Integer scoredDimensionCount,
    TaskMetricProgress progress
) {
}
