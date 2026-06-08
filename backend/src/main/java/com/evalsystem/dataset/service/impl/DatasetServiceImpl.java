package com.evalsystem.dataset.service.impl;

import com.evalsystem.common.PageResponse;
import com.evalsystem.dataset.dto.BatchRowsRequest;
import com.evalsystem.dataset.dto.CreateDatasetRequest;
import com.evalsystem.dataset.dto.DatasetSummary;
import com.evalsystem.dataset.dto.DatasetVersionDto;
import com.evalsystem.dataset.dto.FieldDto;
import com.evalsystem.dataset.dto.FieldInput;
import com.evalsystem.dataset.dto.ImportRowsResult;
import com.evalsystem.dataset.dto.RowDto;
import com.evalsystem.dataset.dto.RowInput;
import com.evalsystem.dataset.dto.VersionDetail;
import com.evalsystem.dataset.mapper.DatasetMapper;
import com.evalsystem.dataset.service.DatasetService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DatasetServiceImpl implements DatasetService {
  private static final List<String> SUPPORTED_FIELD_TYPES = List.of("string", "number", "boolean");

  private final DatasetMapper datasetMapper;

  public DatasetServiceImpl(DatasetMapper datasetMapper) {
    this.datasetMapper = datasetMapper;
  }

  public PageResponse<DatasetSummary> listDatasets(int page, int size, String keyword) {
    int offset = Math.max(page - 1, 0) * size;
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    List<DatasetSummary> records = datasetMapper.listDatasetSummaries(like, size, offset);
    long total = datasetMapper.countDatasetSummaries(like);
    return new PageResponse<>(records, total, page, size);
  }

  @Transactional
  public DatasetSummary createDataset(CreateDatasetRequest request) {
    String datasetId = id();
    String draftVersionId = id();
    String now = now();
    datasetMapper.insertDataset(datasetId, request.name(), request.description(), now);
    datasetMapper.insertVersion(draftVersionId, datasetId, 0, 0, now);
    replaceFields(draftVersionId, request.fields() == null ? List.of() : request.fields());
    return getDatasetSummary(datasetId);
  }

  public DatasetSummary getDatasetSummary(String datasetId) {
    return datasetMapper.findDatasetSummary(datasetId);
  }

  public void deleteDataset(String datasetId) {
    datasetMapper.softDeleteDataset(datasetId, now());
  }

  public List<DatasetVersionDto> listVersions(String datasetId) {
    return datasetMapper.listVersions(datasetId);
  }

  public VersionDetail getVersionDetail(String versionId, int page, int size, String fieldId, String keyword) {
    DatasetVersionDto version = datasetMapper.findVersion(versionId);
    List<FieldDto> fields = listFields(versionId);
    PageResponse<RowDto> rows = listRows(versionId, page, size, fieldId, keyword);
    return new VersionDetail(version, fields, rows);
  }

  public List<FieldDto> listFields(String versionId) {
    return datasetMapper.listFields(versionId);
  }

  public PageResponse<RowDto> listRows(String versionId, int page, int size, String fieldId, String keyword) {
    int offset = Math.max(page - 1, 0) * size;
    boolean searching = StringUtils.hasText(fieldId) && StringUtils.hasText(keyword);
    List<DatasetMapper.RowRecord> rowRecords;
    long total;
    if (searching) {
      String like = "%" + keyword.trim() + "%";
      rowRecords = datasetMapper.searchRows(versionId, fieldId, like, size, offset);
      total = datasetMapper.countSearchRows(versionId, fieldId, like);
    } else {
      rowRecords = datasetMapper.listRows(versionId, size, offset);
      total = datasetMapper.countRows(versionId);
    }
    Map<String, Map<String, String>> values = datasetMapper.loadValues(rowRecords.stream().map(DatasetMapper.RowRecord::id).toList());
    List<RowDto> rows = rowRecords.stream()
        .map(row -> new RowDto(row.id(), row.rowNo(), values.getOrDefault(row.id(), Map.of()), row.createdAt(), row.updatedAt()))
        .toList();
    return new PageResponse<>(rows, total, page, size);
  }

  @Transactional
  public List<FieldDto> replaceFields(String versionId, List<FieldInput> fields) {
    ensureDraft(versionId);
    validateFields(fields);
    List<String> existingIds = datasetMapper.listFieldIds(versionId);
    List<String> keptIds = new ArrayList<>();
    int order = 1;
    String now = now();
    for (FieldInput field : fields) {
      String fieldId = StringUtils.hasText(field.id()) && existingIds.contains(field.id()) ? field.id() : id();
      keptIds.add(fieldId);
      if (existingIds.contains(fieldId)) {
        datasetMapper.updateField(fieldId, field.fieldName(), field.fieldType(), bool(field.required()), field.description(), order++, now);
      } else {
        datasetMapper.insertField(fieldId, versionId, field.fieldName(), field.fieldType(), bool(field.required()), field.description(), order++, now);
        addBlankCellsForNewField(versionId, fieldId);
      }
    }
    List<String> removedIds = existingIds.stream().filter(existing -> !keptIds.contains(existing)).toList();
    if (!removedIds.isEmpty()) {
      datasetMapper.deleteFields(versionId, removedIds);
    }
    touchVersion(versionId);
    return listFields(versionId);
  }

  @Transactional
  public RowDto addRow(String versionId, RowInput request) {
    ensureDraft(versionId);
    String itemId = id();
    String now = now();
    datasetMapper.insertItem(itemId, versionId, datasetMapper.nextRowNo(versionId), now);
    insertCells(versionId, itemId, request.values());
    updateItemCount(versionId);
    touchVersion(versionId);
    return getRow(itemId);
  }

  @Transactional
  public List<RowDto> addRows(String versionId, BatchRowsRequest request) {
    List<RowDto> rows = new ArrayList<>();
    if (request.rows() == null) {
      return rows;
    }
    for (Map<String, String> values : request.rows()) {
      rows.add(addRow(versionId, new RowInput(null, values)));
    }
    return rows;
  }

  @Transactional
  public ImportRowsResult importRows(String versionId, MultipartFile file) {
    ensureDraft(versionId);
    validateExcelFile(file);
    List<FieldDto> fields = listFields(versionId);
    if (fields.isEmpty()) {
      throw new IllegalArgumentException("请先维护表头");
    }

    List<Map<String, String>> rows = readExcelRows(file, fields);
    addRows(versionId, new BatchRowsRequest(rows));
    return new ImportRowsResult(rows.size());
  }

  @Transactional
  public ImportRowsResult coverRowsByExcel(String versionId, MultipartFile file) {
    ensureDraft(versionId);
    validateExcelFile(file);
    List<FieldDto> fields = listFields(versionId);
    if (fields.isEmpty()) {
      throw new IllegalArgumentException("请先维护表头");
    }

    List<Map<String, String>> rows = readExcelRows(file, fields);
    datasetMapper.clearVersionCells(versionId);
    datasetMapper.clearVersionItems(versionId);
    addRows(versionId, new BatchRowsRequest(rows));
    updateItemCount(versionId);
    touchVersion(versionId);
    return new ImportRowsResult(rows.size());
  }

  @Transactional
  public RowDto updateRow(String versionId, String itemId, RowInput request) {
    ensureDraft(versionId);
    datasetMapper.updateItem(itemId, versionId, now());
    datasetMapper.deleteCellsByItem(itemId);
    insertCells(versionId, itemId, request.values());
    touchVersion(versionId);
    return getRow(itemId);
  }

  @Transactional
  public void deleteRow(String versionId, String itemId) {
    ensureDraft(versionId);
    datasetMapper.deleteCellsByItem(itemId);
    datasetMapper.deleteItem(itemId, versionId);
    updateItemCount(versionId);
    touchVersion(versionId);
  }

  @Transactional
  public DatasetVersionDto publish(String datasetId) {
    String draftVersionId = datasetMapper.findDraftVersionId(datasetId);
    int nextVersionNo = datasetMapper.nextVersionNo(datasetId);
    String newVersionId = id();
    String now = now();
    datasetMapper.insertVersion(newVersionId, datasetId, nextVersionNo, datasetMapper.findVersionItemCount(draftVersionId), now);
    copyVersionContent(draftVersionId, newVersionId);
    datasetMapper.refreshDatasetVersionStats(datasetId, now());
    return datasetMapper.findVersion(newVersionId);
  }

  @Transactional
  public void deleteVersion(String versionId) {
    DatasetVersionDto version = datasetMapper.findVersion(versionId);
    if (version.draft()) {
      throw new IllegalArgumentException("草稿版本不能删除");
    }
    datasetMapper.softDeleteVersion(versionId, now());
    datasetMapper.refreshDatasetVersionStats(version.datasetId(), now());
  }

  @Transactional
  public DatasetVersionDto coverDraft(String datasetId, String sourceVersionId) {
    DatasetVersionDto source = datasetMapper.findVersion(sourceVersionId);
    if (source.draft()) {
      throw new IllegalArgumentException("不能用草稿覆盖草稿");
    }
    String draftVersionId = datasetMapper.findDraftVersionId(datasetId);
    clearVersionContent(draftVersionId);
    copyVersionContent(sourceVersionId, draftVersionId);
    updateItemCount(draftVersionId);
    return datasetMapper.findVersion(draftVersionId);
  }

  private void insertCells(String versionId, String itemId, Map<String, String> values) {
    List<FieldDto> fields = listFields(versionId);
    String now = now();
    Map<String, String> safeValues = values == null ? Map.of() : values;
    for (FieldDto field : fields) {
      datasetMapper.insertCell(id(), versionId, itemId, field.id(), safeValues.getOrDefault(field.id(), ""), now);
    }
  }

  private void validateFields(List<FieldInput> fields) {
    if (fields == null || fields.isEmpty()) {
      throw new IllegalArgumentException("请维护表头");
    }
    for (FieldInput field : fields) {
      if (!StringUtils.hasText(field.fieldName())) {
        throw new IllegalArgumentException("列名不能为空");
      }
      if (!SUPPORTED_FIELD_TYPES.contains(field.fieldType())) {
        throw new IllegalArgumentException("字段类型仅支持string、number、boolean");
      }
    }
  }

  private void validateExcelFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("请上传Excel文件");
    }
    String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
      throw new IllegalArgumentException("仅支持xlsx或xls文件");
    }
  }

  private List<Map<String, String>> readExcelRows(MultipartFile file, List<FieldDto> fields) {
    DataFormatter formatter = new DataFormatter();
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      if (workbook.getNumberOfSheets() == 0) {
        throw new IllegalArgumentException("Excel文件没有工作表");
      }
      Sheet sheet = workbook.getSheetAt(0);
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        throw new IllegalArgumentException("Excel第一行必须是表头");
      }
      validateExcelHeader(headerRow, fields, formatter);

      List<Map<String, String>> rows = new ArrayList<>();
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || isBlankRow(row, fields.size(), formatter)) {
          continue;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (int columnIndex = 0; columnIndex < fields.size(); columnIndex++) {
          FieldDto field = fields.get(columnIndex);
          values.put(field.id(), getCellText(row.getCell(columnIndex), formatter));
        }
        rows.add(values);
      }
      return rows;
    } catch (IllegalArgumentException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalArgumentException("读取Excel文件失败");
    }
  }

  private void validateExcelHeader(Row headerRow, List<FieldDto> fields, DataFormatter formatter) {
    int lastCellNum = headerRow.getLastCellNum();
    if (lastCellNum != fields.size()) {
      throw new IllegalArgumentException("Excel表头列数必须和评测集表头一致");
    }
    for (int columnIndex = 0; columnIndex < fields.size(); columnIndex++) {
      String actual = getCellText(headerRow.getCell(columnIndex), formatter);
      String expected = fields.get(columnIndex).fieldName();
      if (!expected.equals(actual)) {
        throw new IllegalArgumentException("Excel第" + (columnIndex + 1) + "列表头应为：" + expected);
      }
    }
  }

  private boolean isBlankRow(Row row, int fieldCount, DataFormatter formatter) {
    for (int columnIndex = 0; columnIndex < fieldCount; columnIndex++) {
      if (StringUtils.hasText(getCellText(row.getCell(columnIndex), formatter))) {
        return false;
      }
    }
    return true;
  }

  private String getCellText(Cell cell, DataFormatter formatter) {
    return cell == null ? "" : formatter.formatCellValue(cell).trim();
  }

  private RowDto getRow(String itemId) {
    DatasetMapper.RowRecord row = datasetMapper.findRow(itemId);
    Map<String, String> values = datasetMapper.loadValues(List.of(itemId)).getOrDefault(itemId, Map.of());
    return new RowDto(row.id(), row.rowNo(), values, row.createdAt(), row.updatedAt());
  }

  private void copyVersionContent(String sourceVersionId, String targetVersionId) {
    Map<String, String> fieldIdMap = new LinkedHashMap<>();
    for (FieldDto field : listFields(sourceVersionId)) {
      String newFieldId = id();
      fieldIdMap.put(field.id(), newFieldId);
      String now = now();
      datasetMapper.insertField(
          newFieldId,
          targetVersionId,
          field.fieldName(),
          field.fieldType(),
          bool(field.required()),
          field.description(),
          field.displayOrder(),
          now);
    }

    List<DatasetMapper.RowRecord> sourceRows = datasetMapper.listAllRows(sourceVersionId);
    Map<String, Map<String, String>> sourceValues = datasetMapper.loadValues(sourceRows.stream().map(DatasetMapper.RowRecord::id).toList());
    for (DatasetMapper.RowRecord sourceRow : sourceRows) {
      String newItemId = id();
      String now = now();
      datasetMapper.insertItem(newItemId, targetVersionId, sourceRow.rowNo(), now);
      Map<String, String> rowValues = sourceValues.getOrDefault(sourceRow.id(), Map.of());
      for (Map.Entry<String, String> entry : rowValues.entrySet()) {
        String targetFieldId = fieldIdMap.get(entry.getKey());
        if (targetFieldId != null) {
          datasetMapper.insertCell(id(), targetVersionId, newItemId, targetFieldId, entry.getValue(), now);
        }
      }
    }
  }

  private void clearVersionContent(String versionId) {
    datasetMapper.clearVersionContent(versionId);
  }

  private void addBlankCellsForNewField(String versionId, String fieldId) {
    String now = now();
    for (String itemId : datasetMapper.listItemIds(versionId)) {
      datasetMapper.insertCell(id(), versionId, itemId, fieldId, "", now);
    }
  }

  private void ensureDraft(String versionId) {
    if (datasetMapper.findVersionNo(versionId) != 0) {
      throw new IllegalArgumentException("只有草稿版本允许修改");
    }
  }

  private void updateItemCount(String versionId) {
    datasetMapper.updateVersionItemCount(versionId, (int) datasetMapper.countRows(versionId), now());
  }

  private void touchVersion(String versionId) {
    datasetMapper.touchVersionAndDataset(versionId, now());
  }

  private int bool(Boolean value) {
    return Boolean.TRUE.equals(value) ? 1 : 0;
  }

  private String id() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private String now() {
    return String.valueOf(System.currentTimeMillis());
  }
}
