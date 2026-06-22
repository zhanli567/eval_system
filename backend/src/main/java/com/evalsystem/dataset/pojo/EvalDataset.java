package com.evalsystem.dataset.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("eval_dataset")
public class EvalDataset {
  @TableId(type = IdType.INPUT)
  private String id;
  private String name;
  private String description;
  private Integer publishedVersionCount;
  private String latestPublishedVersionId;
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getPublishedVersionCount() {
    return publishedVersionCount;
  }

  public void setPublishedVersionCount(Integer publishedVersionCount) {
    this.publishedVersionCount = publishedVersionCount;
  }

  public String getLatestPublishedVersionId() {
    return latestPublishedVersionId;
  }

  public void setLatestPublishedVersionId(String latestPublishedVersionId) {
    this.latestPublishedVersionId = latestPublishedVersionId;
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
