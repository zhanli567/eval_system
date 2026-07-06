package com.agentnexus.backend.task.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@TableName("t_eval_task_tag_result")
public class EvalTaskTagResult {
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
}
