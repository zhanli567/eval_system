CREATE TABLE IF NOT EXISTS eval_tag (
  id VARCHAR(64) COMMENT '标签ID',
  tag_name VARCHAR(50) COMMENT '标签名称',
  tag_type VARCHAR(32) COMMENT '标签类型：category分类，boolean布尔值，number数字，text文本',
  description VARCHAR(200) COMMENT '标签描述',
  min_value INT COMMENT '数字类型评分范围最小值',
  max_value INT COMMENT '数字类型评分范围最大值',
  pass_threshold INT COMMENT '数字类型通过阈值：大于等于该值为通过',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测标签表';

CREATE TABLE IF NOT EXISTS eval_tag_option (
  id VARCHAR(64) COMMENT '标签选项ID',
  tag_id VARCHAR(64) COMMENT '标签ID',
  option_name VARCHAR(50) COMMENT '选项名称：分类选项名或布尔值True/False',
  option_group VARCHAR(16) COMMENT '选项分组：pass通过，fail失败',
  display_order INT COMMENT '展示顺序',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测标签选项表';
