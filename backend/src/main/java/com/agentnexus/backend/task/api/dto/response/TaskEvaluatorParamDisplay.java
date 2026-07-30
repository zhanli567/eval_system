package com.agentnexus.backend.task.api.dto.response;

/**
 * 评测任务中评估器参数的展示信息。
 *
 * @param paramId 参数ID
 * @param paramName 参数名称
 * @param sourceType 参数来源类型
 * @param sourceName 参数来源名称
 * @param datasetFieldId 评测集字段ID
 * @param appOutputName 应用输出字段名称
 * @param value 当前数据行下的参数展示值
 * @param displayOrder 展示顺序
 */
public record TaskEvaluatorParamDisplay(
    String paramId,
    String paramName,
    String sourceType,
    String sourceName,
    String datasetFieldId,
    String appOutputName,
    String value,
    Integer displayOrder
) {
}
