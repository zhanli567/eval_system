package com.agentnexus.backend.evaluation.jiuwen;

import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openjiuwen.agent_evolving.dataset.Case;
import com.openjiuwen.agent_evolving.dataset.EvaluatedCase;
import com.openjiuwen.agent_evolving.evaluator.BaseEvaluator;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;
import org.springframework.util.StringUtils;

final class ScoreLlmEvaluator extends BaseEvaluator {
  static final String RENDERED_PROMPT_OPTION = "renderedPrompt";

  private final EvaluatorSnapshot snapshot;
  private final EvaluationModelInvoker modelInvoker;
  private final ObjectMapper objectMapper;

  ScoreLlmEvaluator(
      EvaluatorSnapshot snapshot,
      EvaluationModelInvoker modelInvoker,
      ObjectMapper objectMapper
  ) {
    this.snapshot = snapshot;
    this.modelInvoker = modelInvoker;
    this.objectMapper = objectMapper;
  }

  @Override
  public EvaluatedCase evaluate(Case caseValue, Map<String, Object> predict) {
    String prompt = renderedPrompt();
    String response = modelInvoker.invoke(snapshot.modelId(), snapshot.modelName(), prompt);
    ParsedScore parsed = parseResponse(response);
    BigDecimal normalizedScore = parsed.score()
        .subtract(snapshot.scoreMin())
        .divide(snapshot.scoreMax().subtract(snapshot.scoreMin()), MathContext.DECIMAL64);
    EvaluatedCase evaluatedCase = new EvaluatedCase(
        caseValue,
        predict,
        0.0d,
        reasonWithRangeNotice(parsed.score(), parsed.reason()),
        Map.of("llm", normalizedScore.doubleValue()));
    evaluatedCase.setScore(normalizedScore.doubleValue());
    return evaluatedCase;
  }

  private String renderedPrompt() {
    Object configured = snapshot.options().get(RENDERED_PROMPT_OPTION);
    return configured == null ? snapshot.prompt() : String.valueOf(configured);
  }

  private ParsedScore parseResponse(String response) {
    if (!StringUtils.hasText(response)) {
      throw new IllegalStateException("模型对话接口未返回评估结果");
    }
    String json = extractJson(response);
    if (!StringUtils.hasText(json)) {
      throw new IllegalStateException("模型评估结果不是JSON格式");
    }
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode scoreNode = root.get("score");
      if (scoreNode == null || scoreNode.isNull()) {
        throw new IllegalStateException("模型评估结果缺少score字段");
      }
      BigDecimal score = scoreNode.isNumber()
          ? scoreNode.decimalValue()
          : new BigDecimal(scoreNode.asText().trim());
      String reason = root.hasNonNull("reason") ? root.get("reason").asText() : response;
      return new ParsedScore(score, reason);
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("模型评估结果解析失败：" + e.getMessage(), e);
    }
  }

  private String extractJson(String response) {
    String trimmed = response == null ? "" : response.trim();
    if (trimmed.startsWith("```")) {
      trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
      int fenceIndex = trimmed.lastIndexOf("```");
      if (fenceIndex >= 0) {
        trimmed = trimmed.substring(0, fenceIndex).trim();
      }
    }
    int start = trimmed.indexOf('{');
    int end = trimmed.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return trimmed.substring(start, end + 1);
    }
    return trimmed.startsWith("{") && trimmed.endsWith("}") ? trimmed : "";
  }

  private String reasonWithRangeNotice(BigDecimal score, String reason) {
    if (score.compareTo(snapshot.scoreMin()) >= 0 && score.compareTo(snapshot.scoreMax()) <= 0) {
      return reason == null ? "" : reason;
    }
    String notice = "模型评估结果中的score超出评分范围";
    if (!StringUtils.hasText(reason)) {
      return notice;
    }
    return reason + "\n" + notice;
  }

  private record ParsedScore(BigDecimal score, String reason) {
  }
}
