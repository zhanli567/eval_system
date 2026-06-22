package com.evalsystem.evaluator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("t_eval_evaluator_version")
public class EvalEvaluatorVersion {
  @TableId(type = IdType.INPUT)
  private String id;
  private String evaluatorId;
  private Integer versionNo;
  private String modelId;
  private String prompt;
  private String executeCode;
  private BigDecimal scoreMin;
  private BigDecimal scoreMax;
  private BigDecimal passThreshold;
  @TableField("is_deleted")
  private Integer isDeleted;
  private String createdAt;
  private String updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getEvaluatorId() {
    return evaluatorId;
  }

  public void setEvaluatorId(String evaluatorId) {
    this.evaluatorId = evaluatorId;
  }

  public Integer getVersionNo() {
    return versionNo;
  }

  public void setVersionNo(Integer versionNo) {
    this.versionNo = versionNo;
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

  public Integer getIsDeleted() {
    return isDeleted;
  }

  public void setIsDeleted(Integer isDeleted) {
    this.isDeleted = isDeleted;
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
