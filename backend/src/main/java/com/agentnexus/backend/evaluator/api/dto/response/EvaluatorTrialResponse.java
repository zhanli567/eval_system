package com.agentnexus.backend.evaluator.api.dto.response;

import java.math.BigDecimal;

/**
 * 评估器试运行结果。
 *
 * @param outputText 模型原始输出
 * @param result 评估结果，取值为pass或fail
 * @param score 评估得分
 * @param reason 评估原因
 * @param errorMessage 试运行错误信息
 */
public record EvaluatorTrialResponse(
    String outputText,
    String result,
    BigDecimal score,
    String reason,
    String errorMessage
) {
}
