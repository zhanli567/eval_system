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

UPDATE t_eval_dataset_item_cell cell
SET cell_value = CASE
  WHEN LOWER(TRIM(cell.cell_value)) = 'true' THEN 'TRUE'
  WHEN LOWER(TRIM(cell.cell_value)) = 'false' THEN 'FALSE'
  ELSE cell.cell_value
END
FROM t_eval_dataset_field field
WHERE cell.space_id = field.space_id
  AND cell.field_id = field.id
  AND field.field_type = 'boolean'
  AND LOWER(TRIM(cell.cell_value)) IN ('true', 'false')
  AND cell.cell_value NOT IN ('TRUE', 'FALSE');

WITH invalid_tags AS (
  SELECT id
  FROM t_eval_tag
  WHERE tag_type = 'number'
    AND (
      min_value IS NULL
      OR max_value IS NULL
      OR pass_threshold IS NULL
      OR min_value NOT BETWEEN -100000 AND 100000
      OR max_value NOT BETWEEN -100000 AND 100000
      OR pass_threshold NOT BETWEEN -100000 AND 100000
      OR max_value <= min_value
      OR pass_threshold NOT BETWEEN min_value AND max_value
    )
),
invalid_task_tags AS (
  SELECT id
  FROM t_eval_task_tag
  WHERE tag_id IN (SELECT id FROM invalid_tags)
)
DELETE FROM t_eval_task_tag_result
WHERE task_tag_id IN (SELECT id FROM invalid_task_tags);

WITH invalid_tags AS (
  SELECT id
  FROM t_eval_tag
  WHERE tag_type = 'number'
    AND (
      min_value IS NULL
      OR max_value IS NULL
      OR pass_threshold IS NULL
      OR min_value NOT BETWEEN -100000 AND 100000
      OR max_value NOT BETWEEN -100000 AND 100000
      OR pass_threshold NOT BETWEEN -100000 AND 100000
      OR max_value <= min_value
      OR pass_threshold NOT BETWEEN min_value AND max_value
    )
)
DELETE FROM t_eval_task_tag
WHERE tag_id IN (SELECT id FROM invalid_tags);

WITH invalid_tags AS (
  SELECT id
  FROM t_eval_tag
  WHERE tag_type = 'number'
    AND (
      min_value IS NULL
      OR max_value IS NULL
      OR pass_threshold IS NULL
      OR min_value NOT BETWEEN -100000 AND 100000
      OR max_value NOT BETWEEN -100000 AND 100000
      OR pass_threshold NOT BETWEEN -100000 AND 100000
      OR max_value <= min_value
      OR pass_threshold NOT BETWEEN min_value AND max_value
    )
)
DELETE FROM t_eval_tag_option
WHERE tag_id IN (SELECT id FROM invalid_tags);

DELETE FROM t_eval_tag
WHERE tag_type = 'number'
  AND (
    min_value IS NULL
    OR max_value IS NULL
    OR pass_threshold IS NULL
    OR min_value NOT BETWEEN -100000 AND 100000
    OR max_value NOT BETWEEN -100000 AND 100000
    OR pass_threshold NOT BETWEEN -100000 AND 100000
    OR max_value <= min_value
    OR pass_threshold NOT BETWEEN min_value AND max_value
  );

WITH invalid_versions AS (
  SELECT id
  FROM t_eval_evaluator_version
  WHERE score_min NOT BETWEEN -100000 AND 100000
    OR score_max NOT BETWEEN -100000 AND 100000
    OR pass_threshold NOT BETWEEN -100000 AND 100000
    OR score_min >= score_max
    OR pass_threshold NOT BETWEEN score_min AND score_max
),
invalid_task_evaluators AS (
  SELECT id
  FROM t_eval_task_evaluator
  WHERE evaluator_source = 'custom'
    AND evaluator_version_id IN (SELECT id FROM invalid_versions)
)
DELETE FROM t_eval_task_evaluator_result
WHERE task_evaluator_id IN (SELECT id FROM invalid_task_evaluators);

WITH invalid_versions AS (
  SELECT id
  FROM t_eval_evaluator_version
  WHERE score_min NOT BETWEEN -100000 AND 100000
    OR score_max NOT BETWEEN -100000 AND 100000
    OR pass_threshold NOT BETWEEN -100000 AND 100000
    OR score_min >= score_max
    OR pass_threshold NOT BETWEEN score_min AND score_max
),
invalid_task_evaluators AS (
  SELECT id
  FROM t_eval_task_evaluator
  WHERE evaluator_source = 'custom'
    AND evaluator_version_id IN (SELECT id FROM invalid_versions)
)
DELETE FROM t_eval_task_evaluator_param_mapping
WHERE task_evaluator_id IN (SELECT id FROM invalid_task_evaluators);

WITH invalid_versions AS (
  SELECT id
  FROM t_eval_evaluator_version
  WHERE score_min NOT BETWEEN -100000 AND 100000
    OR score_max NOT BETWEEN -100000 AND 100000
    OR pass_threshold NOT BETWEEN -100000 AND 100000
    OR score_min >= score_max
    OR pass_threshold NOT BETWEEN score_min AND score_max
)
DELETE FROM t_eval_task_evaluator
WHERE evaluator_source = 'custom'
  AND evaluator_version_id IN (SELECT id FROM invalid_versions);

WITH invalid_versions AS (
  SELECT id
  FROM t_eval_evaluator_version
  WHERE score_min NOT BETWEEN -100000 AND 100000
    OR score_max NOT BETWEEN -100000 AND 100000
    OR pass_threshold NOT BETWEEN -100000 AND 100000
    OR score_min >= score_max
    OR pass_threshold NOT BETWEEN score_min AND score_max
)
DELETE FROM t_eval_evaluator_param
WHERE target_type = 'version'
  AND target_id IN (SELECT id FROM invalid_versions);

DELETE FROM t_eval_evaluator_version
WHERE score_min NOT BETWEEN -100000 AND 100000
  OR score_max NOT BETWEEN -100000 AND 100000
  OR pass_threshold NOT BETWEEN -100000 AND 100000
  OR score_min >= score_max
  OR pass_threshold NOT BETWEEN score_min AND score_max;

UPDATE t_eval_evaluator evaluator
SET latest_version_id = latest_version.id
FROM (
  SELECT DISTINCT ON (evaluator_id) evaluator_id, id
  FROM t_eval_evaluator_version
  ORDER BY evaluator_id, version_no DESC, created_date DESC, id DESC
) latest_version
WHERE evaluator.id = latest_version.evaluator_id
  AND NOT EXISTS (
    SELECT 1
    FROM t_eval_evaluator_version current_version
    WHERE current_version.evaluator_id = evaluator.id
      AND current_version.id = evaluator.latest_version_id
  );

DELETE FROM t_eval_evaluator evaluator
WHERE NOT EXISTS (
  SELECT 1
  FROM t_eval_evaluator_version version
  WHERE version.evaluator_id = evaluator.id
);

COMMIT;
