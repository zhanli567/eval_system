package com.evalsystem.dataset.mapper;

import com.evalsystem.dataset.dto.DatasetSummary;
import com.evalsystem.dataset.dto.DatasetVersionDto;
import com.evalsystem.dataset.dto.FieldDto;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface DatasetMapper {
  List<DatasetSummary> listDatasetSummaries(@Param("like") String like, @Param("size") int size, @Param("offset") int offset);

  long countDatasetSummaries(@Param("like") String like);

  @Insert("""
      INSERT INTO eval_dataset
      (id, name, description, published_version_count, latest_published_version_id, is_deleted, created_at, updated_at)
      VALUES (#{datasetId}, #{name}, #{description}, 0, NULL, 0, #{now}, #{now})
      """)
  void insertDataset(
      @Param("datasetId") String datasetId,
      @Param("name") String name,
      @Param("description") String description,
      @Param("now") String now
  );

  @Insert("""
      INSERT INTO eval_dataset_version
      (id, dataset_id, version_no, item_count, is_deleted, created_at, updated_at)
      VALUES (#{versionId}, #{datasetId}, #{versionNo}, #{itemCount}, 0, #{now}, #{now})
      """)
  void insertVersion(
      @Param("versionId") String versionId,
      @Param("datasetId") String datasetId,
      @Param("versionNo") int versionNo,
      @Param("itemCount") int itemCount,
      @Param("now") String now
  );

  DatasetSummary findDatasetSummary(@Param("datasetId") String datasetId);

  @Update("UPDATE eval_dataset SET is_deleted = 1, updated_at = #{now} WHERE id = #{datasetId}")
  void softDeleteDataset(@Param("datasetId") String datasetId, @Param("now") String now);

  List<DatasetVersionDto> listVersions(@Param("datasetId") String datasetId);

  DatasetVersionDto findVersion(@Param("versionId") String versionId);

  @Select("""
      SELECT id
      FROM eval_dataset_version
      WHERE dataset_id = #{datasetId} AND version_no = 0 AND is_deleted = 0
      """)
  String findDraftVersionId(@Param("datasetId") String datasetId);

  @ConstructorArgs({
      @Arg(column = "id", javaType = String.class),
      @Arg(column = "versionId", javaType = String.class),
      @Arg(column = "fieldName", javaType = String.class),
      @Arg(column = "fieldType", javaType = String.class),
      @Arg(column = "required", javaType = Boolean.class),
      @Arg(column = "description", javaType = String.class),
      @Arg(column = "displayOrder", javaType = Integer.class)
  })
  @Select("""
      SELECT id AS id,
             version_id AS versionId,
             field_name AS fieldName,
             field_type AS fieldType,
             is_required AS required,
             description AS description,
             display_order AS displayOrder
      FROM eval_dataset_field
      WHERE version_id = #{versionId}
      ORDER BY display_order ASC
      """)
  List<FieldDto> listFields(@Param("versionId") String versionId);

  @Select("SELECT id FROM eval_dataset_field WHERE version_id = #{versionId}")
  List<String> listFieldIds(@Param("versionId") String versionId);

  @Update("""
      UPDATE eval_dataset_field
      SET field_name = #{fieldName},
          field_type = #{fieldType},
          is_required = #{required},
          description = #{description},
          display_order = #{displayOrder},
          updated_at = #{now}
      WHERE id = #{fieldId}
      """)
  void updateField(
      @Param("fieldId") String fieldId,
      @Param("fieldName") String fieldName,
      @Param("fieldType") String fieldType,
      @Param("required") int required,
      @Param("description") String description,
      @Param("displayOrder") int displayOrder,
      @Param("now") String now
  );

  @Insert("""
      INSERT INTO eval_dataset_field
      (id, version_id, field_name, field_type, is_required, description, display_order, created_at, updated_at)
      VALUES (#{fieldId}, #{versionId}, #{fieldName}, #{fieldType}, #{required}, #{description}, #{displayOrder}, #{now}, #{now})
      """)
  void insertField(
      @Param("fieldId") String fieldId,
      @Param("versionId") String versionId,
      @Param("fieldName") String fieldName,
      @Param("fieldType") String fieldType,
      @Param("required") int required,
      @Param("description") String description,
      @Param("displayOrder") int displayOrder,
      @Param("now") String now
  );

  void deleteCellsByFieldIds(@Param("versionId") String versionId, @Param("fieldIds") List<String> fieldIds);

  void deleteFieldsByIds(@Param("versionId") String versionId, @Param("fieldIds") List<String> fieldIds);

  default void deleteFields(String versionId, List<String> fieldIds) {
    deleteCellsByFieldIds(versionId, fieldIds);
    deleteFieldsByIds(versionId, fieldIds);
  }

  List<RowRecord> searchRows(
      @Param("versionId") String versionId,
      @Param("fieldId") String fieldId,
      @Param("like") String like,
      @Param("size") int size,
      @Param("offset") int offset
  );

  long countSearchRows(@Param("versionId") String versionId, @Param("fieldId") String fieldId, @Param("like") String like);

  @ConstructorArgs({
      @Arg(column = "id", javaType = String.class),
      @Arg(column = "rowNo", javaType = Integer.class),
      @Arg(column = "createdAt", javaType = String.class),
      @Arg(column = "updatedAt", javaType = String.class)
  })
  @Select("""
      SELECT id AS id,
             row_no AS rowNo,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM eval_dataset_item
      WHERE version_id = #{versionId}
      ORDER BY row_no ASC
      LIMIT #{size} OFFSET #{offset}
      """)
  List<RowRecord> listRows(@Param("versionId") String versionId, @Param("size") int size, @Param("offset") int offset);

  @ConstructorArgs({
      @Arg(column = "id", javaType = String.class),
      @Arg(column = "rowNo", javaType = Integer.class),
      @Arg(column = "createdAt", javaType = String.class),
      @Arg(column = "updatedAt", javaType = String.class)
  })
  @Select("""
      SELECT id AS id,
             row_no AS rowNo,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM eval_dataset_item
      WHERE version_id = #{versionId}
      ORDER BY row_no ASC
      """)
  List<RowRecord> listAllRows(@Param("versionId") String versionId);

  @Select("SELECT COUNT(*) FROM eval_dataset_item WHERE version_id = #{versionId}")
  long countRows(@Param("versionId") String versionId);

  List<CellValueRecord> listCellValues(@Param("itemIds") List<String> itemIds);

  @Select("SELECT COALESCE(MAX(row_no), 0) + 1 FROM eval_dataset_item WHERE version_id = #{versionId}")
  int nextRowNo(@Param("versionId") String versionId);

  @Insert("""
      INSERT INTO eval_dataset_item (id, version_id, row_no, created_at, updated_at)
      VALUES (#{itemId}, #{versionId}, #{rowNo}, #{now}, #{now})
      """)
  void insertItem(@Param("itemId") String itemId, @Param("versionId") String versionId, @Param("rowNo") int rowNo, @Param("now") String now);

  @Insert("""
      INSERT INTO eval_dataset_item_cell
      (id, version_id, item_id, field_id, cell_value, created_at, updated_at)
      VALUES (#{cellId}, #{versionId}, #{itemId}, #{fieldId}, #{cellValue}, #{now}, #{now})
      """)
  void insertCell(
      @Param("cellId") String cellId,
      @Param("versionId") String versionId,
      @Param("itemId") String itemId,
      @Param("fieldId") String fieldId,
      @Param("cellValue") String cellValue,
      @Param("now") String now
  );

  @Update("UPDATE eval_dataset_item SET updated_at = #{now} WHERE id = #{itemId} AND version_id = #{versionId}")
  void updateItem(@Param("itemId") String itemId, @Param("versionId") String versionId, @Param("now") String now);

  @Delete("DELETE FROM eval_dataset_item_cell WHERE item_id = #{itemId}")
  void deleteCellsByItem(@Param("itemId") String itemId);

  @Delete("DELETE FROM eval_dataset_item WHERE id = #{itemId} AND version_id = #{versionId}")
  void deleteItem(@Param("itemId") String itemId, @Param("versionId") String versionId);

  @ConstructorArgs({
      @Arg(column = "id", javaType = String.class),
      @Arg(column = "rowNo", javaType = Integer.class),
      @Arg(column = "createdAt", javaType = String.class),
      @Arg(column = "updatedAt", javaType = String.class)
  })
  @Select("""
      SELECT id AS id,
             row_no AS rowNo,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM eval_dataset_item
      WHERE id = #{itemId}
      """)
  RowRecord findRow(@Param("itemId") String itemId);

  @Select("""
      SELECT COALESCE(MAX(version_no), 0) + 1
      FROM eval_dataset_version
      WHERE dataset_id = #{datasetId} AND is_deleted = 0
      """)
  int nextVersionNo(@Param("datasetId") String datasetId);

  @Select("SELECT item_count FROM eval_dataset_version WHERE id = #{versionId}")
  int findVersionItemCount(@Param("versionId") String versionId);

  @Update("UPDATE eval_dataset_version SET is_deleted = 1, updated_at = #{now} WHERE id = #{versionId}")
  void softDeleteVersion(@Param("versionId") String versionId, @Param("now") String now);

  @Delete("DELETE FROM eval_dataset_item_cell WHERE version_id = #{versionId}")
  void clearVersionCells(@Param("versionId") String versionId);

  @Delete("DELETE FROM eval_dataset_item WHERE version_id = #{versionId}")
  void clearVersionItems(@Param("versionId") String versionId);

  @Delete("DELETE FROM eval_dataset_field WHERE version_id = #{versionId}")
  void clearVersionFields(@Param("versionId") String versionId);

  default void clearVersionContent(String versionId) {
    clearVersionCells(versionId);
    clearVersionItems(versionId);
    clearVersionFields(versionId);
  }

  @Select("SELECT id FROM eval_dataset_item WHERE version_id = #{versionId}")
  List<String> listItemIds(@Param("versionId") String versionId);

  @Select("SELECT version_no FROM eval_dataset_version WHERE id = #{versionId} AND is_deleted = 0")
  Integer findVersionNo(@Param("versionId") String versionId);

  @Update("UPDATE eval_dataset_version SET item_count = #{itemCount}, updated_at = #{now} WHERE id = #{versionId}")
  void updateVersionItemCount(@Param("versionId") String versionId, @Param("itemCount") int itemCount, @Param("now") String now);

  @Update("UPDATE eval_dataset_version SET updated_at = #{now} WHERE id = #{versionId}")
  void touchVersion(@Param("versionId") String versionId, @Param("now") String now);

  @Select("SELECT dataset_id FROM eval_dataset_version WHERE id = #{versionId}")
  String findDatasetIdByVersionId(@Param("versionId") String versionId);

  @Update("UPDATE eval_dataset SET updated_at = #{now} WHERE id = #{datasetId}")
  void touchDataset(@Param("datasetId") String datasetId, @Param("now") String now);

  default void touchVersionAndDataset(String versionId, String now) {
    touchVersion(versionId, now);
    touchDataset(findDatasetIdByVersionId(versionId), now);
  }

  @Select("""
      SELECT COUNT(*)
      FROM eval_dataset_version
      WHERE dataset_id = #{datasetId} AND version_no > 0 AND is_deleted = 0
      """)
  int countPublishedVersions(@Param("datasetId") String datasetId);

  @Select("""
      SELECT id
      FROM eval_dataset_version
      WHERE dataset_id = #{datasetId} AND version_no > 0 AND is_deleted = 0
      ORDER BY version_no DESC
      LIMIT 1
      """)
  String findLatestPublishedVersionId(@Param("datasetId") String datasetId);

  @Update("""
      UPDATE eval_dataset
      SET published_version_count = #{count},
          latest_published_version_id = #{latestVersionId},
          updated_at = #{now}
      WHERE id = #{datasetId}
      """)
  void updateDatasetVersionStats(
      @Param("datasetId") String datasetId,
      @Param("count") int count,
      @Param("latestVersionId") String latestVersionId,
      @Param("now") String now
  );

  default void refreshDatasetVersionStats(String datasetId, String now) {
    updateDatasetVersionStats(datasetId, countPublishedVersions(datasetId), findLatestPublishedVersionId(datasetId), now);
  }

  default void refreshDatasetVersionStats(String datasetId) {
    refreshDatasetVersionStats(datasetId, String.valueOf(System.currentTimeMillis()));
  }

  default java.util.Map<String, java.util.Map<String, String>> loadValues(List<String> itemIds) {
    if (itemIds.isEmpty()) {
      return java.util.Map.of();
    }
    java.util.Map<String, java.util.Map<String, String>> values = new java.util.LinkedHashMap<>();
    for (CellValueRecord cell : listCellValues(itemIds)) {
      values.computeIfAbsent(cell.itemId(), ignored -> new java.util.LinkedHashMap<>())
          .put(cell.fieldId(), cell.cellValue());
    }
    return values;
  }

  record RowRecord(String id, Integer rowNo, String createdAt, String updatedAt) {
  }

  record CellValueRecord(String itemId, String fieldId, String cellValue) {
  }
}
