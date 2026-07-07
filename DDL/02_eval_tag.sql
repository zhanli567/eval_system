CREATE TABLE IF NOT EXISTS t_eval_tag (
  id VARCHAR(36) PRIMARY KEY,
  space_id VARCHAR(36) NOT NULL DEFAULT '',
  tag_name VARCHAR(50) NOT NULL,
  tag_type VARCHAR(32) NOT NULL,
  description VARCHAR(200) NOT NULL DEFAULT '',
  min_value INT,
  max_value INT,
  pass_threshold INT,
  created_by_name VARCHAR(100) NOT NULL DEFAULT '',
  created_by VARCHAR(36) NOT NULL DEFAULT '',
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  last_updated_by VARCHAR(36) NOT NULL DEFAULT '',
  last_updated_by_name VARCHAR(100) NOT NULL DEFAULT '',
  last_updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uq_t_eval_tag_space_name UNIQUE (space_id, tag_name),
  CONSTRAINT ck_t_eval_tag_type CHECK (tag_type IN ('category', 'boolean', 'number', 'text')),
  CONSTRAINT ck_t_eval_tag_number_config CHECK (
    tag_type <> 'number'
    OR (
      min_value IS NOT NULL
      AND max_value IS NOT NULL
      AND pass_threshold IS NOT NULL
      AND min_value > 0
      AND max_value > min_value
      AND pass_threshold BETWEEN min_value AND max_value
    )
  )
);

COMMENT ON TABLE t_eval_tag IS '评测标签表';
COMMENT ON COLUMN t_eval_tag.id IS '标签ID';
COMMENT ON COLUMN t_eval_tag.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_tag.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_tag.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_tag.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_tag.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_tag.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_tag.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_tag.tag_name IS '标签名称';
COMMENT ON COLUMN t_eval_tag.tag_type IS '标签类型：category分类，boolean布尔值，number数字，text文本';
COMMENT ON COLUMN t_eval_tag.description IS '标签描述';
COMMENT ON COLUMN t_eval_tag.min_value IS '数字类型评分范围最小值';
COMMENT ON COLUMN t_eval_tag.max_value IS '数字类型评分范围最大值';
COMMENT ON COLUMN t_eval_tag.pass_threshold IS '数字类型通过阈值：大于等于该值为通过';

CREATE TABLE IF NOT EXISTS t_eval_tag_option (
  id VARCHAR(36) PRIMARY KEY,
  space_id VARCHAR(36) NOT NULL DEFAULT '',
  tag_id VARCHAR(64) NOT NULL,
  option_name VARCHAR(50) NOT NULL,
  option_group VARCHAR(16) NOT NULL,
  display_order INT NOT NULL,
  created_by_name VARCHAR(100) NOT NULL DEFAULT '',
  created_by VARCHAR(36) NOT NULL DEFAULT '',
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  last_updated_by VARCHAR(36) NOT NULL DEFAULT '',
  last_updated_by_name VARCHAR(100) NOT NULL DEFAULT '',
  last_updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uq_t_eval_tag_option_name UNIQUE (tag_id, option_name),
  CONSTRAINT uq_t_eval_tag_option_order UNIQUE (tag_id, display_order),
  CONSTRAINT ck_t_eval_tag_option_group CHECK (option_group IN ('pass', 'fail')),
  CONSTRAINT ck_t_eval_tag_option_order CHECK (display_order > 0)
);

COMMENT ON TABLE t_eval_tag_option IS '评测标签选项表';
COMMENT ON COLUMN t_eval_tag_option.id IS '标签选项ID';
COMMENT ON COLUMN t_eval_tag_option.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_tag_option.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_tag_option.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_tag_option.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_tag_option.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_tag_option.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_tag_option.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_tag_option.tag_id IS '标签ID';
COMMENT ON COLUMN t_eval_tag_option.option_name IS '选项名称：分类选项名或布尔值True/False';
COMMENT ON COLUMN t_eval_tag_option.option_group IS '选项分组：pass通过，fail失败';
COMMENT ON COLUMN t_eval_tag_option.display_order IS '展示顺序';
