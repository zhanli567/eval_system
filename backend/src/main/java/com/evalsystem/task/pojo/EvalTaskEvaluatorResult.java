package com.evalsystem.task.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("eval_task_evaluator_result")
public class EvalTaskEvaluatorResult {
  @TableId(type = IdType.INPUT)
  private String id;
  private String taskId;
  private String taskItemId;
  private String taskEvaluatorId;
  private String status;
  private BigDecimal score;
  private String passResult;
  private String resultValue;
  private String errorMessage;
  private String startedAt;
  private String finishedAt;
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

  public String getTaskEvaluatorId() {
    return taskEvaluatorId;
  }

  public void setTaskEvaluatorId(String taskEvaluatorId) {
    this.taskEvaluatorId = taskEvaluatorId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public BigDecimal getScore() {
    return score;
  }

  public void setScore(BigDecimal score) {
    this.score = score;
  }

  public String getPassResult() {
    return passResult;
  }

  public void setPassResult(String passResult) {
    this.passResult = passResult;
  }

  public String getResultValue() {
    return resultValue;
  }

  public void setResultValue(String resultValue) {
    this.resultValue = resultValue;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(String startedAt) {
    this.startedAt = startedAt;
  }

  public String getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(String finishedAt) {
    this.finishedAt = finishedAt;
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
