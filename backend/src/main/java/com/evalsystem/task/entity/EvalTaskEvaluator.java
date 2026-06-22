package com.evalsystem.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("t_eval_task_evaluator")
public class EvalTaskEvaluator {
  @TableId(type = IdType.INPUT)
  private String id;
  private String taskId;
  private String evaluatorSource;
  private String evaluatorId;
  private String evaluatorVersionId;
  private String modelId;
  private String status;
  private Integer displayOrder;
  private String createdAt;
  private String updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getEvaluatorSource() {
    return evaluatorSource;
  }

  public void setEvaluatorSource(String evaluatorSource) {
    this.evaluatorSource = evaluatorSource;
  }

  public String getEvaluatorId() {
    return evaluatorId;
  }

  public void setEvaluatorId(String evaluatorId) {
    this.evaluatorId = evaluatorId;
  }

  public String getEvaluatorVersionId() {
    return evaluatorVersionId;
  }

  public void setEvaluatorVersionId(String evaluatorVersionId) {
    this.evaluatorVersionId = evaluatorVersionId;
  }

  public String getModelId() {
    return modelId;
  }

  public void setModelId(String modelId) {
    this.modelId = modelId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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
