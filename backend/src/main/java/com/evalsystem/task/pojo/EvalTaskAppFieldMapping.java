package com.evalsystem.task.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("eval_task_app_field_mapping")
public class EvalTaskAppFieldMapping {
  @TableId(type = IdType.INPUT)
  private String id;
  private String taskId;
  private String appInputId;
  private String appInputName;
  private String appInputType;
  private String datasetVersionId;
  private String datasetFieldId;
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
