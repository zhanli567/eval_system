package com.evalsystem.dataset.service;

import com.evalsystem.common.PageResponse;
import com.evalsystem.dataset.dto.BatchRowsRequest;
import com.evalsystem.dataset.dto.CreateDatasetRequest;
import com.evalsystem.dataset.dto.DatasetSummary;
import com.evalsystem.dataset.dto.DatasetVersionDto;
import com.evalsystem.dataset.dto.FieldDto;
import com.evalsystem.dataset.dto.FieldInput;
import com.evalsystem.dataset.dto.RowDto;
import com.evalsystem.dataset.dto.RowInput;
import com.evalsystem.dataset.dto.VersionDetail;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DatasetService {
  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedJdbcTemplate;

  public DatasetService(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.namedJdbcTemplate = namedJdbcTemplate;
  }

  @PostConstruct
  public void seedDemoData() {
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM eval_dataset", Integer.class);
    if (count != null && count > 0) {
      return;
    }

    DatasetSummary dataset = createDataset(new CreateDatasetRequest(
        "计算机专业知识评测",
        "用于演示智能体问答评测的样例数据集",
        List.of(
            new FieldInput(null, "query", "string", true, "用户问题"),
            new FieldInput(null, "reference_response", "string", false, "参考答案"),
            new FieldInput(null, "difficulty", "string", false, "难度")
        )
    ));
    String draftVersionId = listVersions(dataset.id()).stream()
        .filter(DatasetVersionDto::draft)
        .findFirst()
        .orElseThrow()
        .id();
    List<FieldDto> fields = listFields(draftVersionId);
    String queryFieldId = fields.get(0).id();
    String referenceFieldId = fields.get(1).id();
    String difficultyFieldId = fields.get(2).id();

    addRows(draftVersionId, new BatchRowsRequest(List.of(
        Map.of(
            queryFieldId, "什么是进程调度？常见的调度算法有哪些？",
            referenceFieldId, "进程调度是操作系统从就绪队列中选择进程分配CPU的过程，常见算法包括先来先服务、短作业优先、时间片轮转和优先级调度。",
            difficultyFieldId, "medium"
        ),
        Map.of(
            queryFieldId, "什么是页面置换算法？常见的算法有哪些？",
            referenceFieldId, "页面置换算法用于在缺页时选择被换出的页面，常见算法包括FIFO、LRU、LFU和Clock算法。",
            difficultyFieldId, "medium"
        ),
        Map.of(
            queryFieldId, "请解释TCP三次握手的过程。",
            referenceFieldId, "客户端发送SYN，服务端返回SYN-ACK，客户端再发送ACK，连接建立。",
            difficultyFieldId, "easy"
        )
    )));
    publish(dataset.id());
  }

  public PageResponse<DatasetSummary> listDatasets(int page, int size, String keyword) {
    int offset = Math.max(page - 1, 0) * size;
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    List<DatasetSummary> records = jdbcTemplate.query("""
            SELECT d.id, d.name, d.description, d.published_version_count,
                   d.latest_published_version_id, COALESCE(v.item_count, 0) latest_item_count,
                   d.created_at, d.updated_at
            FROM eval_dataset d
            LEFT JOIN eval_dataset_version v ON d.latest_published_version_id = v.id
            WHERE d.is_deleted = 0 AND (? = '%%' OR d.name LIKE ?)
            ORDER BY d.updated_at DESC
            LIMIT ? OFFSET ?
            """,
        (rs, rowNum) -> new DatasetSummary(
            rs.getString("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("published_version_count"),
            rs.getString("latest_published_version_id"),
            rs.getInt("latest_item_count"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        ),
        like, like, size, offset);
    Long total = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM eval_dataset
            WHERE is_deleted = 0 AND (? = '%%' OR name LIKE ?)
            """, Long.class, like, like);
    return new PageResponse<>(records, total == null ? 0 : total, page, size);
  }

  @Transactional
  public DatasetSummary createDataset(CreateDatasetRequest request) {
    String datasetId = id();
    String draftVersionId = id();
    String now = now();
    jdbcTemplate.update("""
            INSERT INTO eval_dataset
            (id, name, description, published_version_count, latest_published_version_id, is_deleted, created_at, updated_at)
            VALUES (?, ?, ?, 0, NULL, 0, ?, ?)
            """,
        datasetId, request.name(), request.description(), now, now);
    jdbcTemplate.update("""
            INSERT INTO eval_dataset_version
            (id, dataset_id, version_no, item_count, is_deleted, created_at, updated_at)
            VALUES (?, ?, 0, 0, 0, ?, ?)
            """,
        draftVersionId, datasetId, now, now);
    replaceFields(draftVersionId, request.fields() == null ? List.of() : request.fields());
    return getDatasetSummary(datasetId);
  }

  public DatasetSummary getDatasetSummary(String datasetId) {
    return jdbcTemplate.queryForObject("""
            SELECT d.id, d.name, d.description, d.published_version_count,
                   d.latest_published_version_id, COALESCE(v.item_count, 0) latest_item_count,
                   d.created_at, d.updated_at
            FROM eval_dataset d
            LEFT JOIN eval_dataset_version v ON d.latest_published_version_id = v.id
            WHERE d.id = ?
            """,
        (rs, rowNum) -> new DatasetSummary(
            rs.getString("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("published_version_count"),
            rs.getString("latest_published_version_id"),
            rs.getInt("latest_item_count"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        ),
        datasetId);
  }

  public void deleteDataset(String datasetId) {
    jdbcTemplate.update(
        "UPDATE eval_dataset SET is_deleted = 1, updated_at = ? WHERE id = ?",
        now(), datasetId);
  }

  public List<DatasetVersionDto> listVersions(String datasetId) {
    return jdbcTemplate.query("""
            SELECT id, dataset_id, version_no, item_count, created_at, updated_at
            FROM eval_dataset_version
            WHERE dataset_id = ? AND is_deleted = 0
            ORDER BY version_no ASC
            """,
        (rs, rowNum) -> toVersionDto(
            rs.getString("id"),
            rs.getString("dataset_id"),
            rs.getInt("version_no"),
            rs.getInt("item_count"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        ),
        datasetId);
  }

  public VersionDetail getVersionDetail(String versionId, int page, int size, String fieldId, String keyword) {
    DatasetVersionDto version = getVersion(versionId);
    List<FieldDto> fields = listFields(versionId);
    PageResponse<RowDto> rows = listRows(versionId, page, size, fieldId, keyword);
    return new VersionDetail(version, fields, rows);
  }

  public List<FieldDto> listFields(String versionId) {
    return jdbcTemplate.query("""
            SELECT id, version_id, field_name, field_type, is_required, description, display_order
            FROM eval_dataset_field
            WHERE version_id = ?
            ORDER BY display_order ASC
            """,
        (rs, rowNum) -> new FieldDto(
            rs.getString("id"),
            rs.getString("version_id"),
            rs.getString("field_name"),
            rs.getString("field_type"),
            rs.getInt("is_required") == 1,
            rs.getString("description"),
            rs.getInt("display_order")
        ),
        versionId);
  }

  public PageResponse<RowDto> listRows(String versionId, int page, int size, String fieldId, String keyword) {
    int offset = Math.max(page - 1, 0) * size;
    boolean searching = StringUtils.hasText(fieldId) && StringUtils.hasText(keyword);
    List<RowShell> shells;
    Long total;
    if (searching) {
      String like = "%" + keyword.trim() + "%";
      shells = jdbcTemplate.query("""
              SELECT DISTINCT i.id, i.row_no, i.created_at, i.updated_at
              FROM eval_dataset_item i
              JOIN eval_dataset_item_cell c ON c.item_id = i.id
              WHERE i.version_id = ? AND c.field_id = ? AND c.cell_value LIKE ?
              ORDER BY i.row_no ASC
              LIMIT ? OFFSET ?
              """,
          (rs, rowNum) -> new RowShell(
              rs.getString("id"),
              rs.getInt("row_no"),
              rs.getString("created_at"),
              rs.getString("updated_at")
          ),
          versionId, fieldId, like, size, offset);
      total = jdbcTemplate.queryForObject("""
              SELECT COUNT(DISTINCT i.id)
              FROM eval_dataset_item i
              JOIN eval_dataset_item_cell c ON c.item_id = i.id
              WHERE i.version_id = ? AND c.field_id = ? AND c.cell_value LIKE ?
              """, Long.class, versionId, fieldId, like);
    } else {
      shells = jdbcTemplate.query("""
              SELECT id, row_no, created_at, updated_at
              FROM eval_dataset_item
              WHERE version_id = ?
              ORDER BY row_no ASC
              LIMIT ? OFFSET ?
              """,
          (rs, rowNum) -> new RowShell(
              rs.getString("id"),
              rs.getInt("row_no"),
              rs.getString("created_at"),
              rs.getString("updated_at")
          ),
          versionId, size, offset);
      total = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM eval_dataset_item WHERE version_id = ?",
          Long.class,
          versionId);
    }
    Map<String, Map<String, String>> values = loadValues(shells.stream().map(RowShell::id).toList());
    List<RowDto> rows = shells.stream()
        .map(row -> new RowDto(row.id(), row.rowNo(), values.getOrDefault(row.id(), Map.of()), row.createdAt(), row.updatedAt()))
        .toList();
    return new PageResponse<>(rows, total == null ? 0 : total, page, size);
  }

  @Transactional
  public List<FieldDto> replaceFields(String versionId, List<FieldInput> fields) {
    ensureDraft(versionId);
    List<String> existingIds = jdbcTemplate.queryForList(
        "SELECT id FROM eval_dataset_field WHERE version_id = ?",
        String.class,
        versionId);
    List<String> keptIds = new ArrayList<>();
    int order = 1;
    String now = now();
    for (FieldInput field : fields) {
      String fieldId = StringUtils.hasText(field.id()) && existingIds.contains(field.id()) ? field.id() : id();
      keptIds.add(fieldId);
      if (existingIds.contains(fieldId)) {
        jdbcTemplate.update("""
                UPDATE eval_dataset_field
                SET field_name = ?, field_type = ?, is_required = ?, description = ?, display_order = ?, updated_at = ?
                WHERE id = ?
                """,
            field.fieldName(), field.fieldType(), bool(field.required()), field.description(), order++, now, fieldId);
      } else {
        jdbcTemplate.update("""
                INSERT INTO eval_dataset_field
                (id, version_id, field_name, field_type, is_required, description, display_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            fieldId, versionId, field.fieldName(), field.fieldType(), bool(field.required()), field.description(), order++, now, now);
        addBlankCellsForNewField(versionId, fieldId);
      }
    }
    List<String> removedIds = existingIds.stream().filter(existing -> !keptIds.contains(existing)).toList();
    if (!removedIds.isEmpty()) {
      MapSqlParameterSource params = new MapSqlParameterSource()
          .addValue("versionId", versionId)
          .addValue("fieldIds", removedIds);
      namedJdbcTemplate.update(
          "DELETE FROM eval_dataset_item_cell WHERE version_id = :versionId AND field_id IN (:fieldIds)",
          params);
      namedJdbcTemplate.update(
          "DELETE FROM eval_dataset_field WHERE version_id = :versionId AND id IN (:fieldIds)",
          params);
    }
    touchVersion(versionId);
    return listFields(versionId);
  }

  @Transactional
  public RowDto addRow(String versionId, RowInput request) {
    ensureDraft(versionId);
    String itemId = id();
    Integer nextRowNo = jdbcTemplate.queryForObject(
        "SELECT COALESCE(MAX(row_no), 0) + 1 FROM eval_dataset_item WHERE version_id = ?",
        Integer.class,
        versionId);
    String now = now();
    jdbcTemplate.update("""
            INSERT INTO eval_dataset_item (id, version_id, row_no, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """,
        itemId, versionId, nextRowNo == null ? 1 : nextRowNo, now, now);
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
  public RowDto updateRow(String versionId, String itemId, RowInput request) {
    ensureDraft(versionId);
    jdbcTemplate.update(
        "UPDATE eval_dataset_item SET updated_at = ? WHERE id = ? AND version_id = ?",
        now(), itemId, versionId);
    jdbcTemplate.update("DELETE FROM eval_dataset_item_cell WHERE item_id = ?", itemId);
    insertCells(versionId, itemId, request.values());
    touchVersion(versionId);
    return getRow(itemId);
  }

  @Transactional
  public void deleteRow(String versionId, String itemId) {
    ensureDraft(versionId);
    jdbcTemplate.update("DELETE FROM eval_dataset_item_cell WHERE item_id = ?", itemId);
    jdbcTemplate.update("DELETE FROM eval_dataset_item WHERE id = ? AND version_id = ?", itemId, versionId);
    updateItemCount(versionId);
    touchVersion(versionId);
  }

  @Transactional
  public DatasetVersionDto publish(String datasetId) {
    String draftVersionId = getDraftVersionId(datasetId);
    Integer nextVersionNo = jdbcTemplate.queryForObject("""
            SELECT COALESCE(MAX(version_no), 0) + 1
            FROM eval_dataset_version
            WHERE dataset_id = ? AND is_deleted = 0
            """, Integer.class, datasetId);
    String newVersionId = id();
    String now = now();
    Integer itemCount = jdbcTemplate.queryForObject(
        "SELECT item_count FROM eval_dataset_version WHERE id = ?",
        Integer.class,
        draftVersionId);
    jdbcTemplate.update("""
            INSERT INTO eval_dataset_version
            (id, dataset_id, version_no, item_count, is_deleted, created_at, updated_at)
            VALUES (?, ?, ?, ?, 0, ?, ?)
            """,
        newVersionId, datasetId, nextVersionNo, itemCount, now, now);
    copyVersionContent(draftVersionId, newVersionId);
    refreshDatasetVersionStats(datasetId);
    return getVersion(newVersionId);
  }

  @Transactional
  public void deleteVersion(String versionId) {
    DatasetVersionDto version = getVersion(versionId);
    if (version.draft()) {
      throw new IllegalArgumentException("草稿版本不能删除");
    }
    jdbcTemplate.update("UPDATE eval_dataset_version SET is_deleted = 1, updated_at = ? WHERE id = ?", now(), versionId);
    refreshDatasetVersionStats(version.datasetId());
  }

  @Transactional
  public DatasetVersionDto coverDraft(String datasetId, String sourceVersionId) {
    DatasetVersionDto source = getVersion(sourceVersionId);
    if (source.draft()) {
      throw new IllegalArgumentException("不能用草稿覆盖草稿");
    }
    String draftVersionId = getDraftVersionId(datasetId);
    clearVersionContent(draftVersionId);
    copyVersionContent(sourceVersionId, draftVersionId);
    Integer itemCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM eval_dataset_item WHERE version_id = ?",
        Integer.class,
        draftVersionId);
    jdbcTemplate.update(
        "UPDATE eval_dataset_version SET item_count = ?, updated_at = ? WHERE id = ?",
        itemCount == null ? 0 : itemCount, now(), draftVersionId);
    return getVersion(draftVersionId);
  }

  private void insertCells(String versionId, String itemId, Map<String, String> values) {
    List<FieldDto> fields = listFields(versionId);
    String now = now();
    Map<String, String> safeValues = values == null ? Map.of() : values;
    for (FieldDto field : fields) {
      jdbcTemplate.update("""
              INSERT INTO eval_dataset_item_cell
              (id, version_id, item_id, field_id, cell_value, created_at, updated_at)
              VALUES (?, ?, ?, ?, ?, ?, ?)
              """,
          id(), versionId, itemId, field.id(), safeValues.getOrDefault(field.id(), ""), now, now);
    }
  }

  private RowDto getRow(String itemId) {
    RowShell row = jdbcTemplate.queryForObject("""
            SELECT id, row_no, created_at, updated_at
            FROM eval_dataset_item
            WHERE id = ?
            """,
        (rs, rowNum) -> new RowShell(
            rs.getString("id"),
            rs.getInt("row_no"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        ),
        itemId);
    Map<String, String> values = loadValues(List.of(itemId)).getOrDefault(itemId, Map.of());
    return new RowDto(row.id(), row.rowNo(), values, row.createdAt(), row.updatedAt());
  }

  private Map<String, Map<String, String>> loadValues(List<String> itemIds) {
    if (itemIds.isEmpty()) {
      return Map.of();
    }
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("itemIds", itemIds);
    Map<String, Map<String, String>> values = new LinkedHashMap<>();
    namedJdbcTemplate.query("""
            SELECT item_id, field_id, cell_value
            FROM eval_dataset_item_cell
            WHERE item_id IN (:itemIds)
            """,
        params,
        rs -> {
          values.computeIfAbsent(rs.getString("item_id"), ignored -> new LinkedHashMap<>())
              .put(rs.getString("field_id"), rs.getString("cell_value"));
        });
    return values;
  }

  private void copyVersionContent(String sourceVersionId, String targetVersionId) {
    Map<String, String> fieldIdMap = new LinkedHashMap<>();
    for (FieldDto field : listFields(sourceVersionId)) {
      String newFieldId = id();
      fieldIdMap.put(field.id(), newFieldId);
      String now = now();
      jdbcTemplate.update("""
              INSERT INTO eval_dataset_field
              (id, version_id, field_name, field_type, is_required, description, display_order, created_at, updated_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
              """,
          newFieldId, targetVersionId, field.fieldName(), field.fieldType(), bool(field.required()),
          field.description(), field.displayOrder(), now, now);
    }

    List<RowShell> sourceRows = jdbcTemplate.query("""
            SELECT id, row_no, created_at, updated_at
            FROM eval_dataset_item
            WHERE version_id = ?
            ORDER BY row_no ASC
            """,
        (rs, rowNum) -> new RowShell(
            rs.getString("id"),
            rs.getInt("row_no"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        ),
        sourceVersionId);
    Map<String, Map<String, String>> sourceValues = loadValues(sourceRows.stream().map(RowShell::id).toList());
    for (RowShell sourceRow : sourceRows) {
      String newItemId = id();
      String now = now();
      jdbcTemplate.update("""
              INSERT INTO eval_dataset_item
              (id, version_id, row_no, created_at, updated_at)
              VALUES (?, ?, ?, ?, ?)
              """,
          newItemId, targetVersionId, sourceRow.rowNo(), now, now);
      Map<String, String> rowValues = sourceValues.getOrDefault(sourceRow.id(), Map.of());
      for (Map.Entry<String, String> entry : rowValues.entrySet()) {
        String targetFieldId = fieldIdMap.get(entry.getKey());
        if (targetFieldId != null) {
          jdbcTemplate.update("""
                  INSERT INTO eval_dataset_item_cell
                  (id, version_id, item_id, field_id, cell_value, created_at, updated_at)
                  VALUES (?, ?, ?, ?, ?, ?, ?)
                  """,
              id(), targetVersionId, newItemId, targetFieldId, entry.getValue(), now, now);
        }
      }
    }
  }

  private void clearVersionContent(String versionId) {
    jdbcTemplate.update("DELETE FROM eval_dataset_item_cell WHERE version_id = ?", versionId);
    jdbcTemplate.update("DELETE FROM eval_dataset_item WHERE version_id = ?", versionId);
    jdbcTemplate.update("DELETE FROM eval_dataset_field WHERE version_id = ?", versionId);
  }

  private void addBlankCellsForNewField(String versionId, String fieldId) {
    List<String> itemIds = jdbcTemplate.queryForList(
        "SELECT id FROM eval_dataset_item WHERE version_id = ?",
        String.class,
        versionId);
    String now = now();
    for (String itemId : itemIds) {
      jdbcTemplate.update("""
              INSERT INTO eval_dataset_item_cell
              (id, version_id, item_id, field_id, cell_value, created_at, updated_at)
              VALUES (?, ?, ?, ?, '', ?, ?)
              """,
          id(), versionId, itemId, fieldId, now, now);
    }
  }

  private DatasetVersionDto getVersion(String versionId) {
    return jdbcTemplate.queryForObject("""
            SELECT id, dataset_id, version_no, item_count, created_at, updated_at
            FROM eval_dataset_version
            WHERE id = ? AND is_deleted = 0
            """,
        (rs, rowNum) -> toVersionDto(
            rs.getString("id"),
            rs.getString("dataset_id"),
            rs.getInt("version_no"),
            rs.getInt("item_count"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        ),
        versionId);
  }

  private String getDraftVersionId(String datasetId) {
    return jdbcTemplate.queryForObject("""
            SELECT id
            FROM eval_dataset_version
            WHERE dataset_id = ? AND version_no = 0 AND is_deleted = 0
            """,
        String.class,
        datasetId);
  }

  private void ensureDraft(String versionId) {
    Integer versionNo = jdbcTemplate.queryForObject(
        "SELECT version_no FROM eval_dataset_version WHERE id = ? AND is_deleted = 0",
        Integer.class,
        versionId);
    if (versionNo == null || versionNo != 0) {
      throw new IllegalArgumentException("只有草稿版本允许修改");
    }
  }

  private void updateItemCount(String versionId) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM eval_dataset_item WHERE version_id = ?",
        Integer.class,
        versionId);
    jdbcTemplate.update(
        "UPDATE eval_dataset_version SET item_count = ?, updated_at = ? WHERE id = ?",
        count == null ? 0 : count, now(), versionId);
  }

  private void touchVersion(String versionId) {
    String now = now();
    jdbcTemplate.update("UPDATE eval_dataset_version SET updated_at = ? WHERE id = ?", now, versionId);
    String datasetId = jdbcTemplate.queryForObject(
        "SELECT dataset_id FROM eval_dataset_version WHERE id = ?",
        String.class,
        versionId);
    jdbcTemplate.update("UPDATE eval_dataset SET updated_at = ? WHERE id = ?", now, datasetId);
  }

  private void refreshDatasetVersionStats(String datasetId) {
    Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM eval_dataset_version
            WHERE dataset_id = ? AND version_no > 0 AND is_deleted = 0
            """,
        Integer.class,
        datasetId);
    List<String> latestIds = jdbcTemplate.queryForList("""
            SELECT id
            FROM eval_dataset_version
            WHERE dataset_id = ? AND version_no > 0 AND is_deleted = 0
            ORDER BY version_no DESC
            LIMIT 1
            """,
        String.class,
        datasetId);
    jdbcTemplate.update("""
            UPDATE eval_dataset
            SET published_version_count = ?, latest_published_version_id = ?, updated_at = ?
            WHERE id = ?
            """,
        count == null ? 0 : count, latestIds.isEmpty() ? null : latestIds.get(0), now(), datasetId);
  }

  private DatasetVersionDto toVersionDto(String id, String datasetId, Integer versionNo, Integer itemCount, String createdAt, String updatedAt) {
    return new DatasetVersionDto(
        id,
        datasetId,
        versionNo,
        versionNo == 0 ? "草稿" : "V" + versionNo,
        itemCount,
        versionNo == 0,
        createdAt,
        updatedAt);
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

  private record RowShell(String id, Integer rowNo, String createdAt, String updatedAt) {
  }
}
