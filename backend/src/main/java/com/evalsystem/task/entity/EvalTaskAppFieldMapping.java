package com.evalsystem.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("t_eval_task_app_field_mapping")
public class EvalTaskAppFieldMapping {
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
  private String appInputId;
  private String appInputName;
  private String appInputType;
  private String datasetVersionId;
  private String datasetFieldId;
  private Integer displayOrder;

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

  public String getAppInputId() {
    return appInputId;
  }

  public void setAppInputId(String appInputId) {
    this.appInputId = appInputId;
  }

  public String getAppInputName() {
    return appInputName;
  }

  public void setAppInputName(String appInputName) {
    this.appInputName = appInputName;
  }

  public String getAppInputType() {
    return appInputType;
  }

  public void setAppInputType(String appInputType) {
    this.appInputType = appInputType;
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

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
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
