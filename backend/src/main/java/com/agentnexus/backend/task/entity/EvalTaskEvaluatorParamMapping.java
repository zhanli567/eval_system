package com.agentnexus.backend.task.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_eval_task_evaluator_param_mapping")
public class EvalTaskEvaluatorParamMapping {
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
  private String taskEvaluatorId;
  private String paramId;
  private String paramName;
  private String sourceType;
  private String datasetVersionId;
  private String datasetFieldId;
  private String appOutputName;
  private Integer displayOrder;
}
