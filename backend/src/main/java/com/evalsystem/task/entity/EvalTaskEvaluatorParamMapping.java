package com.evalsystem.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("t_eval_task_evaluator_param_mapping")
public class EvalTaskEvaluatorParamMapping {
  @TableId(type = IdType.INPUT)
  private String id;
  private String taskId;
  private String taskEvaluatorId;
  private String paramId;
  private String paramName;
  private String sourceType;
  private String datasetVersionId;
  private String datasetFieldId;
  private String appOutputName;
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

  public String getTaskEvaluatorId() {
    return taskEvaluatorId;
  }

  public void setTaskEvaluatorId(String taskEvaluatorId) {
    this.taskEvaluatorId = taskEvaluatorId;
  }

  public String getParamId() {
    return paramId;
  }

  public void setParamId(String paramId) {
    this.paramId = paramId;
  }

  public String getParamName() {
    return paramName;
  }

  public void setParamName(String paramName) {
    this.paramName = paramName;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public String getDatasetVersionId() {
    return datasetVersionId;
  }

  public void setDatasetVersionId(String datasetVersionId) {
    this.datasetVersionId = datasetVersionId;
  }

  public String getDatasetFieldId() {
    return datasetFieldId;
  }

  public void setDatasetFieldId(String datasetFieldId) {
    this.datasetFieldId = datasetFieldId;
  }

  public String getAppOutputName() {
    return appOutputName;
  }

  public void setAppOutputName(String appOutputName) {
    this.appOutputName = appOutputName;
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
