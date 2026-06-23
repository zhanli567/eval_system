package com.evalsystem.evaluator.preset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record PresetEvaluatorDefinition(
    String id,
    String categoryId,
    String evaluatorName,
    String evaluatorType,
    String description,
    String modelId,
    String prompt,
    String executeCode,
    BigDecimal scoreMin,
    BigDecimal scoreMax,
    BigDecimal passThreshold,
    Integer displayOrder,
    List<PresetParamDefinition> params,
    String createdDate,
    String lastUpdatedDate
) {
  private static final String TYPE_LLM = "llm";
  private static final String TYPE_CODE = "code";
  private static final String BUILT_IN_TIME = "1781539200000";

  public static Builder llm(String id, String categoryId, String evaluatorName) {
    return new Builder(id, categoryId, evaluatorName, TYPE_LLM);
  }

  public static Builder code(String id, String categoryId, String evaluatorName) {
    return new Builder(id, categoryId, evaluatorName, TYPE_CODE);
  }

  public static class Builder {
    private final String id;
    private final String categoryId;
    private final String evaluatorName;
    private final String evaluatorType;
    private String description = "";
    private String modelId = "";
    private String prompt = "";
    private String executeCode = "";
    private BigDecimal scoreMin = BigDecimal.ONE;
    private BigDecimal scoreMax = BigDecimal.valueOf(5);
    private BigDecimal passThreshold = BigDecimal.valueOf(3);
    private Integer displayOrder = 1;
    private final List<PresetParamDefinition> params = new ArrayList<>();

    private Builder(String id, String categoryId, String evaluatorName, String evaluatorType) {
      this.id = id;
      this.categoryId = categoryId;
      this.evaluatorName = evaluatorName;
      this.evaluatorType = evaluatorType;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder modelId(String modelId) {
      this.modelId = modelId;
      return this;
    }

    public Builder prompt(String prompt) {
      this.prompt = prompt;
      return this;
    }

    public Builder executeCode(String executeCode) {
      this.executeCode = executeCode;
      return this;
    }

    public Builder score(String scoreMin, String scoreMax, String passThreshold) {
      this.scoreMin = new BigDecimal(scoreMin);
      this.scoreMax = new BigDecimal(scoreMax);
      this.passThreshold = new BigDecimal(passThreshold);
      return this;
    }

    public Builder displayOrder(Integer displayOrder) {
      this.displayOrder = displayOrder;
      return this;
    }

    public Builder params(PresetParamDefinition... params) {
      this.params.clear();
      this.params.addAll(Arrays.asList(params));
      return this;
    }

    public PresetEvaluatorDefinition build() {
      return new PresetEvaluatorDefinition(
          id,
          categoryId,
          evaluatorName,
          evaluatorType,
          description,
          modelId,
          prompt,
          executeCode,
          scoreMin,
          scoreMax,
          passThreshold,
          displayOrder,
          List.copyOf(params),
          BUILT_IN_TIME,
          BUILT_IN_TIME);
    }
  }
}
