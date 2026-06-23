CREATE TABLE IF NOT EXISTS t_eval_evaluator (
  id VARCHAR(36),
  space_id VARCHAR(36),
  evaluator_name VARCHAR(50),
  evaluator_type VARCHAR(16),
  description VARCHAR(200),
  latest_version_id VARCHAR(64),
  is_deleted SMALLINT,
  created_at VARCHAR(32),
  updated_at VARCHAR(32),
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_evaluator IS '自定义评估器主表';
COMMENT ON COLUMN t_eval_evaluator.id IS '自定义评估器ID';
COMMENT ON COLUMN t_eval_evaluator.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_evaluator.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_evaluator.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_evaluator.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_evaluator.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_evaluator.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_evaluator.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_evaluator.evaluator_name IS '评估器名称';
COMMENT ON COLUMN t_eval_evaluator.evaluator_type IS '评估器类型：llm/code';
COMMENT ON COLUMN t_eval_evaluator.description IS '评估器描述';
COMMENT ON COLUMN t_eval_evaluator.latest_version_id IS '最新版本ID：有发布版本时指向最新发布版本，否则指向草稿版本';
COMMENT ON COLUMN t_eval_evaluator.is_deleted IS '是否删除：0否，1是';
COMMENT ON COLUMN t_eval_evaluator.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_evaluator.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS t_eval_evaluator_version (
  id VARCHAR(36),
  space_id VARCHAR(36),
  evaluator_id VARCHAR(64),
  version_no INT,
  model_id VARCHAR(64),
  prompt TEXT,
  execute_code TEXT,
  score_min DECIMAL(10,4),
  score_max DECIMAL(10,4),
  pass_threshold DECIMAL(10,4),
  is_deleted SMALLINT,
  created_at VARCHAR(32),
  updated_at VARCHAR(32),
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_evaluator_version IS '自定义评估器版本表';
COMMENT ON COLUMN t_eval_evaluator_version.id IS '评估器版本ID';
COMMENT ON COLUMN t_eval_evaluator_version.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_evaluator_version.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_evaluator_version.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_evaluator_version.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_evaluator_version.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_evaluator_version.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_evaluator_version.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_evaluator_version.evaluator_id IS '自定义评估器ID';
COMMENT ON COLUMN t_eval_evaluator_version.version_no IS '版本号：0草稿，>0发布版本';
COMMENT ON COLUMN t_eval_evaluator_version.model_id IS 'LLM评估使用的模型ID，后续可作为模型表外键';
COMMENT ON COLUMN t_eval_evaluator_version.prompt IS 'LLM评估Prompt';
COMMENT ON COLUMN t_eval_evaluator_version.execute_code IS 'Code评估Python执行函数';
COMMENT ON COLUMN t_eval_evaluator_version.score_min IS '评分范围最小值';
COMMENT ON COLUMN t_eval_evaluator_version.score_max IS '评分范围最大值';
COMMENT ON COLUMN t_eval_evaluator_version.pass_threshold IS '通过阈值：大于等于该阈值为Pass';
COMMENT ON COLUMN t_eval_evaluator_version.is_deleted IS '是否删除：0否，1是';
COMMENT ON COLUMN t_eval_evaluator_version.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_evaluator_version.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS t_eval_evaluator_param (
  id VARCHAR(36),
  space_id VARCHAR(36),
  target_type VARCHAR(16),
  target_id VARCHAR(64),
  param_name VARCHAR(64),
  data_type VARCHAR(32),
  default_value TEXT,
  is_required SMALLINT,
  description VARCHAR(200),
  display_order INT,
  created_at VARCHAR(32),
  updated_at VARCHAR(32),
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_evaluator_param IS '评估器参数配置表';
COMMENT ON COLUMN t_eval_evaluator_param.id IS '评估器参数ID';
COMMENT ON COLUMN t_eval_evaluator_param.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_evaluator_param.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_evaluator_param.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_evaluator_param.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_evaluator_param.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_evaluator_param.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_evaluator_param.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_evaluator_param.target_type IS '参数所属类型：version自定义评估器版本';
COMMENT ON COLUMN t_eval_evaluator_param.target_id IS '参数所属ID：自定义评估器版本ID';
COMMENT ON COLUMN t_eval_evaluator_param.param_name IS '变量名';
COMMENT ON COLUMN t_eval_evaluator_param.data_type IS '数据类型：string/number/boolean';
COMMENT ON COLUMN t_eval_evaluator_param.default_value IS '默认值';
COMMENT ON COLUMN t_eval_evaluator_param.is_required IS '是否必填：0否，1是';
COMMENT ON COLUMN t_eval_evaluator_param.description IS '参数描述，用于评测任务绑定时理解参数含义';
COMMENT ON COLUMN t_eval_evaluator_param.display_order IS '展示顺序';
COMMENT ON COLUMN t_eval_evaluator_param.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_evaluator_param.updated_at IS '更新时间';
