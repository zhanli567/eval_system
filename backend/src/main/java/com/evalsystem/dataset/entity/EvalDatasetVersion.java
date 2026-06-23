package com.evalsystem.dataset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("t_eval_dataset_version")
public class EvalDatasetVersion {
  @TableId(type = IdType.INPUT)
  private String id;
  private String spaceId;
  private String createdByName;
  private String createdBy;
  private LocalDateTime createdDate;
  private String lastUpdatedBy;
  private String lastUpdatedByName;
  private LocalDateTime lastUpdatedDate;
  private String datasetId;
  private Integer versionNo;
  private Integer itemCount;
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

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public Integer getVersionNo() {
    return versionNo;
  }

  public void setVersionNo(Integer versionNo) {
    this.versionNo = versionNo;
  }

  public Integer getItemCount() {
    return itemCount;
  }

  public void setItemCount(Integer itemCount) {
    this.itemCount = itemCount;
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
