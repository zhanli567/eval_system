package com.evalsystem.evaluator.preset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PresetEvaluatorStoreTest {
  private final PresetEvaluatorStore store = new PresetEvaluatorStore();

  @Test
  void listsOnlyRealPresetCategoriesInDisplayOrder() {
    var categories = store.listCategories();

    assertThat(categories).extracting("id")
        .containsExactly("general_quality", "agent", "text_match", "text_similarity", "format_check");
    assertThat(categories).extracting("categoryName")
        .containsExactly("通用质量", "智能体", "文本匹配", "文本相似度", "格式校验");
  }

  @Test
  void listsPresetEvaluatorsByCategoryKeywordAndPage() {
    var page = store.listEvaluators(1, 10, "general_quality", "一致");

    assertThat(page.total()).isEqualTo(1);
    assertThat(page.records()).hasSize(1);
    assertThat(page.records().getFirst().id()).isEqualTo("answer_consistency");
    assertThat(page.records().getFirst().categoryName()).isEqualTo("通用质量");
    assertThat(page.records().getFirst().evaluatorType()).isEqualTo("llm");
  }

  @Test
  void buildsLlmPresetDetailWithStableExtractedParams() {
    var detail = store.getPresetEvaluator("answer_consistency");

    assertThat(detail.evaluatorName()).isEqualTo("回复一致性");
    assertThat(detail.evaluatorType()).isEqualTo("llm");
    assertThat(detail.scoreMin()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(detail.scoreMax()).isEqualByComparingTo(BigDecimal.valueOf(5));
    assertThat(detail.passThreshold()).isEqualByComparingTo(BigDecimal.valueOf(3));
    assertThat(detail.prompt()).contains("${query}", "${context}", "${reference_response}", "${response}");
    assertThat(detail.prompt().length()).isLessThanOrEqualTo(2000);
    assertThat(detail.params()).extracting("id")
        .containsExactly(
            "answer_consistency:query",
            "answer_consistency:context",
            "answer_consistency:reference_response",
            "answer_consistency:response");
    assertThat(detail.params()).extracting("paramName")
        .containsExactly("query", "context", "reference_response", "response");
    assertThat(detail.params()).extracting("dataType")
        .containsExactly("string", "string", "string", "string");
    assertThat(detail.params()).allSatisfy(param -> {
      assertThat(param.targetType()).isEqualTo("preset");
      assertThat(param.targetId()).isEqualTo("answer_consistency");
      assertThat(param.required()).isTrue();
    });
  }

  @Test
  void buildsCodePresetDetailWithConfiguredParamsAndCode() {
    var detail = store.getPresetEvaluator("number_accuracy");

    assertThat(detail.categoryId()).isEqualTo("text_match");
    assertThat(detail.evaluatorName()).isEqualTo("数值准确性");
    assertThat(detail.evaluatorType()).isEqualTo("code");
    assertThat(detail.executeCode()).contains("def evaluate(params");
    assertThat(detail.scoreMin()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(detail.scoreMax()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(detail.passThreshold()).isEqualByComparingTo("0.5");
    assertThat(detail.params()).extracting("id")
        .containsExactly(
            "number_accuracy:reference_response",
            "number_accuracy:response",
            "number_accuracy:tolerance");
    assertThat(detail.params()).extracting("dataType")
        .containsExactly("string", "string", "number");
  }

  @Test
  void rejectsUnknownPresetId() {
    assertThatThrownBy(() -> store.getPresetEvaluator("missing"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("预置评估器不存在");
  }
}
