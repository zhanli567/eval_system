CREATE TABLE IF NOT EXISTS t_eval_dataset (
  id VARCHAR(36),
  space_id VARCHAR(36),
  name VARCHAR(50),
  description VARCHAR(200),
  published_version_count INT,
  latest_published_version_id VARCHAR(64),
  is_deleted SMALLINT,
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_dataset IS '评测集主表';
COMMENT ON COLUMN t_eval_dataset.id IS '评测集ID';
COMMENT ON COLUMN t_eval_dataset.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_dataset.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_dataset.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_dataset.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_dataset.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_dataset.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_dataset.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_dataset.name IS '名称';
COMMENT ON COLUMN t_eval_dataset.description IS '描述';
COMMENT ON COLUMN t_eval_dataset.published_version_count IS '已发布版本数';
COMMENT ON COLUMN t_eval_dataset.latest_published_version_id IS '最新发布版本ID';
COMMENT ON COLUMN t_eval_dataset.is_deleted IS '是否删除：0否，1是';

CREATE TABLE IF NOT EXISTS t_eval_dataset_version (
  id VARCHAR(36),
  space_id VARCHAR(36),
  dataset_id VARCHAR(64),
  version_no INT,
  item_count INT,
  is_deleted SMALLINT,
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_dataset_version IS '评测集版本表';
COMMENT ON COLUMN t_eval_dataset_version.id IS '版本ID';
COMMENT ON COLUMN t_eval_dataset_version.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_dataset_version.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_dataset_version.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_dataset_version.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_dataset_version.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_dataset_version.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_dataset_version.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_dataset_version.dataset_id IS '评测集ID';
COMMENT ON COLUMN t_eval_dataset_version.version_no IS '版本号：0草稿，>0发布版本';
COMMENT ON COLUMN t_eval_dataset_version.item_count IS '数据行数';
COMMENT ON COLUMN t_eval_dataset_version.is_deleted IS '是否删除：0否，1是';

CREATE TABLE IF NOT EXISTS t_eval_dataset_field (
  id VARCHAR(36),
  space_id VARCHAR(36),
  version_id VARCHAR(64),
  field_name VARCHAR(64),
  field_type VARCHAR(32),
  is_required SMALLINT,
  description VARCHAR(200),
  display_order INT,
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_dataset_field IS '评测集字段表';
COMMENT ON COLUMN t_eval_dataset_field.id IS '字段ID';
COMMENT ON COLUMN t_eval_dataset_field.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_dataset_field.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_dataset_field.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_dataset_field.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_dataset_field.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_dataset_field.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_dataset_field.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_dataset_field.version_id IS '版本ID';
COMMENT ON COLUMN t_eval_dataset_field.field_name IS '列名';
COMMENT ON COLUMN t_eval_dataset_field.field_type IS '列类型：string/number/boolean';
COMMENT ON COLUMN t_eval_dataset_field.is_required IS '是否必填：0否，1是';
COMMENT ON COLUMN t_eval_dataset_field.description IS '字段描述';
COMMENT ON COLUMN t_eval_dataset_field.display_order IS '展示顺序';

CREATE TABLE IF NOT EXISTS t_eval_dataset_item (
  id VARCHAR(36),
  space_id VARCHAR(36),
  version_id VARCHAR(64),
  row_no INT,
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_dataset_item IS '评测集数据行表';
COMMENT ON COLUMN t_eval_dataset_item.id IS '数据行ID';
COMMENT ON COLUMN t_eval_dataset_item.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_dataset_item.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_dataset_item.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_dataset_item.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_dataset_item.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_dataset_item.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_dataset_item.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_dataset_item.version_id IS '版本ID';
COMMENT ON COLUMN t_eval_dataset_item.row_no IS '行号';

CREATE TABLE IF NOT EXISTS t_eval_dataset_item_cell (
  id VARCHAR(36),
  space_id VARCHAR(36),
  version_id VARCHAR(64),
  item_id VARCHAR(64),
  field_id VARCHAR(64),
  cell_value TEXT,
  created_by_name VARCHAR(100),
  created_by VARCHAR(36),
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_updated_by VARCHAR(36),
  last_updated_by_name VARCHAR(100),
  last_updated_date TIMESTAMP
);

COMMENT ON TABLE t_eval_dataset_item_cell IS '评测集数据单元格表';
COMMENT ON COLUMN t_eval_dataset_item_cell.id IS '单元格ID';
COMMENT ON COLUMN t_eval_dataset_item_cell.space_id IS '空间ID';
COMMENT ON COLUMN t_eval_dataset_item_cell.created_by_name IS '创建人名称';
COMMENT ON COLUMN t_eval_dataset_item_cell.created_by IS '创建人ID';
COMMENT ON COLUMN t_eval_dataset_item_cell.created_date IS '创建时间';
COMMENT ON COLUMN t_eval_dataset_item_cell.last_updated_by IS '最后更新人ID';
COMMENT ON COLUMN t_eval_dataset_item_cell.last_updated_by_name IS '最后更新人名称';
COMMENT ON COLUMN t_eval_dataset_item_cell.last_updated_date IS '最后更新时间';
COMMENT ON COLUMN t_eval_dataset_item_cell.version_id IS '版本ID';
COMMENT ON COLUMN t_eval_dataset_item_cell.item_id IS '数据行ID';
COMMENT ON COLUMN t_eval_dataset_item_cell.field_id IS '字段ID';
COMMENT ON COLUMN t_eval_dataset_item_cell.cell_value IS '单元格值';
