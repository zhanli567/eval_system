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
