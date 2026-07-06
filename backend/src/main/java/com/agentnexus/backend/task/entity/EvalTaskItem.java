package com.agentnexus.backend.task.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Getter
@Setter
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
}
