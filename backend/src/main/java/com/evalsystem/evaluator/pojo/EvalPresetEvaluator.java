package com.evalsystem.evaluator.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("eval_preset_evaluator")
public class EvalPresetEvaluator {
  private String id;
  private String categoryId;
  private String evaluatorName;
  private String evaluatorType;
  private String description;
  private String modelId;
  private String prompt;
  private String executeCode;
  private BigDecimal scoreMin;
  private BigDecimal scoreMax;
  private BigDecimal passThreshold;
  private Integer displayOrder;
  private String createdAt;
  private String updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public String getEvaluatorName() {
    return evaluatorName;
  }

  public void setEvaluatorName(String evaluatorName) {
    this.evaluatorName = evaluatorName;
  }

  public String getEvaluatorType() {
    return evaluatorType;
  }

  public void setEvaluatorType(String evaluatorType) {
    this.evaluatorType = evaluatorType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getModelId() {
    return modelId;
  }

  public void setModelId(String modelId) {
    this.modelId = modelId;
  }

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public String getExecuteCode() {
    return executeCode;
  }

  public void setExecuteCode(String executeCode) {
    this.executeCode = executeCode;
  }

  public BigDecimal getScoreMin() {
    return scoreMin;
  }

  public void setScoreMin(BigDecimal scoreMin) {
    this.scoreMin = scoreMin;
  }

  public BigDecimal getScoreMax() {
    return scoreMax;
  }

  public void setScoreMax(BigDecimal scoreMax) {
    this.scoreMax = scoreMax;
  }

  public BigDecimal getPassThreshold() {
    return passThreshold;
  }

  public void setPassThreshold(BigDecimal passThreshold) {
    this.passThreshold = passThreshold;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }
}
