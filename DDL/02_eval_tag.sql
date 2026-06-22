CREATE TABLE IF NOT EXISTS t_eval_tag (
  id VARCHAR(64),
  tag_name VARCHAR(50),
  tag_type VARCHAR(32),
  description VARCHAR(200),
  min_value INT,
  max_value INT,
  pass_threshold INT,
  created_at VARCHAR(32),
  updated_at VARCHAR(32)
);

COMMENT ON TABLE t_eval_tag IS '评测标签表';
COMMENT ON COLUMN t_eval_tag.id IS '标签ID';
COMMENT ON COLUMN t_eval_tag.tag_name IS '标签名称';
COMMENT ON COLUMN t_eval_tag.tag_type IS '标签类型：category分类，boolean布尔值，number数字，text文本';
COMMENT ON COLUMN t_eval_tag.description IS '标签描述';
COMMENT ON COLUMN t_eval_tag.min_value IS '数字类型评分范围最小值';
COMMENT ON COLUMN t_eval_tag.max_value IS '数字类型评分范围最大值';
COMMENT ON COLUMN t_eval_tag.pass_threshold IS '数字类型通过阈值：大于等于该值为通过';
COMMENT ON COLUMN t_eval_tag.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_tag.updated_at IS '更新时间';

CREATE TABLE IF NOT EXISTS t_eval_tag_option (
  id VARCHAR(64),
  tag_id VARCHAR(64),
  option_name VARCHAR(50),
  option_group VARCHAR(16),
  display_order INT,
  created_at VARCHAR(32),
  updated_at VARCHAR(32)
);

COMMENT ON TABLE t_eval_tag_option IS '评测标签选项表';
COMMENT ON COLUMN t_eval_tag_option.id IS '标签选项ID';
COMMENT ON COLUMN t_eval_tag_option.tag_id IS '标签ID';
COMMENT ON COLUMN t_eval_tag_option.option_name IS '选项名称：分类选项名或布尔值True/False';
COMMENT ON COLUMN t_eval_tag_option.option_group IS '选项分组：pass通过，fail失败';
COMMENT ON COLUMN t_eval_tag_option.display_order IS '展示顺序';
COMMENT ON COLUMN t_eval_tag_option.created_at IS '创建时间';
COMMENT ON COLUMN t_eval_tag_option.updated_at IS '更新时间';
