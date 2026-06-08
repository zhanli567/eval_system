package com.evalsystem.evaluator.pojo;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("eval_evaluator")
public class EvalEvaluator {
  private String id;
  private String evaluatorName;
  private String evaluatorType;
  private String description;
  private String latestVersionId;
  private Integer isDeleted;
  private String createdAt;
  private String updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public String getLatestVersionId() {
    return latestVersionId;
  }

  public void setLatestVersionId(String latestVersionId) {
    this.latestVersionId = latestVersionId;
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
