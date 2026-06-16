CREATE TABLE IF NOT EXISTS eval_evaluator (
  id VARCHAR(64) COMMENT '自定义评估器ID',
  evaluator_name VARCHAR(50) COMMENT '评估器名称',
  evaluator_type VARCHAR(16) COMMENT '评估器类型：llm/code',
  description VARCHAR(200) COMMENT '评估器描述',
  latest_version_id VARCHAR(64) COMMENT '最新版本ID：有发布版本时指向最新发布版本，否则指向草稿版本',
  is_deleted TINYINT COMMENT '是否删除：0否，1是',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='自定义评估器主表';

CREATE TABLE IF NOT EXISTS eval_evaluator_version (
  id VARCHAR(64) COMMENT '评估器版本ID',
  evaluator_id VARCHAR(64) COMMENT '自定义评估器ID',
  version_no INT COMMENT '版本号：0草稿，>0发布版本',
  model_id VARCHAR(64) COMMENT 'LLM评估使用的模型ID，后续可作为模型表外键',
  prompt LONGTEXT COMMENT 'LLM评估Prompt',
  execute_code LONGTEXT COMMENT 'Code评估Python执行函数',
  score_min DECIMAL(10,4) COMMENT '评分范围最小值',
  score_max DECIMAL(10,4) COMMENT '评分范围最大值',
  pass_threshold DECIMAL(10,4) COMMENT '通过阈值：大于等于该阈值为Pass',
  is_deleted TINYINT COMMENT '是否删除：0否，1是',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='自定义评估器版本表';

CREATE TABLE IF NOT EXISTS eval_evaluator_param (
  id VARCHAR(64) COMMENT '评估器参数ID',
  target_type VARCHAR(16) COMMENT '参数所属类型：version自定义评估器版本',
  target_id VARCHAR(64) COMMENT '参数所属ID：自定义评估器版本ID',
  param_name VARCHAR(64) COMMENT '变量名',
  data_type VARCHAR(32) COMMENT '数据类型：string/number/boolean',
  default_value LONGTEXT COMMENT '默认值',
  is_required TINYINT COMMENT '是否必填：0否，1是',
  description VARCHAR(200) COMMENT '参数描述，用于评测任务绑定时理解参数含义',
  display_order INT COMMENT '展示顺序',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评估器参数配置表';
