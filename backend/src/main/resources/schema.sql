CREATE TABLE IF NOT EXISTS eval_dataset (
  id VARCHAR(64) COMMENT '评测集ID',
  name VARCHAR(50) COMMENT '名称',
  description VARCHAR(200) COMMENT '描述',
  published_version_count INT COMMENT '已发布版本数',
  latest_published_version_id VARCHAR(64) COMMENT '最新发布版本ID',
  is_deleted TINYINT COMMENT '是否删除：0否，1是',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测集主表';

CREATE TABLE IF NOT EXISTS eval_dataset_version (
  id VARCHAR(64) COMMENT '版本ID',
  dataset_id VARCHAR(64) COMMENT '评测集ID',
  version_no INT COMMENT '版本号：0草稿，>0发布版本',
  item_count INT COMMENT '数据行数',
  is_deleted TINYINT COMMENT '是否删除：0否，1是',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测集版本表';

CREATE TABLE IF NOT EXISTS eval_dataset_field (
  id VARCHAR(64) COMMENT '字段ID',
  version_id VARCHAR(64) COMMENT '版本ID',
  field_name VARCHAR(64) COMMENT '列名',
  field_type VARCHAR(32) COMMENT '列类型：string/number/boolean',
  is_required TINYINT COMMENT '是否必填：0否，1是',
  description VARCHAR(200) COMMENT '字段描述',
  display_order INT COMMENT '展示顺序',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测集字段表';

CREATE TABLE IF NOT EXISTS eval_dataset_item (
  id VARCHAR(64) COMMENT '数据行ID',
  version_id VARCHAR(64) COMMENT '版本ID',
  row_no INT COMMENT '行号',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测集数据行表';

CREATE TABLE IF NOT EXISTS eval_dataset_item_cell (
  id VARCHAR(64) COMMENT '单元格ID',
  version_id VARCHAR(64) COMMENT '版本ID',
  item_id VARCHAR(64) COMMENT '数据行ID',
  field_id VARCHAR(64) COMMENT '字段ID',
  cell_value LONGTEXT COMMENT '单元格值',
  created_at VARCHAR(32) COMMENT '创建时间',
  updated_at VARCHAR(32) COMMENT '更新时间'
) COMMENT='评测集数据单元格表';

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
