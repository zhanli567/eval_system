package com.agentnexus.backend.task.api.dto.response;

/**
 * 评测任务进度统计。
 *
 * @param progressRate 评测进度百分比
 * @param totalCount 评测集总量
 * @param completedCount 已完成量
 * @param incompleteCount 未完成量
 */
public record TaskMetricProgress(
    Double progressRate,
    Long totalCount,
    Long completedCount,
    Long incompleteCount
) {
}
