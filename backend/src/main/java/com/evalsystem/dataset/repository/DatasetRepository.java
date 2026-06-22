package com.evalsystem.dataset.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.evalsystem.dataset.dto.DatasetSummary;
import com.evalsystem.dataset.dto.DatasetVersionDto;
import com.evalsystem.dataset.dto.FieldDto;
import com.evalsystem.dataset.mapper.DatasetFieldMapper;
import com.evalsystem.dataset.mapper.DatasetItemCellMapper;
import com.evalsystem.dataset.mapper.DatasetItemMapper;
import com.evalsystem.dataset.mapper.DatasetMapper;
import com.evalsystem.dataset.mapper.DatasetVersionMapper;
import com.evalsystem.dataset.pojo.EvalDataset;
import com.evalsystem.dataset.pojo.EvalDatasetField;
import com.evalsystem.dataset.pojo.EvalDatasetItem;
import com.evalsystem.dataset.pojo.EvalDatasetItemCell;
import com.evalsystem.dataset.pojo.EvalDatasetVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class DatasetRepository {
  private final DatasetMapper datasetMapper;
  private final DatasetVersionMapper versionMapper;
  private final DatasetFieldMapper fieldMapper;
  private final DatasetItemMapper itemMapper;
  private final DatasetItemCellMapper cellMapper;

  public DatasetRepository(
      DatasetMapper datasetMapper,
      DatasetVersionMapper versionMapper,
      DatasetFieldMapper fieldMapper,
      DatasetItemMapper itemMapper,
      DatasetItemCellMapper cellMapper
  ) {
    this.datasetMapper = datasetMapper;
    this.versionMapper = versionMapper;
    this.fieldMapper = fieldMapper;
    this.itemMapper = itemMapper;
    this.cellMapper = cellMapper;
  }

  public List<DatasetSummary> listDatasetSummaries(String like, int size, int offset) {
    return datasetMapper.listDatasetSummaries(like, size, offset);
  }

  public long countDatasetSummaries(String like) {
    return datasetMapper.selectCount(new LambdaQueryWrapper<EvalDataset>()
        .eq(EvalDataset::getIsDeleted, 0)
        .like(hasLikeText(like), EvalDataset::getName, likeText(like)));
  }

  public void insertDataset(String datasetId, String name, String description, String now) {
    EvalDataset dataset = new EvalDataset();
    dataset.setId(datasetId);
    dataset.setName(name);
    dataset.setDescription(description);
    dataset.setPublishedVersionCount(0);
    dataset.setLatestPublishedVersionId(null);
    dataset.setIsDeleted(0);
    dataset.setCreatedAt(now);
    dataset.setUpdatedAt(now);
    datasetMapper.insert(dataset);
  }

  public void insertVersion(String versionId, String datasetId, int versionNo, int itemCount, String now) {
    EvalDatasetVersion version = new EvalDatasetVersion();
    version.setId(versionId);
    version.setDatasetId(datasetId);
    version.setVersionNo(versionNo);
    version.setItemCount(itemCount);
    version.setIsDeleted(0);
    version.setCreatedAt(now);
    version.setUpdatedAt(now);
    versionMapper.insert(version);
  }

  public DatasetSummary findDatasetSummary(String datasetId) {
    return datasetMapper.findDatasetSummary(datasetId);
  }

  public void softDeleteDataset(String datasetId, String now) {
    datasetMapper.update(null, new LambdaUpdateWrapper<EvalDataset>()
        .eq(EvalDataset::getId, datasetId)
        .set(EvalDataset::getIsDeleted, 1)
        .set(EvalDataset::getUpdatedAt, now));
  }

  public List<DatasetVersionDto> listVersions(String datasetId) {
    return versionMapper.selectList(new LambdaQueryWrapper<EvalDatasetVersion>()
            .eq(EvalDatasetVersion::getDatasetId, datasetId)
            .eq(EvalDatasetVersion::getIsDeleted, 0)
            .orderByAsc(EvalDatasetVersion::getVersionNo))
        .stream()
        .map(this::toVersionDto)
        .toList();
  }

  public DatasetVersionDto findVersion(String versionId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getId, versionId)
        .eq(EvalDatasetVersion::getIsDeleted, 0)
        .last("LIMIT 1"));
    return version == null ? null : toVersionDto(version);
  }

  public String findDraftVersionId(String datasetId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .select(EvalDatasetVersion::getId)
        .eq(EvalDatasetVersion::getDatasetId, datasetId)
        .eq(EvalDatasetVersion::getVersionNo, 0)
        .eq(EvalDatasetVersion::getIsDeleted, 0)
        .last("LIMIT 1"));
    return version == null ? null : version.getId();
  }

  public List<FieldDto> listFields(String versionId) {
    return fieldMapper.selectList(new LambdaQueryWrapper<EvalDatasetField>()
            .eq(EvalDatasetField::getVersionId, versionId)
            .orderByAsc(EvalDatasetField::getDisplayOrder))
        .stream()
        .map(this::toFieldDto)
        .toList();
  }

  public List<String> listFieldIds(String versionId) {
    return fieldMapper.selectList(new LambdaQueryWrapper<EvalDatasetField>()
            .select(EvalDatasetField::getId)
            .eq(EvalDatasetField::getVersionId, versionId))
        .stream()
        .map(EvalDatasetField::getId)
        .toList();
  }

  public void updateField(
      String fieldId,
      String fieldName,
      String fieldType,
      int required,
      String description,
      int displayOrder,
      String now
  ) {
    fieldMapper.update(null, new LambdaUpdateWrapper<EvalDatasetField>()
        .eq(EvalDatasetField::getId, fieldId)
        .set(EvalDatasetField::getFieldName, fieldName)
        .set(EvalDatasetField::getFieldType, fieldType)
        .set(EvalDatasetField::getIsRequired, required)
        .set(EvalDatasetField::getDescription, description)
        .set(EvalDatasetField::getDisplayOrder, displayOrder)
        .set(EvalDatasetField::getUpdatedAt, now));
  }

  public void insertField(
      String fieldId,
      String versionId,
      String fieldName,
      String fieldType,
      int required,
      String description,
      int displayOrder,
      String now
  ) {
    EvalDatasetField field = new EvalDatasetField();
    field.setId(fieldId);
    field.setVersionId(versionId);
    field.setFieldName(fieldName);
    field.setFieldType(fieldType);
    field.setIsRequired(required);
    field.setDescription(description);
    field.setDisplayOrder(displayOrder);
    field.setCreatedAt(now);
    field.setUpdatedAt(now);
    fieldMapper.insert(field);
  }

  public void deleteFields(String versionId, List<String> fieldIds) {
    if (fieldIds == null || fieldIds.isEmpty()) {
      return;
    }
    cellMapper.delete(new LambdaQueryWrapper<EvalDatasetItemCell>()
        .eq(EvalDatasetItemCell::getVersionId, versionId)
        .in(EvalDatasetItemCell::getFieldId, fieldIds));
    fieldMapper.delete(new LambdaQueryWrapper<EvalDatasetField>()
        .eq(EvalDatasetField::getVersionId, versionId)
        .in(EvalDatasetField::getId, fieldIds));
  }

  public List<DatasetRowRecord> searchRows(String versionId, String fieldId, String like, int size, int offset) {
    return datasetMapper.searchRows(versionId, fieldId, like, size, offset);
  }

  public long countSearchRows(String versionId, String fieldId, String like) {
    return datasetMapper.countSearchRows(versionId, fieldId, like);
  }

  public List<DatasetRowRecord> listRows(String versionId, int size, int offset) {
    return itemMapper.selectList(new LambdaQueryWrapper<EvalDatasetItem>()
            .eq(EvalDatasetItem::getVersionId, versionId)
            .orderByAsc(EvalDatasetItem::getRowNo)
            .last("LIMIT " + size + " OFFSET " + offset))
        .stream()
        .map(this::toRowRecord)
        .toList();
  }

  public List<DatasetRowRecord> listAllRows(String versionId) {
    return itemMapper.selectList(new LambdaQueryWrapper<EvalDatasetItem>()
            .eq(EvalDatasetItem::getVersionId, versionId)
            .orderByAsc(EvalDatasetItem::getRowNo))
        .stream()
        .map(this::toRowRecord)
        .toList();
  }

  public long countRows(String versionId) {
    return itemMapper.selectCount(new LambdaQueryWrapper<EvalDatasetItem>()
        .eq(EvalDatasetItem::getVersionId, versionId));
  }

  public Map<String, Map<String, String>> loadValues(List<String> itemIds) {
    if (itemIds == null || itemIds.isEmpty()) {
      return Map.of();
    }
    Map<String, Map<String, String>> values = new LinkedHashMap<>();
    for (EvalDatasetItemCell cell : cellMapper.selectList(new LambdaQueryWrapper<EvalDatasetItemCell>()
        .in(EvalDatasetItemCell::getItemId, itemIds))) {
      values.computeIfAbsent(cell.getItemId(), ignored -> new LinkedHashMap<>())
          .put(cell.getFieldId(), cell.getCellValue());
    }
    return values;
  }

  public int nextRowNo(String versionId) {
    EvalDatasetItem item = itemMapper.selectOne(new QueryWrapper<EvalDatasetItem>()
        .select("COALESCE(MAX(row_no), 0) + 1 AS row_no")
        .eq("version_id", versionId));
    return item == null || item.getRowNo() == null ? 1 : item.getRowNo();
  }

  public void insertItem(String itemId, String versionId, int rowNo, String now) {
    EvalDatasetItem item = new EvalDatasetItem();
    item.setId(itemId);
    item.setVersionId(versionId);
    item.setRowNo(rowNo);
    item.setCreatedAt(now);
    item.setUpdatedAt(now);
    itemMapper.insert(item);
  }

  public void insertCell(String cellId, String versionId, String itemId, String fieldId, String cellValue, String now) {
    EvalDatasetItemCell cell = new EvalDatasetItemCell();
    cell.setId(cellId);
    cell.setVersionId(versionId);
    cell.setItemId(itemId);
    cell.setFieldId(fieldId);
    cell.setCellValue(cellValue);
    cell.setCreatedAt(now);
    cell.setUpdatedAt(now);
    cellMapper.insert(cell);
  }

  public void updateItem(String itemId, String versionId, String now) {
    itemMapper.update(null, new LambdaUpdateWrapper<EvalDatasetItem>()
        .eq(EvalDatasetItem::getId, itemId)
        .eq(EvalDatasetItem::getVersionId, versionId)
        .set(EvalDatasetItem::getUpdatedAt, now));
  }

  public void deleteCellsByItem(String itemId) {
    cellMapper.delete(new LambdaQueryWrapper<EvalDatasetItemCell>()
        .eq(EvalDatasetItemCell::getItemId, itemId));
  }

  public void deleteItem(String itemId, String versionId) {
    itemMapper.delete(new LambdaQueryWrapper<EvalDatasetItem>()
        .eq(EvalDatasetItem::getId, itemId)
        .eq(EvalDatasetItem::getVersionId, versionId));
  }

  public DatasetRowRecord findRow(String itemId) {
    EvalDatasetItem item = itemMapper.selectOne(new LambdaQueryWrapper<EvalDatasetItem>()
        .eq(EvalDatasetItem::getId, itemId)
        .last("LIMIT 1"));
    return item == null ? null : toRowRecord(item);
  }

  public int nextVersionNo(String datasetId) {
    EvalDatasetVersion version = versionMapper.selectOne(new QueryWrapper<EvalDatasetVersion>()
        .select("COALESCE(MAX(version_no), 0) + 1 AS version_no")
        .eq("dataset_id", datasetId)
        .eq("is_deleted", 0));
    return version == null || version.getVersionNo() == null ? 1 : version.getVersionNo();
  }

  public int findVersionItemCount(String versionId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .select(EvalDatasetVersion::getItemCount)
        .eq(EvalDatasetVersion::getId, versionId)
        .last("LIMIT 1"));
    return version == null || version.getItemCount() == null ? 0 : version.getItemCount();
  }

  public void softDeleteVersion(String versionId, String now) {
    versionMapper.update(null, new LambdaUpdateWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getId, versionId)
        .set(EvalDatasetVersion::getIsDeleted, 1)
        .set(EvalDatasetVersion::getUpdatedAt, now));
  }

  public void clearVersionCells(String versionId) {
    cellMapper.delete(new LambdaQueryWrapper<EvalDatasetItemCell>()
        .eq(EvalDatasetItemCell::getVersionId, versionId));
  }

  public void clearVersionItems(String versionId) {
    itemMapper.delete(new LambdaQueryWrapper<EvalDatasetItem>()
        .eq(EvalDatasetItem::getVersionId, versionId));
  }

  public void clearVersionFields(String versionId) {
    fieldMapper.delete(new LambdaQueryWrapper<EvalDatasetField>()
        .eq(EvalDatasetField::getVersionId, versionId));
  }

  public void clearVersionContent(String versionId) {
    clearVersionCells(versionId);
    clearVersionItems(versionId);
    clearVersionFields(versionId);
  }

  public List<String> listItemIds(String versionId) {
    return itemMapper.selectList(new LambdaQueryWrapper<EvalDatasetItem>()
            .select(EvalDatasetItem::getId)
            .eq(EvalDatasetItem::getVersionId, versionId))
        .stream()
        .map(EvalDatasetItem::getId)
        .toList();
  }

  public Integer findVersionNo(String versionId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .select(EvalDatasetVersion::getVersionNo)
        .eq(EvalDatasetVersion::getId, versionId)
        .eq(EvalDatasetVersion::getIsDeleted, 0)
        .last("LIMIT 1"));
    return version == null ? null : version.getVersionNo();
  }

  public void updateVersionItemCount(String versionId, int itemCount, String now) {
    versionMapper.update(null, new LambdaUpdateWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getId, versionId)
        .set(EvalDatasetVersion::getItemCount, itemCount)
        .set(EvalDatasetVersion::getUpdatedAt, now));
  }

  public void touchVersionAndDataset(String versionId, String now) {
    versionMapper.update(null, new LambdaUpdateWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getId, versionId)
        .set(EvalDatasetVersion::getUpdatedAt, now));
    String datasetId = findDatasetIdByVersionId(versionId);
    if (StringUtils.hasText(datasetId)) {
      datasetMapper.update(null, new LambdaUpdateWrapper<EvalDataset>()
          .eq(EvalDataset::getId, datasetId)
          .set(EvalDataset::getUpdatedAt, now));
    }
  }

  public void refreshDatasetVersionStats(String datasetId, String now) {
    datasetMapper.update(null, new LambdaUpdateWrapper<EvalDataset>()
        .eq(EvalDataset::getId, datasetId)
        .set(EvalDataset::getPublishedVersionCount, countPublishedVersions(datasetId))
        .set(EvalDataset::getLatestPublishedVersionId, findLatestPublishedVersionId(datasetId))
        .set(EvalDataset::getUpdatedAt, now));
  }

  private int countPublishedVersions(String datasetId) {
    return Math.toIntExact(versionMapper.selectCount(new LambdaQueryWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getDatasetId, datasetId)
        .gt(EvalDatasetVersion::getVersionNo, 0)
        .eq(EvalDatasetVersion::getIsDeleted, 0)));
  }

  private String findLatestPublishedVersionId(String datasetId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .select(EvalDatasetVersion::getId)
        .eq(EvalDatasetVersion::getDatasetId, datasetId)
        .gt(EvalDatasetVersion::getVersionNo, 0)
        .eq(EvalDatasetVersion::getIsDeleted, 0)
        .orderByDesc(EvalDatasetVersion::getVersionNo)
        .last("LIMIT 1"));
    return version == null ? null : version.getId();
  }

  private String findDatasetIdByVersionId(String versionId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .select(EvalDatasetVersion::getDatasetId)
        .eq(EvalDatasetVersion::getId, versionId)
        .last("LIMIT 1"));
    return version == null ? null : version.getDatasetId();
  }

  private DatasetVersionDto toVersionDto(EvalDatasetVersion version) {
    int versionNo = version.getVersionNo() == null ? 0 : version.getVersionNo();
    return new DatasetVersionDto(
        version.getId(),
        version.getDatasetId(),
        versionNo,
        versionNo == 0 ? "\u8349\u7a3f" : "V" + versionNo,
        version.getItemCount(),
        versionNo == 0,
        version.getCreatedAt(),
        version.getUpdatedAt());
  }

  private FieldDto toFieldDto(EvalDatasetField field) {
    return new FieldDto(
        field.getId(),
        field.getVersionId(),
        field.getFieldName(),
        field.getFieldType(),
        field.getIsRequired() != null && field.getIsRequired() != 0,
        field.getDescription(),
        field.getDisplayOrder());
  }

  private DatasetRowRecord toRowRecord(EvalDatasetItem item) {
    return new DatasetRowRecord(item.getId(), item.getRowNo(), item.getCreatedAt(), item.getUpdatedAt());
  }

  private boolean hasLikeText(String like) {
    return StringUtils.hasText(like) && !"%%".equals(like);
  }

  private String likeText(String like) {
    return hasLikeText(like) ? like.substring(1, like.length() - 1) : "";
  }
}
