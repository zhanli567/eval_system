BEGIN;

ALTER TABLE t_eval_task
  ADD COLUMN IF NOT EXISTS app_name VARCHAR(128) NOT NULL DEFAULT '';

ALTER TABLE t_eval_task
  ADD COLUMN IF NOT EXISTS app_version_name VARCHAR(128) NOT NULL DEFAULT '';

COMMENT ON COLUMN t_eval_task.app_name IS '智能体应用名称，任务创建时保存';
COMMENT ON COLUMN t_eval_task.app_version_name IS '智能体应用版本名称，任务创建时保存';

ALTER TABLE t_eval_task DROP CONSTRAINT IF EXISTS ck_t_eval_task_status;
ALTER TABLE t_eval_task
  ADD CONSTRAINT ck_t_eval_task_status
  CHECK (status IN ('pending', 'running', 'completed', 'failed', 'stopped'));

ALTER TABLE t_eval_task_evaluator DROP CONSTRAINT IF EXISTS ck_t_eval_task_evaluator_status;
ALTER TABLE t_eval_task_evaluator
  ADD CONSTRAINT ck_t_eval_task_evaluator_status
  CHECK (status IN ('pending', 'running', 'completed', 'failed', 'stopped'));

ALTER TABLE t_eval_task_tag DROP CONSTRAINT IF EXISTS ck_t_eval_task_tag_status;
ALTER TABLE t_eval_task_tag
  ADD CONSTRAINT ck_t_eval_task_tag_status
  CHECK (status IN ('pending', 'annotating', 'completed', 'stopped'));

ALTER TABLE t_eval_task_item DROP CONSTRAINT IF EXISTS ck_t_eval_task_item_status;
ALTER TABLE t_eval_task_item
  ADD CONSTRAINT ck_t_eval_task_item_status
  CHECK (status IN ('pending', 'running', 'annotation_pending', 'completed', 'failed', 'stopped'));

ALTER TABLE t_eval_task_item DROP CONSTRAINT IF EXISTS ck_t_eval_task_item_app_status;
ALTER TABLE t_eval_task_item
  ADD CONSTRAINT ck_t_eval_task_item_app_status
  CHECK (app_output_status IN ('pending', 'running', 'completed', 'failed', 'skipped', 'stopped'));

ALTER TABLE t_eval_task_evaluator_result DROP CONSTRAINT IF EXISTS ck_t_eval_task_evaluator_result_status;
ALTER TABLE t_eval_task_evaluator_result
  ADD CONSTRAINT ck_t_eval_task_evaluator_result_status
  CHECK (status IN ('pending', 'running', 'completed', 'failed', 'skipped', 'stopped'));

ALTER TABLE t_eval_task_tag_result DROP CONSTRAINT IF EXISTS ck_t_eval_task_tag_result_status;
ALTER TABLE t_eval_task_tag_result
  ADD CONSTRAINT ck_t_eval_task_tag_result_status
  CHECK (status IN ('pending', 'completed', 'stopped'));

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

ALTER TABLE t_eval_tag DROP CONSTRAINT IF EXISTS ck_t_eval_tag_number_config;
ALTER TABLE t_eval_tag
  ADD CONSTRAINT ck_t_eval_tag_number_config
  CHECK (
    tag_type <> 'number'
    OR (
      min_value IS NOT NULL
      AND max_value IS NOT NULL
      AND pass_threshold IS NOT NULL
      AND min_value BETWEEN -100000 AND 100000
      AND max_value BETWEEN -100000 AND 100000
      AND pass_threshold BETWEEN -100000 AND 100000
      AND max_value > min_value
      AND pass_threshold BETWEEN min_value AND max_value
    )
  );

ALTER TABLE t_eval_evaluator_version DROP CONSTRAINT IF EXISTS ck_t_eval_evaluator_version_score;
ALTER TABLE t_eval_evaluator_version
  ADD CONSTRAINT ck_t_eval_evaluator_version_score
  CHECK (
    score_min BETWEEN -100000 AND 100000
    AND score_max BETWEEN -100000 AND 100000
    AND pass_threshold BETWEEN -100000 AND 100000
    AND score_min < score_max
    AND pass_threshold BETWEEN score_min AND score_max
  );

COMMIT;
