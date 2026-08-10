package com.agentnexus.backend.task.api.dto.response;

/**
 * 评测任务维度得分统计。
 *
 * @param dimensionType 维度类型
 * @param dimensionId 维度ID
 * @param dimensionName 维度名称
 * @param versionName 维度版本
 * @param displayName 展示名称
 * @param passCount 通过数量
 * @param failedCount 未通过数量
 * @param pendingCount 未完成数量
 * @param completedCount 已完成数量
 * @param totalCount 总数量
 * @param passRate 通过率
 * @param displayOrder 展示顺序
 */
public record TaskMetricDimensionScore(
    String dimensionType,
    String dimensionId,
    String dimensionName,
    String versionName,
    String displayName,
    Integer passCount,
    Integer failedCount,
    Integer pendingCount,
    Integer completedCount,
    Integer totalCount,
    Double passRate,
    Integer displayOrder
) {
}
