package com.agentnexus.backend.task.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_eval_task_evaluator")
public class EvalTaskEvaluator {
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
  private String evaluatorSource;
  private String evaluatorId;
  private String evaluatorVersionId;
  private String modelId;
  private String modelName;
  private String status;
  private Integer displayOrder;
}
