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
  void buildsAgentPresetDetails() {
    assertAgentPreset(
        "memory_accuracy",
        "记忆准确性",
        "验证 Agent 记录或检索出的记忆是否事实准确。",
        "0",
        "1",
        "0.5",
        List.of("context", "history", "observation", "memory"),
        List.of(false, false, true, true));
    assertAgentPreset(
        "memory_detail_retention",
        "记忆细节保持程度",
        "检查记忆信息是否完整保留了原文的关键细节。",
        "0",
        "1",
        "0.5",
        List.of("context", "history", "observation", "memory"),
        List.of(false, false, true, true));
    assertAgentPreset(
        "reflection_accuracy",
        "反思准确性",
        "评估 Agent 自我反思或自我修正过程的正确性。",
        "0",
        "1",
        "0.5",
        List.of("context", "history", "observation", "reflection"),
        List.of(false, false, true, true));
    assertAgentPreset(
        "action_plan_alignment",
        "动作与规划一致性",
        "评估 Agent 动作是否与其计划/推理一致。",
        "0",
        "1",
        "0.5",
        List.of("context", "history", "plan", "action"),
        List.of(false, false, true, true));
    assertAgentPreset(
        "tool_selection_relevance",
        "工具选择相关性",
        "评估 Agent 选择工具和问题的相关性。",
        "1",
        "5",
        "3",
        List.of("query", "available_tools", "selected_tools"),
        List.of(true, true, true));
    assertAgentPreset(
        "tool_parameter_correctness",
        "工具参数正确性",
        "评估从查询中提取工具参数的准确性。",
        "0",
        "1",
        "0.5",
        List.of("query", "tool_definitions", "tool_calls"),
        List.of(true, true, true));
    assertAgentPreset(
        "tool_call_accuracy",
        "工具调用准确性",
        "评估Agent选择工具与问题的相关性和参数正确性。相当于工具选择相关性+工具参数准确性",
        "1",
        "5",
        "3",
        List.of("query", "tool_calls", "tool_definitions"),
        List.of(true, true, true));
    assertAgentPreset(
        "memory_retrieval_effectiveness",
        "记忆检索有效性",
        "评估检索到的记忆对当前任务的辅助程度。",
        "0",
        "1",
        "0.5",
        List.of("context", "history", "plan", "observation", "memory"),
        List.of(false, false, true, true, true));
    assertAgentPreset(
        "trajectory_accuracy",
        "轨迹准确性",
        "评估 AI 智能体轨迹（动作序列、工具调用和响应）在实现用户目标方面的整体正确性和有效性。",
        "1",
        "3",
        "2",
        List.of("messages"),
        List.of(true));
    assertAgentPreset(
        "tool_execution_success",
        "工具执行是否成功",
        "检查工具执行结果是否在技术上成功，不关注业务效果。",
        "0",
        "1",
        "0.5",
        List.of("tool_definitions", "tool_calls", "tool_responses"),
        List.of(true, true, true));
    assertAgentPreset(
        "reflection_result_understanding",
        "反思理解准确性",
        "评估 Agent 是否能准确理解并反思前一步动作的执行结果。",
        "0",
        "1",
        "0.5",
        List.of("context", "history", "observation", "reflection"),
        List.of(false, false, true, true));
    assertAgentPreset(
        "plan_feasibility",
        "规划可行性",
        "评估 Agent 提出的执行计划在当前环境下的可行性。",
        "0",
        "1",
        "0.5",
        List.of("context", "history", "plan", "observation", "memory"),
        List.of(false, false, true, true, true));
    assertAgentPreset(
        "progress_awareness_accuracy",
        "进度感知准确性",
        "评估 Agent 在反思中对任务当前进展及剩余步骤的认知是否准确。",
        "0",
        "1",
        "0.5",
        List.of("context", "history", "observation", "reflection"),
        List.of(false, false, true, true));
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

  private void assertAgentPreset(
      String id,
      String name,
      String description,
      String scoreMin,
      String scoreMax,
      String passThreshold,
      List<String> paramNames,
      List<Boolean> required
  ) {
    var detail = store.getPresetEvaluator(id);

    assertThat(detail.categoryId()).isEqualTo("agent");
    assertThat(detail.evaluatorName()).isEqualTo(name);
    assertThat(detail.evaluatorType()).isEqualTo("llm");
    assertThat(detail.description()).isEqualTo(description);
    assertThat(detail.scoreMin()).isEqualByComparingTo(scoreMin);
    assertThat(detail.scoreMax()).isEqualByComparingTo(scoreMax);
    assertThat(detail.passThreshold()).isEqualByComparingTo(passThreshold);
    assertThat(detail.prompt()).contains("请仅输出JSON对象");
    assertThat(detail.params()).extracting("paramName").containsExactlyElementsOf(paramNames);
    assertThat(detail.params()).extracting("required").containsExactlyElementsOf(required);
  }
}
