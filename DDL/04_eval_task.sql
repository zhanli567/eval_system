CREATE TABLE IF NOT EXISTS eval_task (
  id VARCHAR(64) COMMENT '评测任务ID',
  task_name VARCHAR(50) COMMENT '任务名称',
  status VARCHAR(32) COMMENT '评测状态：pending待执行，running进行中，completed评测完成，failed评测失败',
  description VARCHAR(200) COMMENT '描述',
  dataset_id VARCHAR(64) COMMENT '评测集ID',
  dataset_version_id VARCHAR(64) COMMENT '评测集版本ID',
  item_count INT COMMENT '评测数据总行数',
  app_type VARCHAR(16) COMMENT '应用类型：none不关联应用，agent智能体',
  app_id VARCHAR(64) COMMENT '智能体应用ID，未关联应用时为空',
  app_version_id VARCHAR(64) COMMENT '智能体应用版本ID，未关联应用时为空',
  started_at VARCHAR(32) COMMENT '开始执行时间',
  finished_at VARCHAR(32) COMMENT '结束执行时间',
  is_deleted TINYINT COMMENT '是否删除：0否，1是',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测任务主表';

CREATE TABLE IF NOT EXISTS eval_task_app_field_mapping (
  id VARCHAR(64) COMMENT '应用字段映射ID',
  task_id VARCHAR(64) COMMENT '评测任务ID',
  app_input_id VARCHAR(64) COMMENT '智能体输入变量ID，暂未接入应用字段表时为空',
  app_input_name VARCHAR(64) COMMENT '智能体输入变量名',
  app_input_type VARCHAR(32) COMMENT '智能体输入变量类型：string/number/boolean',
  dataset_version_id VARCHAR(64) COMMENT '评测集版本ID',
  dataset_field_id VARCHAR(64) COMMENT '映射的评测集字段ID',
  display_order INT COMMENT '展示顺序',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测任务应用入参字段映射表';

CREATE TABLE IF NOT EXISTS eval_task_evaluator (
  id VARCHAR(64) COMMENT '任务评估器ID',
  task_id VARCHAR(64) COMMENT '评测任务ID',
  evaluator_source VARCHAR(16) COMMENT '评估器来源：preset预置评估器，custom自定义评估器',
  evaluator_id VARCHAR(64) COMMENT '评估器ID：预置评估器ID或自定义评估器ID',
  evaluator_version_id VARCHAR(64) COMMENT '自定义评估器版本ID，预置评估器为空',
  status VARCHAR(32) COMMENT '评估器执行状态：pending待执行，running进行中，completed完成，failed失败',
  display_order INT COMMENT '展示顺序',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测任务评估器绑定表';

CREATE TABLE IF NOT EXISTS eval_task_evaluator_param_mapping (
  id VARCHAR(64) COMMENT '评估器参数映射ID',
  task_id VARCHAR(64) COMMENT '评测任务ID',
  task_evaluator_id VARCHAR(64) COMMENT '任务评估器ID',
  param_id VARCHAR(64) COMMENT '评估器参数ID，Prompt自动提取且未入库时为空',
  param_name VARCHAR(64) COMMENT '评估器变量名，param_id为空时使用',
  source_type VARCHAR(32) COMMENT '映射来源：dataset_field评测集字段，app_output应用输出',
  dataset_version_id VARCHAR(64) COMMENT '评测集版本ID，source_type为dataset_field时使用',
  dataset_field_id VARCHAR(64) COMMENT '评测集字段ID，source_type为dataset_field时使用',
  app_output_name VARCHAR(64) COMMENT '应用输出字段名，source_type为app_output时使用，单一输出可为空',
  display_order INT COMMENT '展示顺序',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测任务评估器参数映射表';

CREATE TABLE IF NOT EXISTS eval_task_tag (
  id VARCHAR(64) COMMENT '任务标签ID',
  task_id VARCHAR(64) COMMENT '评测任务ID',
  tag_id VARCHAR(64) COMMENT '标签ID',
  status VARCHAR(32) COMMENT '人工标注状态：pending待标注，annotating标注中，completed标注完成',
  display_order INT COMMENT '展示顺序',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测任务标签绑定表';

CREATE TABLE IF NOT EXISTS eval_task_item (
  id VARCHAR(64) COMMENT '任务数据行ID',
  task_id VARCHAR(64) COMMENT '评测任务ID',
  dataset_version_id VARCHAR(64) COMMENT '评测集版本ID',
  dataset_item_id VARCHAR(64) COMMENT '评测集数据行ID',
  row_no INT COMMENT '评测集行号',
  status VARCHAR(32) COMMENT '数据评测状态：pending待评测，running评测中，annotation_pending待人工标注，completed完成，failed失败',
  app_output LONGTEXT COMMENT '应用输出内容，未关联应用时为空',
  app_output_status VARCHAR(32) COMMENT '应用调用状态：pending待调用，running调用中，completed完成，failed失败，skipped跳过',
  app_error_message LONGTEXT COMMENT '应用调用错误信息',
  started_at VARCHAR(32) COMMENT '单行评测开始时间',
  finished_at VARCHAR(32) COMMENT '单行评测结束时间',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测任务数据行结果表';

CREATE TABLE IF NOT EXISTS eval_task_evaluator_result (
  id VARCHAR(64) COMMENT '评估器结果ID',
  task_id VARCHAR(64) COMMENT '评测任务ID',
  task_item_id VARCHAR(64) COMMENT '任务数据行ID',
  task_evaluator_id VARCHAR(64) COMMENT '任务评估器ID',
  status VARCHAR(32) COMMENT '评估状态：pending待评估，running评估中，completed完成，failed失败，skipped跳过',
  score DECIMAL(10,4) COMMENT '评估得分',
  pass_result VARCHAR(16) COMMENT '通过结果：pass通过，fail失败',
  result_value LONGTEXT COMMENT '评估器输出结果或解释',
  error_message LONGTEXT COMMENT '评估失败错误信息',
  started_at VARCHAR(32) COMMENT '评估开始时间',
  finished_at VARCHAR(32) COMMENT '评估结束时间',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测任务评估器结果表';

CREATE TABLE IF NOT EXISTS eval_task_tag_result (
  id VARCHAR(64) COMMENT '标签标注结果ID',
  task_id VARCHAR(64) COMMENT '评测任务ID',
  task_item_id VARCHAR(64) COMMENT '任务数据行ID',
  task_tag_id VARCHAR(64) COMMENT '任务标签ID',
  status VARCHAR(32) COMMENT '标注状态：pending待标注，completed已标注',
  value_text LONGTEXT COMMENT '文本、分类或布尔标签标注值',
  value_number DECIMAL(10,4) COMMENT '数字标签标注值',
  tag_option_id VARCHAR(64) COMMENT '分类或布尔标签选中的原标签选项ID',
  pass_result VARCHAR(16) COMMENT '通过结果：pass通过，fail失败',
  annotator_id VARCHAR(64) COMMENT '标注人ID，暂未接入用户体系时为空',
  annotator_name VARCHAR(50) COMMENT '标注人名称，暂未接入用户体系时为空',
  annotated_at VARCHAR(32) COMMENT '标注完成时间',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测任务人工标签结果表';
