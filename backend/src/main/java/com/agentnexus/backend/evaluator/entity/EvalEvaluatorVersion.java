package com.agentnexus.backend.evaluator.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@TableName("t_eval_evaluator_version")
public class EvalEvaluatorVersion {
  @TableId(type = IdType.INPUT)
  private String id;
  private String spaceId;
  private String createdByName;
  private String createdBy;
  private LocalDateTime createdDate;
  private String lastUpdatedBy;
  private String lastUpdatedByName;
  private LocalDateTime lastUpdatedDate;
  private String evaluatorId;
  private Integer versionNo;
  private String modelId;
  private String modelName;
  private String prompt;
  private String executeCode;
  private BigDecimal scoreMin;
  private BigDecimal scoreMax;
  private BigDecimal passThreshold;
}
