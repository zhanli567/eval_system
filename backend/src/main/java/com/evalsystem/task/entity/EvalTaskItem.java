package com.evalsystem.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("t_eval_task_item")
public class EvalTaskItem {
  @TableId(type = IdType.INPUT)
  private String id;
  private String spaceId;
  private String createdByName;
  private String createdBy;
  private LocalDateTime createdDate;
  private String lastUpdatedBy;
  private String lastUpdatedByName;
  private LocalDateTime lastUpdatedDate;
  private String taskId;
  private String datasetVersionId;
  private String datasetItemId;
  private Integer rowNo;
  private String status;
  private String appOutput;
  private String appOutputStatus;
  private String appErrorMessage;
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

  public String getDatasetVersionId() {
    return datasetVersionId;
  }

  public void setDatasetVersionId(String datasetVersionId) {
    this.datasetVersionId = datasetVersionId;
  }

  public String getDatasetItemId() {
    return datasetItemId;
  }

  public void setDatasetItemId(String datasetItemId) {
    this.datasetItemId = datasetItemId;
  }

  public Integer getRowNo() {
    return rowNo;
  }

  public void setRowNo(Integer rowNo) {
    this.rowNo = rowNo;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getAppOutput() {
    return appOutput;
  }

  public void setAppOutput(String appOutput) {
    this.appOutput = appOutput;
  }

  public String getAppOutputStatus() {
    return appOutputStatus;
  }

  public void setAppOutputStatus(String appOutputStatus) {
    this.appOutputStatus = appOutputStatus;
  }

  public String getAppErrorMessage() {
    return appErrorMessage;
  }

  public void setAppErrorMessage(String appErrorMessage) {
    this.appErrorMessage = appErrorMessage;
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

  public String getSpaceId() {
    return spaceId;
  }

  public void setSpaceId(String spaceId) {
    this.spaceId = spaceId;
  }

  public String getCreatedByName() {
    return createdByName;
  }

  public void setCreatedByName(String createdByName) {
    this.createdByName = createdByName;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public void setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  public String getLastUpdatedByName() {
    return lastUpdatedByName;
  }

  public void setLastUpdatedByName(String lastUpdatedByName) {
    this.lastUpdatedByName = lastUpdatedByName;
  }

  public LocalDateTime getLastUpdatedDate() {
    return lastUpdatedDate;
  }

  public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) {
    this.lastUpdatedDate = lastUpdatedDate;
  }
}
