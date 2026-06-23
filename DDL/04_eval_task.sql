CREATE TABLE IF NOT EXISTS t_eval_task (
  id VARCHAR(36),
  space_id VARCHAR(36),
  task_name VARCHAR(50),
  status VARCHAR(32),
  description VARCHAR(200),
  dataset_id VARCHAR(64),
  dataset_version_id VARCHAR(64),
  item_count INT,
  app_type VARCHAR(16),
  app_id VARCHAR(64),
  app_version_id VARCHAR(64),
  app_agent_alias VARCHAR(128),
  started_at VARCHAR(32),
  finished_at VARCHAR(32),
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

ALTER TABLE t_eval_task ADD COLUMN IF NOT EXISTS app_agent_alias VARCHAR(128);

COMMENT ON TABLE t_eval_task IS '评测任务主表';
COMMENT ON COLUMN t_eval_task.id IS '评测任务ID';
COMMENT ON COLUMN t_eval_task.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_task.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_task.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_task.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_task.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_task.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_task.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_task.task_name IS '任务名称';
COMMENT ON COLUMN t_eval_task.status IS '评测状态：pending待执行，running进行中，completed评测完成，failed评测失败';
COMMENT ON COLUMN t_eval_task.description IS '描述';
COMMENT ON COLUMN t_eval_task.dataset_id IS '评测集ID';
COMMENT ON COLUMN t_eval_task.dataset_version_id IS '评测集版本ID';
COMMENT ON COLUMN t_eval_task.item_count IS '评测数据总行数';
COMMENT ON COLUMN t_eval_task.app_type IS '应用类型：none不关联应用，agent智能体';
COMMENT ON COLUMN t_eval_task.app_id IS '智能体应用ID，未关联应用时为空';
COMMENT ON COLUMN t_eval_task.app_version_id IS '智能体应用版本ID，未关联应用时为空';
COMMENT ON COLUMN t_eval_task.started_at IS '开始执行时间';
COMMENT ON COLUMN t_eval_task.finished_at IS '结束执行时间';
COMMENT ON COLUMN t_eval_task.is_deleted IS '是否删除：0否，1是';
COMMENT ON COLUMN t_eval_task.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_task.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS t_eval_task_app_field_mapping (
  id VARCHAR(36),
  space_id VARCHAR(36),
  task_id VARCHAR(64),
  app_input_id VARCHAR(64),
  app_input_name VARCHAR(64),
  app_input_type VARCHAR(32),
  dataset_version_id VARCHAR(64),
  dataset_field_id VARCHAR(64),
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

COMMENT ON TABLE t_eval_task_app_field_mapping IS '评测任务应用入参字段映射表';
COMMENT ON COLUMN t_eval_task_app_field_mapping.id IS '应用字段映射ID';
COMMENT ON COLUMN t_eval_task_app_field_mapping.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_task_app_field_mapping.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_task_app_field_mapping.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_task_app_field_mapping.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_task_app_field_mapping.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_task_app_field_mapping.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_task_app_field_mapping.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_task_app_field_mapping.task_id IS '评测任务ID';
COMMENT ON COLUMN t_eval_task_app_field_mapping.app_input_id IS '智能体输入变量ID，暂未接入应用字段表时为空';
COMMENT ON COLUMN t_eval_task_app_field_mapping.app_input_name IS '智能体输入变量名';
COMMENT ON COLUMN t_eval_task_app_field_mapping.app_input_type IS '智能体输入变量类型：string/number/boolean';
COMMENT ON COLUMN t_eval_task_app_field_mapping.dataset_version_id IS '评测集版本ID';
COMMENT ON COLUMN t_eval_task_app_field_mapping.dataset_field_id IS '映射的评测集字段ID';
COMMENT ON COLUMN t_eval_task_app_field_mapping.display_order IS '展示顺序';
COMMENT ON COLUMN t_eval_task_app_field_mapping.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_task_app_field_mapping.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS t_eval_task_evaluator (
  id VARCHAR(36),
  space_id VARCHAR(36),
  task_id VARCHAR(64),
  evaluator_source VARCHAR(16),
  evaluator_id VARCHAR(64),
  evaluator_version_id VARCHAR(64),
  model_id VARCHAR(64),
  status VARCHAR(32),
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

COMMENT ON TABLE t_eval_task_evaluator IS '评测任务评估器绑定表';
COMMENT ON COLUMN t_eval_task_evaluator.id IS '任务评估器ID';
COMMENT ON COLUMN t_eval_task_evaluator.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_task_evaluator.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_task_evaluator.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_task_evaluator.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_task_evaluator.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_task_evaluator.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_task_evaluator.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_task_evaluator.task_id IS '评测任务ID';
COMMENT ON COLUMN t_eval_task_evaluator.evaluator_source IS '评估器来源：preset预置评估器，custom自定义评估器';
COMMENT ON COLUMN t_eval_task_evaluator.evaluator_id IS '评估器ID：预置评估器ID或自定义评估器ID';
COMMENT ON COLUMN t_eval_task_evaluator.evaluator_version_id IS '自定义评估器版本ID，预置评估器为空';
COMMENT ON COLUMN t_eval_task_evaluator.model_id IS '预置LLM评估器在任务中绑定的模型ID';
COMMENT ON COLUMN t_eval_task_evaluator.status IS '评估器执行状态：pending待执行，running进行中，completed完成，failed失败';
COMMENT ON COLUMN t_eval_task_evaluator.display_order IS '展示顺序';
COMMENT ON COLUMN t_eval_task_evaluator.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_task_evaluator.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS t_eval_task_evaluator_param_mapping (
  id VARCHAR(36),
  space_id VARCHAR(36),
  task_id VARCHAR(64),
  task_evaluator_id VARCHAR(64),
  param_id VARCHAR(64),
  param_name VARCHAR(64),
  source_type VARCHAR(32),
  dataset_version_id VARCHAR(64),
  dataset_field_id VARCHAR(64),
  app_output_name VARCHAR(64),
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

COMMENT ON TABLE t_eval_task_evaluator_param_mapping IS '评测任务评估器参数映射表';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.id IS '评估器参数映射ID';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.task_id IS '评测任务ID';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.task_evaluator_id IS '任务评估器ID';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.param_id IS '评估器参数ID，Prompt自动提取且未入库时为空';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.param_name IS '评估器变量名，param_id为空时使用';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.source_type IS '映射来源：dataset_field评测集字段，app_output应用输出';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.dataset_version_id IS '评测集版本ID，source_type为dataset_field时使用';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.dataset_field_id IS '评测集字段ID，source_type为dataset_field时使用';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.app_output_name IS '应用输出字段名，source_type为app_output时使用，单一输出可为空';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.display_order IS '展示顺序';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_task_evaluator_param_mapping.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS t_eval_task_tag (
  id VARCHAR(36),
  space_id VARCHAR(36),
  task_id VARCHAR(64),
  tag_id VARCHAR(64),
  status VARCHAR(32),
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

COMMENT ON TABLE t_eval_task_tag IS '评测任务标签绑定表';
COMMENT ON COLUMN t_eval_task_tag.id IS '任务标签ID';
COMMENT ON COLUMN t_eval_task_tag.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_task_tag.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_task_tag.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_task_tag.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_task_tag.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_task_tag.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_task_tag.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_task_tag.task_id IS '评测任务ID';
COMMENT ON COLUMN t_eval_task_tag.tag_id IS '标签ID';
COMMENT ON COLUMN t_eval_task_tag.status IS '人工标注状态：pending待标注，annotating标注中，completed标注完成';
COMMENT ON COLUMN t_eval_task_tag.display_order IS '展示顺序';
COMMENT ON COLUMN t_eval_task_tag.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_task_tag.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS t_eval_task_item (
  id VARCHAR(36),
  space_id VARCHAR(36),
  task_id VARCHAR(64),
  dataset_version_id VARCHAR(64),
  dataset_item_id VARCHAR(64),
  row_no INT,
  status VARCHAR(32),
  app_output TEXT,
  app_output_status VARCHAR(32),
  app_error_message TEXT,
  started_at VARCHAR(32),
  finished_at VARCHAR(32),
  created_at VARCHAR(32),
  updated_at VARCHAR(32),
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_task_item IS '评测任务数据行结果表';
COMMENT ON COLUMN t_eval_task_item.id IS '任务数据行ID';
COMMENT ON COLUMN t_eval_task_item.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_task_item.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_task_item.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_task_item.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_task_item.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_task_item.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_task_item.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_task_item.task_id IS '评测任务ID';
COMMENT ON COLUMN t_eval_task_item.dataset_version_id IS '评测集版本ID';
COMMENT ON COLUMN t_eval_task_item.dataset_item_id IS '评测集数据行ID';
COMMENT ON COLUMN t_eval_task_item.row_no IS '评测集行号';
COMMENT ON COLUMN t_eval_task_item.status IS '数据评测状态：pending待评测，running评测中，annotation_pending待人工标注，completed完成，failed失败';
COMMENT ON COLUMN t_eval_task_item.app_output IS '应用输出内容，未关联应用时为空';
COMMENT ON COLUMN t_eval_task_item.app_output_status IS '应用调用状态：pending待调用，running调用中，completed完成，failed失败，skipped跳过';
COMMENT ON COLUMN t_eval_task_item.app_error_message IS '应用调用错误信息';
COMMENT ON COLUMN t_eval_task_item.started_at IS '单行评测开始时间';
COMMENT ON COLUMN t_eval_task_item.finished_at IS '单行评测结束时间';
COMMENT ON COLUMN t_eval_task_item.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_task_item.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS t_eval_task_evaluator_result (
  id VARCHAR(36),
  space_id VARCHAR(36),
  task_id VARCHAR(64),
  task_item_id VARCHAR(64),
  task_evaluator_id VARCHAR(64),
  status VARCHAR(32),
  score DECIMAL(10,4),
  pass_result VARCHAR(16),
  result_value TEXT,
  error_message TEXT,
  started_at VARCHAR(32),
  finished_at VARCHAR(32),
  created_at VARCHAR(32),
  updated_at VARCHAR(32),
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_task_evaluator_result IS '评测任务评估器结果表';
COMMENT ON COLUMN t_eval_task_evaluator_result.id IS '评估器结果ID';
COMMENT ON COLUMN t_eval_task_evaluator_result.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_task_evaluator_result.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_task_evaluator_result.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_task_evaluator_result.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_task_evaluator_result.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_task_evaluator_result.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_task_evaluator_result.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_task_evaluator_result.task_id IS '评测任务ID';
COMMENT ON COLUMN t_eval_task_evaluator_result.task_item_id IS '任务数据行ID';
COMMENT ON COLUMN t_eval_task_evaluator_result.task_evaluator_id IS '任务评估器ID';
COMMENT ON COLUMN t_eval_task_evaluator_result.status IS '评估状态：pending待评估，running评估中，completed完成，failed失败，skipped跳过';
COMMENT ON COLUMN t_eval_task_evaluator_result.score IS '评估得分';
COMMENT ON COLUMN t_eval_task_evaluator_result.pass_result IS '通过结果：pass通过，fail失败';
COMMENT ON COLUMN t_eval_task_evaluator_result.result_value IS '评估器输出结果或解释';
COMMENT ON COLUMN t_eval_task_evaluator_result.error_message IS '评估失败错误信息';
COMMENT ON COLUMN t_eval_task_evaluator_result.started_at IS '评估开始时间';
COMMENT ON COLUMN t_eval_task_evaluator_result.finished_at IS '评估结束时间';
COMMENT ON COLUMN t_eval_task_evaluator_result.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_task_evaluator_result.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS t_eval_task_tag_result (
  id VARCHAR(36),
  space_id VARCHAR(36),
  task_id VARCHAR(64),
  task_item_id VARCHAR(64),
  task_tag_id VARCHAR(64),
  status VARCHAR(32),
  value_text TEXT,
  value_number DECIMAL(10,4),
  tag_option_id VARCHAR(64),
  pass_result VARCHAR(16),
  annotator_id VARCHAR(64),
  annotator_name VARCHAR(50),
  annotated_at VARCHAR(32),
  created_at VARCHAR(32),
  updated_at VARCHAR(32),
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_task_tag_result IS '评测任务人工标签结果表';
COMMENT ON COLUMN t_eval_task_tag_result.id IS '标签标注结果ID';
COMMENT ON COLUMN t_eval_task_tag_result.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_task_tag_result.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_task_tag_result.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_task_tag_result.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_task_tag_result.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_task_tag_result.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_task_tag_result.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_task_tag_result.task_id IS '评测任务ID';
COMMENT ON COLUMN t_eval_task_tag_result.task_item_id IS '任务数据行ID';
COMMENT ON COLUMN t_eval_task_tag_result.task_tag_id IS '任务标签ID';
COMMENT ON COLUMN t_eval_task_tag_result.status IS '标注状态：pending待标注，completed已标注';
COMMENT ON COLUMN t_eval_task_tag_result.value_text IS '文本、分类或布尔标签标注值';
COMMENT ON COLUMN t_eval_task_tag_result.value_number IS '数字标签标注值';
COMMENT ON COLUMN t_eval_task_tag_result.tag_option_id IS '分类或布尔标签选中的原标签选项ID';
COMMENT ON COLUMN t_eval_task_tag_result.pass_result IS '通过结果：pass通过，fail失败';
COMMENT ON COLUMN t_eval_task_tag_result.annotator_id IS '标注人ID，暂未接入用户体系时为空';
COMMENT ON COLUMN t_eval_task_tag_result.annotator_name IS '标注人名称，暂未接入用户体系时为空';
COMMENT ON COLUMN t_eval_task_tag_result.annotated_at IS '标注完成时间';
COMMENT ON COLUMN t_eval_task_tag_result.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_task_tag_result.updated_at IS '更新时间';
