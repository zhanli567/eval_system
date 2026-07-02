package com.agentnexus.backend.evaluator.preset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
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
    });
    assertThat(detail.params()).extracting("required")
        .containsExactly(true, false, true, true);
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
  void buildsAdditionalGeneralQualityPresetDetails() {
    assertGeneralQualityPreset(
        "hallucination_detection",
        "幻觉现象",
        "检测回复中是否存在虚假或幻觉信息",
        List.of("query", "context", "reference_response", "response"),
        List.of(true, false, false, true));
    assertGeneralQualityPreset(
        "instruction_following",
        "指令遵循程度",
        "评估回复是否严格遵守了给定的指令、格式与约束",
        List.of("instruction", "query", "response"),
        List.of(true, false, true));
    assertGeneralQualityPreset(
        "answer_relevance",
        "问答相关性",
        "评估模型回复与用户查询的相关性和完整性",
        List.of("query", "context", "reference_response", "response"),
        List.of(true, false, false, true));
    assertGeneralQualityPreset(
        "safety",
        "安全性",
        "识别回复中是否包含有害或不当内容",
        List.of("query", "context", "reference_response", "response"),
        List.of(true, false, false, true));
  }

  @Test
  void rejectsUnknownPresetId() {
    assertThatThrownBy(() -> store.getPresetEvaluator("missing"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("预置评估器不存在");
  }

  private void assertGeneralQualityPreset(
      String id,
      String name,
      String description,
      List<String> paramNames,
      List<Boolean> required
  ) {
    var detail = store.getPresetEvaluator(id);

    assertThat(detail.categoryId()).isEqualTo("general_quality");
    assertThat(detail.evaluatorName()).isEqualTo(name);
    assertThat(detail.evaluatorType()).isEqualTo("llm");
    assertThat(detail.description()).isEqualTo(description);
    assertThat(detail.scoreMin()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(detail.scoreMax()).isEqualByComparingTo(BigDecimal.valueOf(5));
    assertThat(detail.passThreshold()).isEqualByComparingTo(BigDecimal.valueOf(3));
    assertThat(detail.prompt()).contains("请仅输出JSON对象");
    assertThat(detail.params()).extracting("paramName").containsExactlyElementsOf(paramNames);
    assertThat(detail.params()).extracting("required").containsExactlyElementsOf(required);
  }
}
