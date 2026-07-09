package com.agentnexus.backend.dataset.service;

import com.agentnexus.backend.common.PageResponse;
import com.agentnexus.backend.dataset.api.dto.request.CreateDatasetRequest;
import com.agentnexus.backend.dataset.api.dto.response.DatasetSummary;
import com.agentnexus.backend.dataset.api.dto.response.DatasetVersionDto;
import com.agentnexus.backend.dataset.api.dto.response.FieldDto;
import com.agentnexus.backend.dataset.api.dto.request.FieldInput;
import com.agentnexus.backend.dataset.api.dto.response.ImportRowsResult;
import com.agentnexus.backend.dataset.api.dto.response.RowDto;
import com.agentnexus.backend.dataset.api.dto.request.RowInput;
import com.agentnexus.backend.dataset.api.dto.response.VersionDetail;
import com.agentnexus.backend.dataset.repository.DatasetRepository;
import com.agentnexus.backend.dataset.repository.DatasetRowRecord;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DatasetService {
  private static final List<String> SUPPORTED_FIELD_TYPES = List.of("string", "number", "boolean");

  private final DatasetRepository datasetRepository;

  public DatasetService(DatasetRepository datasetRepository) {
    this.datasetRepository = datasetRepository;
  }

  public PageResponse<DatasetSummary> listDatasets(int page, int size, String keyword, String sortBy, String sortOrder) {
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 100);
    int offset = (safePage - 1) * safeSize;
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    String orderColumn = "createdDate".equals(sortBy) ? "d.created_date" : "d.last_updated_date";
    String orderDirection = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
    List<DatasetSummary> records = datasetRepository.listDatasetSummaries(like, orderColumn, orderDirection, safeSize, offset);
    long total = datasetRepository.countDatasetSummaries(like);
    return new PageResponse<>(records, total, safePage, safeSize);
  }

  @Transactional
  public DatasetSummary createDataset(CreateDatasetRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("评测集参数不能为空");
    }
    String name = normalizeDatasetName(request.name());
    String description = normalizeDescription(request.description());
    String datasetId = id();
    String draftVersionId = id();
    String now = now();
    datasetRepository.insertDataset(datasetId, name, description, now);
    datasetRepository.insertVersion(draftVersionId, datasetId, 0, 0, now);
    replaceFields(draftVersionId, request.fields() == null ? List.of() : request.fields());
    return getDatasetSummary(datasetId);
  }

  public DatasetSummary getDatasetSummary(String datasetId) {
    return datasetRepository.findDatasetSummary(datasetId);
  }

  public void deleteDataset(String datasetId) {
    datasetRepository.deleteDataset(datasetId);
  }

  public List<DatasetVersionDto> listVersions(String datasetId) {
    return datasetRepository.listVersions(datasetId);
  }

  public VersionDetail getVersionDetail(String versionId, int page, int size, String fieldId, String keyword) {
    DatasetVersionDto version = datasetRepository.findVersion(versionId);
    List<FieldDto> fields = listFields(versionId);
    PageResponse<RowDto> rows = listRows(versionId, page, size, fieldId, keyword);
    return new VersionDetail(version, fields, rows);
  }

  public List<FieldDto> listFields(String versionId) {
    return datasetRepository.listFields(versionId);
  }

  public PageResponse<RowDto> listRows(String versionId, int page, int size, String fieldId, String keyword) {
    int offset = Math.max(page - 1, 0) * size;
    boolean searching = StringUtils.hasText(fieldId) && StringUtils.hasText(keyword);
    List<DatasetRowRecord> rowRecords;
    long total;
    if (searching) {
      String like = "%" + keyword.trim() + "%";
      rowRecords = datasetRepository.searchRows(versionId, fieldId, like, size, offset);
      total = datasetRepository.countSearchRows(versionId, fieldId, like);
    } else {
      rowRecords = datasetRepository.listRows(versionId, size, offset);
      total = datasetRepository.countRows(versionId);
    }
    Map<String, Map<String, String>> values = datasetRepository.loadValues(rowRecords.stream().map(DatasetRowRecord::id).toList());
    List<RowDto> rows = rowRecords.stream()
        .map(row -> new RowDto(row.id(), row.rowNo(), values.getOrDefault(row.id(), Map.of()), row.createdDate(), row.lastUpdatedDate()))
        .toList();
    return new PageResponse<>(rows, total, page, size);
  }

  @Transactional
  public List<FieldDto> replaceFields(String versionId, List<FieldInput> fields) {
    ensureDraft(versionId);
    validateFields(fields);
    List<String> existingIds = datasetRepository.listFieldIds(versionId);
    Map<String, FieldDto> existingFields = new HashMap<>();
    for (FieldDto existingField : listFields(versionId)) {
      existingFields.put(existingField.id(), existingField);
    }
    List<String> keptIds = new ArrayList<>();
    int order = 1;
    String now = now();
    for (FieldInput field : fields) {
      String fieldId = StringUtils.hasText(field.id()) && existingIds.contains(field.id()) ? field.id() : id();
      keptIds.add(fieldId);
      if (existingIds.contains(fieldId)) {
        FieldDto existingField = existingFields.get(fieldId);
        datasetRepository.updateField(fieldId, field.fieldName(), existingField.fieldType(), bool(existingField.required()), field.description(), order++, now);
      } else {
        datasetRepository.insertField(fieldId, versionId, field.fieldName(), field.fieldType(), bool(field.required()), field.description(), order++, now);
        addBlankCellsForNewField(versionId, fieldId);
      }
    }
    List<String> removedIds = existingIds.stream().filter(existing -> !keptIds.contains(existing)).toList();
    if (!removedIds.isEmpty()) {
      datasetRepository.deleteFields(versionId, removedIds);
    }
    touchVersion(versionId);
    return listFields(versionId);
  }

  @Transactional
  public RowDto addRow(String versionId, RowInput request) {
    ensureDraft(versionId);
    String itemId = id();
    String now = now();
    datasetRepository.insertItem(itemId, versionId, datasetRepository.nextRowNo(versionId), now);
    insertCells(versionId, itemId, request.values());
    updateItemCount(versionId);
    touchVersion(versionId);
    return getRow(itemId);
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
    addRows(versionId, rows);
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
    datasetRepository.clearVersionCells(versionId);
    datasetRepository.clearVersionItems(versionId);
    addRows(versionId, rows);
    updateItemCount(versionId);
    touchVersion(versionId);
    return new ImportRowsResult(rows.size());
  }

  @Transactional
  public RowDto updateRow(String versionId, String itemId, RowInput request) {
    ensureDraft(versionId);
    datasetRepository.updateItem(itemId, versionId, now());
    datasetRepository.deleteCellsByItem(itemId);
    insertCells(versionId, itemId, request.values());
    touchVersion(versionId);
    return getRow(itemId);
  }

  @Transactional
  public void deleteRow(String versionId, String itemId) {
    ensureDraft(versionId);
    datasetRepository.deleteCellsByItem(itemId);
    datasetRepository.deleteItem(itemId, versionId);
    updateItemCount(versionId);
    touchVersion(versionId);
  }

  @Transactional
  public DatasetVersionDto publish(String datasetId) {
    String draftVersionId = datasetRepository.findDraftVersionId(datasetId);
    int nextVersionNo = datasetRepository.nextVersionNo(datasetId);
    String newVersionId = id();
    String now = now();
    datasetRepository.insertVersion(newVersionId, datasetId, nextVersionNo, datasetRepository.findVersionItemCount(draftVersionId), now);
    copyVersionContent(draftVersionId, newVersionId);
    datasetRepository.refreshDatasetVersionStats(datasetId, now());
    return datasetRepository.findVersion(newVersionId);
  }

  @Transactional
  public void deleteVersion(String versionId) {
    DatasetVersionDto version = datasetRepository.findVersion(versionId);
    if (version.draft()) {
      throw new IllegalArgumentException("草稿版本不能删除");
    }
    datasetRepository.deleteVersion(versionId);
    datasetRepository.refreshDatasetVersionStats(version.datasetId(), now());
  }

  @Transactional
  public DatasetVersionDto coverDraft(String datasetId, String sourceVersionId) {
    DatasetVersionDto source = datasetRepository.findVersion(sourceVersionId);
    if (source.draft()) {
      throw new IllegalArgumentException("不能用草稿覆盖草稿");
    }
    String draftVersionId = datasetRepository.findDraftVersionId(datasetId);
    clearVersionContent(draftVersionId);
    copyVersionContent(sourceVersionId, draftVersionId);
    updateItemCount(draftVersionId);
    return datasetRepository.findVersion(draftVersionId);
  }

  private void insertCells(String versionId, String itemId, Map<String, String> values) {
    List<FieldDto> fields = listFields(versionId);
    String now = now();
    Map<String, String> safeValues = values == null ? Map.of() : values;
    for (FieldDto field : fields) {
      datasetRepository.insertCell(id(), versionId, itemId, field.id(), safeValues.getOrDefault(field.id(), ""), now);
    }
  }

  private void validateFields(List<FieldInput> fields) {
    if (fields == null || fields.isEmpty()) {
      throw new IllegalArgumentException("请维护表头");
    }
    if (fields.size() > 10) {
      throw new IllegalArgumentException("评测集最多支持10列");
    }
    Set<String> names = new HashSet<>();
    for (FieldInput field : fields) {
      if (!StringUtils.hasText(field.fieldName())) {
        throw new IllegalArgumentException("列名不能为空");
      }
      if (!names.add(field.fieldName().trim())) {
        throw new IllegalArgumentException("列名不能重复");
      }
      if (!SUPPORTED_FIELD_TYPES.contains(field.fieldType())) {
        throw new IllegalArgumentException("字段类型仅支持string、number、boolean");
      }
    }
  }

  private String normalizeDatasetName(String name) {
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("评测集名称不能为空");
    }
    String normalized = name.trim();
    if (normalized.length() > 50) {
      throw new IllegalArgumentException("评测集名称不能超过50个字符");
    }
    return normalized;
  }

  private String normalizeDescription(String description) {
    String normalized = description == null ? "" : description.trim();
    if (normalized.length() > 200) {
      throw new IllegalArgumentException("描述不能超过200个字符");
    }
    return normalized;
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
      Map<CellPosition, String> mergedCellTextMap = buildMergedCellTextMap(sheet, formatter);
      Map<Integer, FieldDto> matchedColumns = mapExcelColumns(headerRow, fields, formatter, mergedCellTextMap);

      List<Map<String, String>> rows = new ArrayList<>();
      Set<String> rowKeys = new HashSet<>();
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || isBlankRow(sheet, rowIndex, matchedColumns.keySet(), formatter, mergedCellTextMap)) {
          continue;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<Integer, FieldDto> entry : matchedColumns.entrySet()) {
          String value = getCellText(sheet, rowIndex, entry.getKey(), formatter, mergedCellTextMap);
          validateExcelCell(value, entry.getValue(), rowIndex + 1, entry.getKey() + 1);
          values.put(entry.getValue().id(), value);
        }
        String rowKey = excelRowDuplicateKey(fields, values);
        if (rowKeys.add(rowKey)) {
          rows.add(values);
        }
      }
      return rows;
    } catch (IllegalArgumentException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalArgumentException("读取Excel文件失败");
    }
  }

  private Map<Integer, FieldDto> mapExcelColumns(
      Row headerRow,
      List<FieldDto> fields,
      DataFormatter formatter,
      Map<CellPosition, String> mergedCellTextMap
  ) {
    Map<String, FieldDto> fieldByName = new LinkedHashMap<>();
    for (FieldDto field : fields) {
      if (fieldByName.putIfAbsent(field.fieldName(), field) != null) {
        throw new IllegalArgumentException("评测集表头存在重复列名：" + field.fieldName());
      }
    }

    Map<Integer, FieldDto> matchedColumns = new LinkedHashMap<>();
    Set<String> matchedFieldIds = new HashSet<>();
    int lastCellNum = headerRow.getLastCellNum();
    if (lastCellNum < 0) {
      throw new IllegalArgumentException("Excel第一行必须是表头");
    }

    Sheet sheet = headerRow.getSheet();
    int firstCellNum = Math.max(headerRow.getFirstCellNum(), 0);
    for (int columnIndex = firstCellNum; columnIndex < lastCellNum; columnIndex++) {
      String header = getCellText(sheet, headerRow.getRowNum(), columnIndex, formatter, mergedCellTextMap);
      if (!StringUtils.hasText(header)) {
        continue;
      }
      FieldDto matchedField = fieldByName.get(header);
      if (matchedField == null) {
        continue;
      }
      if (!matchedFieldIds.add(matchedField.id())) {
        throw new IllegalArgumentException("Excel表头存在重复列：" + header);
      }
      matchedColumns.put(columnIndex, matchedField);
    }

    List<String> missingRequiredFields = fields.stream()
        .filter(field -> Boolean.TRUE.equals(field.required()))
        .filter(field -> !matchedFieldIds.contains(field.id()))
        .map(FieldDto::fieldName)
        .toList();
    if (!missingRequiredFields.isEmpty()) {
      throw new IllegalArgumentException("Excel缺少必填列：" + String.join("、", missingRequiredFields));
    }
    if (matchedColumns.isEmpty()) {
      throw new IllegalArgumentException("Excel表头未匹配到评测集字段");
    }
    return matchedColumns;
  }

  private boolean isBlankRow(
      Sheet sheet,
      int rowIndex,
      Set<Integer> columnIndexes,
      DataFormatter formatter,
      Map<CellPosition, String> mergedCellTextMap
  ) {
    for (Integer columnIndex : columnIndexes) {
      if (StringUtils.hasText(getCellText(sheet, rowIndex, columnIndex, formatter, mergedCellTextMap))) {
        return false;
      }
    }
    return true;
  }

  private void addRows(String versionId, List<Map<String, String>> rows) {
    for (Map<String, String> values : rows) {
      addRow(versionId, new RowInput(null, values));
    }
  }

  private void validateExcelCell(String value, FieldDto field, int rowNumber, int columnNumber) {
    String position = "Excel第" + rowNumber + "行，第" + columnNumber + "列（" + field.fieldName() + "）";
    if (!StringUtils.hasText(value)) {
      if (Boolean.TRUE.equals(field.required())) {
        throw new IllegalArgumentException(position + "不能为空");
      }
      return;
    }
    if ("number".equals(field.fieldType())) {
      try {
        new BigDecimal(value);
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException(position + "应为数字");
      }
    }
    if ("boolean".equals(field.fieldType())
        && !"true".equalsIgnoreCase(value)
        && !"false".equalsIgnoreCase(value)) {
      throw new IllegalArgumentException(position + "应为布尔值true或false");
    }
  }

  private Map<CellPosition, String> buildMergedCellTextMap(Sheet sheet, DataFormatter formatter) {
    Map<CellPosition, String> mergedCellTextMap = new HashMap<>();
    for (CellRangeAddress range : sheet.getMergedRegions()) {
      Row firstRow = sheet.getRow(range.getFirstRow());
      Cell firstCell = firstRow == null ? null : firstRow.getCell(range.getFirstColumn());
      String text = getCellText(firstCell, formatter);
      for (int rowIndex = range.getFirstRow(); rowIndex <= range.getLastRow(); rowIndex++) {
        for (int columnIndex = range.getFirstColumn(); columnIndex <= range.getLastColumn(); columnIndex++) {
          mergedCellTextMap.put(new CellPosition(rowIndex, columnIndex), text);
        }
      }
    }
    return mergedCellTextMap;
  }

  private String excelRowDuplicateKey(List<FieldDto> fields, Map<String, String> values) {
    StringBuilder builder = new StringBuilder();
    for (FieldDto field : fields) {
      builder.append(values.getOrDefault(field.id(), "")).append('\u001F');
    }
    return builder.toString();
  }

  private String getCellText(
      Sheet sheet,
      int rowIndex,
      int columnIndex,
      DataFormatter formatter,
      Map<CellPosition, String> mergedCellTextMap
  ) {
    String mergedText = mergedCellTextMap.get(new CellPosition(rowIndex, columnIndex));
    if (mergedText != null) {
      return mergedText;
    }
    Row row = sheet.getRow(rowIndex);
    return row == null ? "" : getCellText(row.getCell(columnIndex), formatter);
  }

  private String getCellText(Cell cell, DataFormatter formatter) {
    return cell == null ? "" : formatter.formatCellValue(cell).trim();
  }

  private record CellPosition(int rowIndex, int columnIndex) {
  }

  private RowDto getRow(String itemId) {
    DatasetRowRecord row = datasetRepository.findRow(itemId);
    Map<String, String> values = datasetRepository.loadValues(List.of(itemId)).getOrDefault(itemId, Map.of());
    return new RowDto(row.id(), row.rowNo(), values, row.createdDate(), row.lastUpdatedDate());
  }

  private void copyVersionContent(String sourceVersionId, String targetVersionId) {
    Map<String, String> fieldIdMap = new LinkedHashMap<>();
    for (FieldDto field : listFields(sourceVersionId)) {
      String newFieldId = id();
      fieldIdMap.put(field.id(), newFieldId);
      String now = now();
      datasetRepository.insertField(
          newFieldId,
          targetVersionId,
          field.fieldName(),
          field.fieldType(),
          bool(field.required()),
          field.description(),
          field.displayOrder(),
          now);
    }

    List<DatasetRowRecord> sourceRows = datasetRepository.listAllRows(sourceVersionId);
    Map<String, Map<String, String>> sourceValues = datasetRepository.loadValues(sourceRows.stream().map(DatasetRowRecord::id).toList());
    for (DatasetRowRecord sourceRow : sourceRows) {
      String newItemId = id();
      String now = now();
      datasetRepository.insertItem(newItemId, targetVersionId, sourceRow.rowNo(), now);
      Map<String, String> rowValues = sourceValues.getOrDefault(sourceRow.id(), Map.of());
      for (Map.Entry<String, String> entry : rowValues.entrySet()) {
        String targetFieldId = fieldIdMap.get(entry.getKey());
        if (targetFieldId != null) {
          datasetRepository.insertCell(id(), targetVersionId, newItemId, targetFieldId, entry.getValue(), now);
        }
      }
    }
  }

  private void clearVersionContent(String versionId) {
    datasetRepository.clearVersionContent(versionId);
  }

  private void addBlankCellsForNewField(String versionId, String fieldId) {
    String now = now();
    for (String itemId : datasetRepository.listItemIds(versionId)) {
      datasetRepository.insertCell(id(), versionId, itemId, fieldId, "", now);
    }
  }

  private void ensureDraft(String versionId) {
    if (datasetRepository.findVersionNo(versionId) != 0) {
      throw new IllegalArgumentException("只有草稿版本允许修改");
    }
  }

  private void updateItemCount(String versionId) {
    datasetRepository.updateVersionItemCount(versionId, (int) datasetRepository.countRows(versionId), now());
  }

  private void touchVersion(String versionId) {
    datasetRepository.touchVersionAndDataset(versionId, now());
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
