package com.agentnexus.backend.evaluator.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_eval_evaluator_param")
public class EvalEvaluatorParam {
  @TableId(type = IdType.INPUT)
  private String id;
  private String spaceId;
  private String createdByName;
  private String createdBy;
  private LocalDateTime createdDate;
  private String lastUpdatedBy;
  private String lastUpdatedByName;
  private LocalDateTime lastUpdatedDate;
  private String targetType;
  private String targetId;
  private String paramName;
  private String dataType;
  private String defaultValue;
  @TableField("is_required")
  private Integer isRequired;
  private String description;
  private Integer displayOrder;
}
