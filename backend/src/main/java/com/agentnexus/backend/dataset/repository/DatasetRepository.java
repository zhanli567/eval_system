package com.agentnexus.backend.dataset.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.agentnexus.backend.common.context.CurrentSpaceHolder;
import com.agentnexus.backend.common.context.CurrentUserHolder;
import com.agentnexus.backend.common.security.CurrentUser;
import com.agentnexus.backend.dataset.api.dto.response.DatasetSummary;
import com.agentnexus.backend.dataset.api.dto.response.DatasetVersionDto;
import com.agentnexus.backend.dataset.api.dto.response.FieldDto;
import com.agentnexus.backend.dataset.mapper.DatasetFieldMapper;
import com.agentnexus.backend.dataset.mapper.DatasetItemCellMapper;
import com.agentnexus.backend.dataset.mapper.DatasetItemMapper;
import com.agentnexus.backend.dataset.mapper.DatasetMapper;
import com.agentnexus.backend.dataset.mapper.DatasetVersionMapper;
import com.agentnexus.backend.dataset.entity.EvalDataset;
import com.agentnexus.backend.dataset.entity.EvalDatasetField;
import com.agentnexus.backend.dataset.entity.EvalDatasetItem;
import com.agentnexus.backend.dataset.entity.EvalDatasetItemCell;
import com.agentnexus.backend.dataset.entity.EvalDatasetVersion;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  public List<DatasetSummary> listDatasetSummaries(String like, String orderColumn, String orderDirection, int size, int offset) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        datasetMapper.listDatasetSummaries(currentSpaceId(), like, orderColumn, orderDirection, size, offset));
  }

  public long countDatasetSummaries(String like) {
    return datasetMapper.selectCount(new LambdaQueryWrapper<EvalDataset>()
        .eq(EvalDataset::getSpaceId, currentSpaceId())
        .like(hasLikeText(like), EvalDataset::getName, likeText(like)));
  }

  public boolean existsDatasetName(String name) {
    return datasetMapper.selectCount(new LambdaQueryWrapper<EvalDataset>()
        .eq(EvalDataset::getSpaceId, currentSpaceId())
        .eq(EvalDataset::getName, name)) > 0;
  }

  public void insertDataset(String datasetId, String name, String description, String now) {
    EvalDataset dataset = new EvalDataset();
    dataset.setId(datasetId);
    dataset.setName(name);
    dataset.setDescription(description);
    dataset.setPublishedVersionCount(0);
    dataset.setLatestPublishedVersionId(null);
    dataset.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(dataset);
    datasetMapper.insert(dataset);
  }

  public void insertVersion(String versionId, String datasetId, int versionNo, int itemCount, String now) {
    EvalDatasetVersion version = new EvalDatasetVersion();
    version.setId(versionId);
    version.setDatasetId(datasetId);
    version.setVersionNo(versionNo);
    version.setItemCount(itemCount);
    version.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(version);
    versionMapper.insert(version);
  }

  public DatasetSummary findDatasetSummary(String datasetId) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        datasetMapper.findDatasetSummary(currentSpaceId(), datasetId));
  }

  public void deleteDataset(String datasetId) {
    List<String> versionIds = versionMapper.selectList(new LambdaQueryWrapper<EvalDatasetVersion>()
            .select(EvalDatasetVersion::getId)
            .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
            .eq(EvalDatasetVersion::getDatasetId, datasetId))
        .stream()
        .map(EvalDatasetVersion::getId)
        .toList();
    versionIds.forEach(this::clearVersionContent);
    versionMapper.delete(new LambdaQueryWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
        .eq(EvalDatasetVersion::getDatasetId, datasetId));
    datasetMapper.delete(new LambdaQueryWrapper<EvalDataset>()
        .eq(EvalDataset::getSpaceId, currentSpaceId())
        .eq(EvalDataset::getId, datasetId));
  }

  public List<DatasetVersionDto> listVersions(String datasetId) {
    return versionMapper.selectList(new LambdaQueryWrapper<EvalDatasetVersion>()
            .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
            .eq(EvalDatasetVersion::getDatasetId, datasetId)
            .orderByAsc(EvalDatasetVersion::getVersionNo))
        .stream()
        .map(this::toVersionDto)
        .toList();
  }

  public DatasetVersionDto findVersion(String versionId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
        .eq(EvalDatasetVersion::getId, versionId)
        .last("LIMIT 1"));
    return version == null ? null : toVersionDto(version);
  }

  public String findDraftVersionId(String datasetId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .select(EvalDatasetVersion::getId)
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
        .eq(EvalDatasetVersion::getDatasetId, datasetId)
        .eq(EvalDatasetVersion::getVersionNo, 0)
        .last("LIMIT 1"));
    return version == null ? null : version.getId();
  }

  public List<FieldDto> listFields(String versionId) {
    return fieldMapper.selectList(new LambdaQueryWrapper<EvalDatasetField>()
            .eq(EvalDatasetField::getSpaceId, currentSpaceId())
            .eq(EvalDatasetField::getVersionId, versionId)
            .orderByAsc(EvalDatasetField::getDisplayOrder))
        .stream()
        .map(this::toFieldDto)
        .toList();
  }

  public List<String> listFieldIds(String versionId) {
    return fieldMapper.selectList(new LambdaQueryWrapper<EvalDatasetField>()
            .select(EvalDatasetField::getId)
            .eq(EvalDatasetField::getSpaceId, currentSpaceId())
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
        .eq(EvalDatasetField::getSpaceId, currentSpaceId())
        .eq(EvalDatasetField::getId, fieldId)
        .set(EvalDatasetField::getFieldName, fieldName)
        .set(EvalDatasetField::getFieldType, fieldType)
        .set(EvalDatasetField::getIsRequired, required)
        .set(EvalDatasetField::getDescription, description)
        .set(EvalDatasetField::getDisplayOrder, displayOrder)
        .set(EvalDatasetField::getLastUpdatedBy, currentUserId())
        .set(EvalDatasetField::getLastUpdatedByName, currentUserName())
        .set(EvalDatasetField::getLastUpdatedDate, toLastUpdatedDate(now)));
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
    field.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(field);
    fieldMapper.insert(field);
  }

  public void deleteFields(String versionId, List<String> fieldIds) {
    if (fieldIds == null || fieldIds.isEmpty()) {
      return;
    }
    cellMapper.delete(new LambdaQueryWrapper<EvalDatasetItemCell>()
        .eq(EvalDatasetItemCell::getSpaceId, currentSpaceId())
        .eq(EvalDatasetItemCell::getVersionId, versionId)
        .in(EvalDatasetItemCell::getFieldId, fieldIds));
    fieldMapper.delete(new LambdaQueryWrapper<EvalDatasetField>()
        .eq(EvalDatasetField::getSpaceId, currentSpaceId())
        .eq(EvalDatasetField::getVersionId, versionId)
        .in(EvalDatasetField::getId, fieldIds));
  }

  public List<DatasetRowRecord> searchRows(String versionId, String fieldId, String like, int size, int offset) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        datasetMapper.searchRows(currentSpaceId(), versionId, fieldId, like, size, offset));
  }

  public long countSearchRows(String versionId, String fieldId, String like) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        datasetMapper.countSearchRows(currentSpaceId(), versionId, fieldId, like));
  }

  public List<DatasetRowRecord> listRows(String versionId, int size, int offset) {
    return itemMapper.selectList(new LambdaQueryWrapper<EvalDatasetItem>()
            .eq(EvalDatasetItem::getSpaceId, currentSpaceId())
            .eq(EvalDatasetItem::getVersionId, versionId)
            .orderByAsc(EvalDatasetItem::getRowNo)
            .last("LIMIT " + size + " OFFSET " + offset))
        .stream()
        .map(this::toRowRecord)
        .toList();
  }

  public List<DatasetRowRecord> listAllRows(String versionId) {
    return itemMapper.selectList(new LambdaQueryWrapper<EvalDatasetItem>()
            .eq(EvalDatasetItem::getSpaceId, currentSpaceId())
            .eq(EvalDatasetItem::getVersionId, versionId)
            .orderByAsc(EvalDatasetItem::getRowNo))
        .stream()
        .map(this::toRowRecord)
        .toList();
  }

  public long countRows(String versionId) {
    return itemMapper.selectCount(new LambdaQueryWrapper<EvalDatasetItem>()
        .eq(EvalDatasetItem::getSpaceId, currentSpaceId())
        .eq(EvalDatasetItem::getVersionId, versionId));
  }

  public Map<String, Map<String, String>> loadValues(List<String> itemIds) {
    if (itemIds == null || itemIds.isEmpty()) {
      return Map.of();
    }
    Map<String, Map<String, String>> values = new LinkedHashMap<>();
    for (EvalDatasetItemCell cell : cellMapper.selectList(new LambdaQueryWrapper<EvalDatasetItemCell>()
        .eq(EvalDatasetItemCell::getSpaceId, currentSpaceId())
        .in(EvalDatasetItemCell::getItemId, itemIds))) {
      values.computeIfAbsent(cell.getItemId(), ignored -> new LinkedHashMap<>())
          .put(cell.getFieldId(), cell.getCellValue());
    }
    return values;
  }

  public int nextRowNo(String versionId) {
    EvalDatasetItem item = itemMapper.selectOne(new QueryWrapper<EvalDatasetItem>()
        .select("COALESCE(MAX(row_no), 0) + 1 AS row_no")
        .eq("space_id", currentSpaceId())
        .eq("version_id", versionId));
    return item == null || item.getRowNo() == null ? 1 : item.getRowNo();
  }

  public void insertItem(String itemId, String versionId, int rowNo, String now) {
    EvalDatasetItem item = new EvalDatasetItem();
    item.setId(itemId);
    item.setVersionId(versionId);
    item.setRowNo(rowNo);
    item.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(item);
    itemMapper.insert(item);
  }

  public void insertCell(String cellId, String versionId, String itemId, String fieldId, String cellValue, String now) {
    EvalDatasetItemCell cell = new EvalDatasetItemCell();
    cell.setId(cellId);
    cell.setVersionId(versionId);
    cell.setItemId(itemId);
    cell.setFieldId(fieldId);
    cell.setCellValue(cellValue);
    cell.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(cell);
    cellMapper.insert(cell);
  }

  public void updateItem(String itemId, String versionId, String now) {
    itemMapper.update(null, new LambdaUpdateWrapper<EvalDatasetItem>()
        .eq(EvalDatasetItem::getSpaceId, currentSpaceId())
        .eq(EvalDatasetItem::getId, itemId)
        .eq(EvalDatasetItem::getVersionId, versionId)
        .set(EvalDatasetItem::getLastUpdatedBy, currentUserId())
        .set(EvalDatasetItem::getLastUpdatedByName, currentUserName())
        .set(EvalDatasetItem::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void deleteCellsByItem(String itemId) {
    cellMapper.delete(new LambdaQueryWrapper<EvalDatasetItemCell>()
        .eq(EvalDatasetItemCell::getSpaceId, currentSpaceId())
        .eq(EvalDatasetItemCell::getItemId, itemId));
  }

  public void deleteItem(String itemId, String versionId) {
    itemMapper.delete(new LambdaQueryWrapper<EvalDatasetItem>()
        .eq(EvalDatasetItem::getSpaceId, currentSpaceId())
        .eq(EvalDatasetItem::getId, itemId)
        .eq(EvalDatasetItem::getVersionId, versionId));
  }

  public DatasetRowRecord findRow(String itemId) {
    EvalDatasetItem item = itemMapper.selectOne(new LambdaQueryWrapper<EvalDatasetItem>()
        .eq(EvalDatasetItem::getSpaceId, currentSpaceId())
        .eq(EvalDatasetItem::getId, itemId)
        .last("LIMIT 1"));
    return item == null ? null : toRowRecord(item);
  }

  public int nextVersionNo(String datasetId) {
    EvalDatasetVersion version = versionMapper.selectOne(new QueryWrapper<EvalDatasetVersion>()
        .select("COALESCE(MAX(version_no), 0) + 1 AS version_no")
        .eq("space_id", currentSpaceId())
        .eq("dataset_id", datasetId));
    return version == null || version.getVersionNo() == null ? 1 : version.getVersionNo();
  }

  public int findVersionItemCount(String versionId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .select(EvalDatasetVersion::getItemCount)
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
        .eq(EvalDatasetVersion::getId, versionId)
        .last("LIMIT 1"));
    return version == null || version.getItemCount() == null ? 0 : version.getItemCount();
  }

  public void deleteVersion(String versionId) {
    clearVersionContent(versionId);
    versionMapper.delete(new LambdaQueryWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
        .eq(EvalDatasetVersion::getId, versionId));
  }

  public void clearVersionCells(String versionId) {
    cellMapper.delete(new LambdaQueryWrapper<EvalDatasetItemCell>()
        .eq(EvalDatasetItemCell::getSpaceId, currentSpaceId())
        .eq(EvalDatasetItemCell::getVersionId, versionId));
  }

  public void clearVersionItems(String versionId) {
    itemMapper.delete(new LambdaQueryWrapper<EvalDatasetItem>()
        .eq(EvalDatasetItem::getSpaceId, currentSpaceId())
        .eq(EvalDatasetItem::getVersionId, versionId));
  }

  public void clearVersionFields(String versionId) {
    fieldMapper.delete(new LambdaQueryWrapper<EvalDatasetField>()
        .eq(EvalDatasetField::getSpaceId, currentSpaceId())
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
            .eq(EvalDatasetItem::getSpaceId, currentSpaceId())
            .eq(EvalDatasetItem::getVersionId, versionId))
        .stream()
        .map(EvalDatasetItem::getId)
        .toList();
  }

  public Integer findVersionNo(String versionId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .select(EvalDatasetVersion::getVersionNo)
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
        .eq(EvalDatasetVersion::getId, versionId)
        .last("LIMIT 1"));
    return version == null ? null : version.getVersionNo();
  }

  public void updateVersionItemCount(String versionId, int itemCount, String now) {
    versionMapper.update(null, new LambdaUpdateWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
        .eq(EvalDatasetVersion::getId, versionId)
        .set(EvalDatasetVersion::getItemCount, itemCount)
        .set(EvalDatasetVersion::getLastUpdatedBy, currentUserId())
        .set(EvalDatasetVersion::getLastUpdatedByName, currentUserName())
        .set(EvalDatasetVersion::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void touchVersionAndDataset(String versionId, String now) {
    versionMapper.update(null, new LambdaUpdateWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
        .eq(EvalDatasetVersion::getId, versionId)
        .set(EvalDatasetVersion::getLastUpdatedBy, currentUserId())
        .set(EvalDatasetVersion::getLastUpdatedByName, currentUserName())
        .set(EvalDatasetVersion::getLastUpdatedDate, toLastUpdatedDate(now)));
    String datasetId = findDatasetIdByVersionId(versionId);
    if (StringUtils.hasText(datasetId)) {
      datasetMapper.update(null, new LambdaUpdateWrapper<EvalDataset>()
          .eq(EvalDataset::getSpaceId, currentSpaceId())
          .eq(EvalDataset::getId, datasetId)
          .set(EvalDataset::getLastUpdatedBy, currentUserId())
          .set(EvalDataset::getLastUpdatedByName, currentUserName())
          .set(EvalDataset::getLastUpdatedDate, toLastUpdatedDate(now)));
    }
  }

  public void refreshDatasetVersionStats(String datasetId, String now) {
    datasetMapper.update(null, new LambdaUpdateWrapper<EvalDataset>()
        .eq(EvalDataset::getSpaceId, currentSpaceId())
        .eq(EvalDataset::getId, datasetId)
        .set(EvalDataset::getPublishedVersionCount, countPublishedVersions(datasetId))
        .set(EvalDataset::getLatestPublishedVersionId, findLatestPublishedVersionId(datasetId))
        .set(EvalDataset::getLastUpdatedBy, currentUserId())
        .set(EvalDataset::getLastUpdatedByName, currentUserName())
        .set(EvalDataset::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  private int countPublishedVersions(String datasetId) {
    return Math.toIntExact(versionMapper.selectCount(new LambdaQueryWrapper<EvalDatasetVersion>()
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
        .eq(EvalDatasetVersion::getDatasetId, datasetId)
        .gt(EvalDatasetVersion::getVersionNo, 0)));
  }

  private String findLatestPublishedVersionId(String datasetId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .select(EvalDatasetVersion::getId)
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
        .eq(EvalDatasetVersion::getDatasetId, datasetId)
        .gt(EvalDatasetVersion::getVersionNo, 0)
        .orderByDesc(EvalDatasetVersion::getVersionNo)
        .last("LIMIT 1"));
    return version == null ? null : version.getId();
  }

  private String findDatasetIdByVersionId(String versionId) {
    EvalDatasetVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalDatasetVersion>()
        .select(EvalDatasetVersion::getDatasetId)
        .eq(EvalDatasetVersion::getSpaceId, currentSpaceId())
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
        version.getCreatedByName(),
        version.getCreatedDate(),
        version.getLastUpdatedDate());
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
    return new DatasetRowRecord(item.getId(), item.getRowNo(), item.getCreatedDate(), item.getLastUpdatedDate());
  }

  private void fillCreated(EvalDataset dataset) {
    dataset.setSpaceId(currentSpaceId());
    dataset.setCreatedBy(currentUserId());
    dataset.setCreatedByName(currentUserName());
    dataset.setLastUpdatedBy(currentUserId());
    dataset.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalDatasetVersion version) {
    version.setSpaceId(currentSpaceId());
    version.setCreatedBy(currentUserId());
    version.setCreatedByName(currentUserName());
    version.setLastUpdatedBy(currentUserId());
    version.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalDatasetField field) {
    field.setSpaceId(currentSpaceId());
    field.setCreatedBy(currentUserId());
    field.setCreatedByName(currentUserName());
    field.setLastUpdatedBy(currentUserId());
    field.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalDatasetItem item) {
    item.setSpaceId(currentSpaceId());
    item.setCreatedBy(currentUserId());
    item.setCreatedByName(currentUserName());
    item.setLastUpdatedBy(currentUserId());
    item.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalDatasetItemCell cell) {
    cell.setSpaceId(currentSpaceId());
    cell.setCreatedBy(currentUserId());
    cell.setCreatedByName(currentUserName());
    cell.setLastUpdatedBy(currentUserId());
    cell.setLastUpdatedByName(currentUserName());
  }

  private String currentSpaceId() {
    return Objects.toString(CurrentSpaceHolder.get(), "");
  }

  private String currentUserId() {
    CurrentUser user = CurrentUserHolder.get();
    return user == null ? "" : Objects.toString(user.userId(), "");
  }

  private String currentUserName() {
    CurrentUser user = CurrentUserHolder.get();
    return user == null ? "" : Objects.toString(user.displayName(), "");
  }

  private LocalDateTime toLastUpdatedDate(String now) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(now)), ZoneId.systemDefault());
  }

  private boolean hasLikeText(String like) {
    return StringUtils.hasText(like) && !"%%".equals(like);
  }

  private String likeText(String like) {
    return hasLikeText(like) ? like.substring(1, like.length() - 1) : "";
  }
}
