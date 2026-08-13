package com.agentnexus.backend.evaluation.jiuwen;

import com.agentnexus.backend.evaluation.EvaluationOutcome;
import com.agentnexus.backend.evaluation.EvaluatorSnapshot;
import com.openjiuwen.agent_evolving.dataset.EvaluatedCase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** 将 Jiuwen 的 0～1 评分映射回 Eval 评估器配置的原始评分区间。 */
public final class JiuwenResultMapper {

  public EvaluationOutcome map(EvaluatedCase evaluated, EvaluatorSnapshot snapshot) {
    BigDecimal normalized = BigDecimal.valueOf(evaluated.getScore());
    BigDecimal rawScore = snapshot.scoreMin().add(
        snapshot.scoreMax().subtract(snapshot.scoreMin()).multiply(normalized))
        .setScale(Math.max(snapshot.scoreMin().scale(), snapshot.scoreMax().scale()) + 2, RoundingMode.HALF_UP);
    Map<String, Object> rawResult = Map.of(
        "answer", evaluated.getAnswer() == null ? Map.of() : evaluated.getAnswer(),
        "jiuwenScore", evaluated.getScore());
    return EvaluationOutcome.completed(
        rawScore,
        snapshot.scoreMin(),
        snapshot.scoreMax(),
        snapshot.passThreshold(),
        evaluated.getReason(),
        evaluated.getPerMetric(),
        rawResult);
  }
}
