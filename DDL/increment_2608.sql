BEGIN;

ALTER TABLE t_eval_task
  ADD COLUMN IF NOT EXISTS app_name VARCHAR(128) NOT NULL DEFAULT '';

ALTER TABLE t_eval_task
  ADD COLUMN IF NOT EXISTS app_version_name VARCHAR(128) NOT NULL DEFAULT '';

COMMENT ON COLUMN t_eval_task.app_name IS '智能体应用名称，任务创建时保存';
COMMENT ON COLUMN t_eval_task.app_version_name IS '智能体应用版本名称，任务创建时保存';

-- 移除数据库显式约束，业务校验放在前端和服务端处理。
ALTER TABLE t_eval_dataset DROP CONSTRAINT IF EXISTS uq_t_eval_dataset_space_name;
ALTER TABLE t_eval_dataset DROP CONSTRAINT IF EXISTS ck_t_eval_dataset_published_count;
ALTER TABLE t_eval_dataset_version DROP CONSTRAINT IF EXISTS uq_t_eval_dataset_version_dataset_no;
ALTER TABLE t_eval_dataset_version DROP CONSTRAINT IF EXISTS ck_t_eval_dataset_version_no;
ALTER TABLE t_eval_dataset_version DROP CONSTRAINT IF EXISTS ck_t_eval_dataset_version_item_count;
ALTER TABLE t_eval_dataset_field DROP CONSTRAINT IF EXISTS uq_t_eval_dataset_field_name;
ALTER TABLE t_eval_dataset_field DROP CONSTRAINT IF EXISTS ck_t_eval_dataset_field_type;
ALTER TABLE t_eval_dataset_field DROP CONSTRAINT IF EXISTS ck_t_eval_dataset_field_required;
ALTER TABLE t_eval_dataset_field DROP CONSTRAINT IF EXISTS ck_t_eval_dataset_field_order;
ALTER TABLE t_eval_dataset_item DROP CONSTRAINT IF EXISTS uq_t_eval_dataset_item_row;
ALTER TABLE t_eval_dataset_item DROP CONSTRAINT IF EXISTS ck_t_eval_dataset_item_row_no;
ALTER TABLE t_eval_dataset_item_cell DROP CONSTRAINT IF EXISTS uq_t_eval_dataset_item_cell_field;
ALTER TABLE t_eval_tag DROP CONSTRAINT IF EXISTS uq_t_eval_tag_space_name;
ALTER TABLE t_eval_tag DROP CONSTRAINT IF EXISTS ck_t_eval_tag_type;
ALTER TABLE t_eval_tag DROP CONSTRAINT IF EXISTS ck_t_eval_tag_number_config;
ALTER TABLE t_eval_tag_option DROP CONSTRAINT IF EXISTS uq_t_eval_tag_option_name;
ALTER TABLE t_eval_tag_option DROP CONSTRAINT IF EXISTS uq_t_eval_tag_option_order;
ALTER TABLE t_eval_tag_option DROP CONSTRAINT IF EXISTS ck_t_eval_tag_option_group;
ALTER TABLE t_eval_tag_option DROP CONSTRAINT IF EXISTS ck_t_eval_tag_option_order;
ALTER TABLE t_eval_evaluator DROP CONSTRAINT IF EXISTS uq_t_eval_evaluator_space_name;
ALTER TABLE t_eval_evaluator DROP CONSTRAINT IF EXISTS ck_t_eval_evaluator_type;
ALTER TABLE t_eval_evaluator_version DROP CONSTRAINT IF EXISTS uq_t_eval_evaluator_version_no;
ALTER TABLE t_eval_evaluator_version DROP CONSTRAINT IF EXISTS ck_t_eval_evaluator_version_no;
ALTER TABLE t_eval_evaluator_version DROP CONSTRAINT IF EXISTS ck_t_eval_evaluator_version_score;
ALTER TABLE t_eval_evaluator_param DROP CONSTRAINT IF EXISTS uq_t_eval_evaluator_param_name;
ALTER TABLE t_eval_evaluator_param DROP CONSTRAINT IF EXISTS uq_t_eval_evaluator_param_order;
ALTER TABLE t_eval_evaluator_param DROP CONSTRAINT IF EXISTS ck_t_eval_evaluator_param_target;
ALTER TABLE t_eval_evaluator_param DROP CONSTRAINT IF EXISTS ck_t_eval_evaluator_param_data_type;
ALTER TABLE t_eval_evaluator_param DROP CONSTRAINT IF EXISTS ck_t_eval_evaluator_param_required;
ALTER TABLE t_eval_evaluator_param DROP CONSTRAINT IF EXISTS ck_t_eval_evaluator_param_order;
ALTER TABLE t_eval_task DROP CONSTRAINT IF EXISTS uq_t_eval_task_space_name;
ALTER TABLE t_eval_task DROP CONSTRAINT IF EXISTS ck_t_eval_task_status;
ALTER TABLE t_eval_task DROP CONSTRAINT IF EXISTS ck_t_eval_task_app_type;
ALTER TABLE t_eval_task DROP CONSTRAINT IF EXISTS ck_t_eval_task_item_count;
ALTER TABLE t_eval_task_app_field_mapping DROP CONSTRAINT IF EXISTS uq_t_eval_task_app_field_input;
ALTER TABLE t_eval_task_app_field_mapping DROP CONSTRAINT IF EXISTS uq_t_eval_task_app_field_order;
ALTER TABLE t_eval_task_app_field_mapping DROP CONSTRAINT IF EXISTS ck_t_eval_task_app_field_type;
ALTER TABLE t_eval_task_app_field_mapping DROP CONSTRAINT IF EXISTS ck_t_eval_task_app_field_order;
ALTER TABLE t_eval_task_evaluator DROP CONSTRAINT IF EXISTS uq_t_eval_task_evaluator_binding;
ALTER TABLE t_eval_task_evaluator DROP CONSTRAINT IF EXISTS uq_t_eval_task_evaluator_order;
ALTER TABLE t_eval_task_evaluator DROP CONSTRAINT IF EXISTS ck_t_eval_task_evaluator_source;
ALTER TABLE t_eval_task_evaluator DROP CONSTRAINT IF EXISTS ck_t_eval_task_evaluator_status;
ALTER TABLE t_eval_task_evaluator DROP CONSTRAINT IF EXISTS ck_t_eval_task_evaluator_order;
ALTER TABLE t_eval_task_evaluator_param_mapping DROP CONSTRAINT IF EXISTS uq_t_eval_task_evaluator_param_name;
ALTER TABLE t_eval_task_evaluator_param_mapping DROP CONSTRAINT IF EXISTS uq_t_eval_task_evaluator_param_order;
ALTER TABLE t_eval_task_evaluator_param_mapping DROP CONSTRAINT IF EXISTS ck_t_eval_task_evaluator_param_source;
ALTER TABLE t_eval_task_evaluator_param_mapping DROP CONSTRAINT IF EXISTS ck_t_eval_task_evaluator_param_order;
ALTER TABLE t_eval_task_tag DROP CONSTRAINT IF EXISTS uq_t_eval_task_tag_binding;
ALTER TABLE t_eval_task_tag DROP CONSTRAINT IF EXISTS uq_t_eval_task_tag_order;
ALTER TABLE t_eval_task_tag DROP CONSTRAINT IF EXISTS ck_t_eval_task_tag_status;
ALTER TABLE t_eval_task_tag DROP CONSTRAINT IF EXISTS ck_t_eval_task_tag_order;
ALTER TABLE t_eval_task_item DROP CONSTRAINT IF EXISTS uq_t_eval_task_item_dataset_item;
ALTER TABLE t_eval_task_item DROP CONSTRAINT IF EXISTS uq_t_eval_task_item_row;
ALTER TABLE t_eval_task_item DROP CONSTRAINT IF EXISTS ck_t_eval_task_item_row_no;
ALTER TABLE t_eval_task_item DROP CONSTRAINT IF EXISTS ck_t_eval_task_item_status;
ALTER TABLE t_eval_task_item DROP CONSTRAINT IF EXISTS ck_t_eval_task_item_app_status;
ALTER TABLE t_eval_task_evaluator_result DROP CONSTRAINT IF EXISTS uq_t_eval_task_evaluator_result_item;
ALTER TABLE t_eval_task_evaluator_result DROP CONSTRAINT IF EXISTS ck_t_eval_task_evaluator_result_status;
ALTER TABLE t_eval_task_evaluator_result DROP CONSTRAINT IF EXISTS ck_t_eval_task_evaluator_result_pass;
ALTER TABLE t_eval_task_tag_result DROP CONSTRAINT IF EXISTS uq_t_eval_task_tag_result_item;
ALTER TABLE t_eval_task_tag_result DROP CONSTRAINT IF EXISTS ck_t_eval_task_tag_result_status;
ALTER TABLE t_eval_task_tag_result DROP CONSTRAINT IF EXISTS ck_t_eval_task_tag_result_pass;

COMMENT ON COLUMN t_eval_task.status IS '评测状态：pending待执行，running进行中，completed评测完成，failed评测失败，stopped已中止';
COMMENT ON COLUMN t_eval_task_evaluator.status IS '评估器执行状态：pending待执行，running进行中，completed完成，failed失败，stopped已中止';
COMMENT ON COLUMN t_eval_task_tag.status IS '人工标注状态：pending待标注，annotating标注中，completed标注完成，stopped已中止';
COMMENT ON COLUMN t_eval_task_item.status IS '数据评测状态：pending待评测，running评测中，annotation_pending待人工标注，completed完成，failed失败，stopped已中止';
COMMENT ON COLUMN t_eval_task_item.app_output_status IS '应用调用状态：pending待调用，running调用中，completed完成，failed失败，skipped跳过，stopped已中止';
COMMENT ON COLUMN t_eval_task_evaluator_result.status IS '评估状态：pending待评估，running评估中，completed完成，failed失败，skipped跳过，stopped已中止';
COMMENT ON COLUMN t_eval_task_tag_result.status IS '标注状态：pending待标注，completed已标注，stopped已中止';

COMMIT;
