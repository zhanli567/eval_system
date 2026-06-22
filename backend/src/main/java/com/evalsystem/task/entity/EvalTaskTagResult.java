package com.evalsystem.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("t_eval_task_tag_result")
public class EvalTaskTagResult {
  @TableId(type = IdType.INPUT)
  private String id;
  private String taskId;
  private String taskItemId;
  private String taskTagId;
  private String status;
  private String valueText;
  private BigDecimal valueNumber;
  private String tagOptionId;
  private String passResult;
  private String annotatorId;
  private String annotatorName;
  private String annotatedAt;
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

  public String getTaskItemId() {
    return taskItemId;
  }

  public void setTaskItemId(String taskItemId) {
    this.taskItemId = taskItemId;
  }

  public String getTaskTagId() {
    return taskTagId;
  }

  public void setTaskTagId(String taskTagId) {
    this.taskTagId = taskTagId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getValueText() {
    return valueText;
  }

  public void setValueText(String valueText) {
    this.valueText = valueText;
  }

  public BigDecimal getValueNumber() {
    return valueNumber;
  }

  public void setValueNumber(BigDecimal valueNumber) {
    this.valueNumber = valueNumber;
  }

  public String getTagOptionId() {
    return tagOptionId;
  }

  public void setTagOptionId(String tagOptionId) {
    this.tagOptionId = tagOptionId;
  }

  public String getPassResult() {
    return passResult;
  }

  public void setPassResult(String passResult) {
    this.passResult = passResult;
  }

  public String getAnnotatorId() {
    return annotatorId;
  }

  public void setAnnotatorId(String annotatorId) {
    this.annotatorId = annotatorId;
  }

  public String getAnnotatorName() {
    return annotatorName;
  }

  public void setAnnotatorName(String annotatorName) {
    this.annotatorName = annotatorName;
  }

  public String getAnnotatedAt() {
    return annotatedAt;
  }

  public void setAnnotatedAt(String annotatedAt) {
    this.annotatedAt = annotatedAt;
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
