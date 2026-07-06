package com.agentnexus.backend.task.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_eval_task")
public class EvalTask {
  @TableId(type = IdType.INPUT)
  private String id;
  private String spaceId;
  private String createdByName;
  private String createdBy;
  private LocalDateTime createdDate;
  private String lastUpdatedBy;
  private String lastUpdatedByName;
  private LocalDateTime lastUpdatedDate;
  private String taskName;
  private String status;
  private String description;
  private String datasetId;
  private String datasetVersionId;
  private Integer itemCount;
  private String appType;
  private String appId;
  private String appVersionId;
  private String appAgentAlias;
  private String startedAt;
  private String finishedAt;
  @TableField("is_deleted")
  private Integer isDeleted;
}
