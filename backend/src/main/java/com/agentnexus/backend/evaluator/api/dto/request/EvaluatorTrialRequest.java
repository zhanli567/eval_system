package com.agentnexus.backend.evaluator.api.dto.request;

import java.util.Map;

/**
 * 评估器试运行请求。
 *
 * @param evaluator 当前页面中的评估器配置
 * @param paramValues Prompt参数示例值
 */
public record EvaluatorTrialRequest(
    EvaluatorInput evaluator,
    Map<String, String> paramValues
) {
}
