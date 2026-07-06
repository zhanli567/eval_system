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
@TableName("t_eval_task_evaluator_result")
public class EvalTaskEvaluatorResult {
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
  private String taskEvaluatorId;
  private String status;
  private BigDecimal score;
  private String passResult;
  private String resultValue;
  private String errorMessage;
  private String startedAt;
  private String finishedAt;
}
