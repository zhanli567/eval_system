package com.evalsystem.task.mapper;

import com.evalsystem.task.dto.TaskBase;
import com.evalsystem.task.dto.TaskEvaluatorDimension;
import com.evalsystem.task.dto.TaskEvaluatorResultDto;
import com.evalsystem.task.dto.TaskTagDimension;
import com.evalsystem.task.dto.TaskTagResultDto;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TaskMapper {
  List<TaskBase> listTaskBases(
      @Param("status") String status,
      @Param("like") String like,
      @Param("orderColumn") String orderColumn,
      @Param("orderDirection") String orderDirection,
      @Param("size") int size,
      @Param("offset") int offset
  );

  long countTaskBases(@Param("status") String status, @Param("like") String like);

  TaskBase findTaskBase(@Param("taskId") String taskId);

  List<TaskEvaluatorDimension> listEvaluatorDimensions(@Param("taskId") String taskId);

  List<TaskTagDimension> listTagDimensions(@Param("taskId") String taskId);

  List<TaskItemRecord> listTaskItems(@Param("taskId") String taskId, @Param("size") int size, @Param("offset") int offset);

  List<TaskItemRecord> listAllTaskItems(@Param("taskId") String taskId);

  @Select("SELECT COUNT(*) FROM eval_task_item WHERE task_id = #{taskId}")
  long countTaskItems(@Param("taskId") String taskId);

  List<TaskEvaluatorResultDto> listEvaluatorResultsByTaskItemIds(@Param("taskItemIds") List<String> taskItemIds);

  List<TaskTagResultDto> listTagResultsByTaskItemIds(@Param("taskItemIds") List<String> taskItemIds);

  List<TaskAppFieldMappingRecord> listAppFieldMappings(@Param("taskId") String taskId);

  @Insert("""
      INSERT INTO eval_task
      (id, task_name, status, description, dataset_id, dataset_version_id, item_count, app_type, app_id, app_version_id,
       started_at, finished_at, is_deleted, created_at, updated_at)
      VALUES
      (#{taskId}, #{taskName}, #{status}, #{description}, #{datasetId}, #{datasetVersionId}, #{itemCount}, #{appType}, #{appId}, #{appVersionId},
       '', '', 0, #{now}, #{now})
      """)
  void insertTask(
      @Param("taskId") String taskId,
      @Param("taskName") String taskName,
      @Param("status") String status,
      @Param("description") String description,
      @Param("datasetId") String datasetId,
      @Param("datasetVersionId") String datasetVersionId,
      @Param("itemCount") int itemCount,
      @Param("appType") String appType,
      @Param("appId") String appId,
      @Param("appVersionId") String appVersionId,
      @Param("now") String now
  );

  @Insert("""
      INSERT INTO eval_task_app_field_mapping
      (id, task_id, app_input_id, app_input_name, app_input_type, dataset_version_id, dataset_field_id, display_order, created_at, updated_at)
      VALUES
      (#{id}, #{taskId}, #{appInputId}, #{appInputName}, #{appInputType}, #{datasetVersionId}, #{datasetFieldId}, #{displayOrder}, #{now}, #{now})
      """)
  void insertAppFieldMapping(
      @Param("id") String id,
      @Param("taskId") String taskId,
      @Param("appInputId") String appInputId,
      @Param("appInputName") String appInputName,
      @Param("appInputType") String appInputType,
      @Param("datasetVersionId") String datasetVersionId,
      @Param("datasetFieldId") String datasetFieldId,
      @Param("displayOrder") int displayOrder,
      @Param("now") String now
  );

  @Insert("""
      INSERT INTO eval_task_evaluator
      (id, task_id, evaluator_source, evaluator_id, evaluator_version_id, status, display_order, created_at, updated_at)
      VALUES
      (#{id}, #{taskId}, #{evaluatorSource}, #{evaluatorId}, #{evaluatorVersionId}, #{status}, #{displayOrder}, #{now}, #{now})
      """)
  void insertTaskEvaluator(
      @Param("id") String id,
      @Param("taskId") String taskId,
      @Param("evaluatorSource") String evaluatorSource,
      @Param("evaluatorId") String evaluatorId,
      @Param("evaluatorVersionId") String evaluatorVersionId,
      @Param("status") String status,
      @Param("displayOrder") int displayOrder,
      @Param("now") String now
  );

  @Insert("""
      INSERT INTO eval_task_evaluator_param_mapping
      (id, task_id, task_evaluator_id, param_id, param_name, source_type, dataset_version_id, dataset_field_id, app_output_name,
       display_order, created_at, updated_at)
      VALUES
      (#{id}, #{taskId}, #{taskEvaluatorId}, #{paramId}, #{paramName}, #{sourceType}, #{datasetVersionId}, #{datasetFieldId}, #{appOutputName},
       #{displayOrder}, #{now}, #{now})
      """)
  void insertParamMapping(
      @Param("id") String id,
      @Param("taskId") String taskId,
      @Param("taskEvaluatorId") String taskEvaluatorId,
      @Param("paramId") String paramId,
      @Param("paramName") String paramName,
      @Param("sourceType") String sourceType,
      @Param("datasetVersionId") String datasetVersionId,
      @Param("datasetFieldId") String datasetFieldId,
      @Param("appOutputName") String appOutputName,
      @Param("displayOrder") int displayOrder,
      @Param("now") String now
  );

  @Insert("""
      INSERT INTO eval_task_tag
      (id, task_id, tag_id, status, display_order, created_at, updated_at)
      VALUES (#{id}, #{taskId}, #{tagId}, #{status}, #{displayOrder}, #{now}, #{now})
      """)
  void insertTaskTag(
      @Param("id") String id,
      @Param("taskId") String taskId,
      @Param("tagId") String tagId,
      @Param("status") String status,
      @Param("displayOrder") int displayOrder,
      @Param("now") String now
  );

  @Insert("""
      INSERT INTO eval_task_item
      (id, task_id, dataset_version_id, dataset_item_id, row_no, status, app_output, app_output_status, app_error_message,
       started_at, finished_at, created_at, updated_at)
      VALUES
      (#{id}, #{taskId}, #{datasetVersionId}, #{datasetItemId}, #{rowNo}, #{status}, '', #{appOutputStatus}, '',
       '', '', #{now}, #{now})
      """)
  void insertTaskItem(
      @Param("id") String id,
      @Param("taskId") String taskId,
      @Param("datasetVersionId") String datasetVersionId,
      @Param("datasetItemId") String datasetItemId,
      @Param("rowNo") int rowNo,
      @Param("status") String status,
      @Param("appOutputStatus") String appOutputStatus,
      @Param("now") String now
  );

  @Insert("""
      INSERT INTO eval_task_evaluator_result
      (id, task_id, task_item_id, task_evaluator_id, status, score, pass_result, result_value, error_message,
       started_at, finished_at, created_at, updated_at)
      VALUES
      (#{id}, #{taskId}, #{taskItemId}, #{taskEvaluatorId}, #{status}, NULL, '', '', '',
       '', '', #{now}, #{now})
      """)
  void insertEvaluatorResult(
      @Param("id") String id,
      @Param("taskId") String taskId,
      @Param("taskItemId") String taskItemId,
      @Param("taskEvaluatorId") String taskEvaluatorId,
      @Param("status") String status,
      @Param("now") String now
  );

  @Insert("""
      INSERT INTO eval_task_tag_result
      (id, task_id, task_item_id, task_tag_id, status, value_text, value_number, tag_option_id, pass_result,
       annotator_id, annotator_name, annotated_at, created_at, updated_at)
      VALUES
      (#{id}, #{taskId}, #{taskItemId}, #{taskTagId}, #{status}, '', NULL, '', '',
       '', '', '', #{now}, #{now})
      """)
  void insertTagResult(
      @Param("id") String id,
      @Param("taskId") String taskId,
      @Param("taskItemId") String taskItemId,
      @Param("taskTagId") String taskTagId,
      @Param("status") String status,
      @Param("now") String now
  );

  @Update("UPDATE eval_task SET is_deleted = 1, updated_at = #{now} WHERE id = #{taskId}")
  void softDeleteTask(@Param("taskId") String taskId, @Param("now") String now);

  @Update("""
      UPDATE eval_task
      SET status = #{status},
          started_at = CASE WHEN #{startedAt} IS NULL THEN started_at ELSE #{startedAt} END,
          finished_at = CASE WHEN #{finishedAt} IS NULL THEN finished_at ELSE #{finishedAt} END,
          updated_at = #{now}
      WHERE id = #{taskId} AND is_deleted = 0
      """)
  void updateTaskStatus(
      @Param("taskId") String taskId,
      @Param("status") String status,
      @Param("startedAt") String startedAt,
      @Param("finishedAt") String finishedAt,
      @Param("now") String now
  );

  @Update("UPDATE eval_task_evaluator SET status = #{status}, updated_at = #{now} WHERE id = #{taskEvaluatorId}")
  void updateTaskEvaluatorStatus(@Param("taskEvaluatorId") String taskEvaluatorId, @Param("status") String status, @Param("now") String now);

  @Update("UPDATE eval_task_tag SET status = #{status}, updated_at = #{now} WHERE id = #{taskTagId}")
  void updateTaskTagStatus(@Param("taskTagId") String taskTagId, @Param("status") String status, @Param("now") String now);

  @Update("""
      UPDATE eval_task_item
      SET status = #{status},
          app_output = #{appOutput},
          app_output_status = #{appOutputStatus},
          app_error_message = #{appErrorMessage},
          started_at = #{startedAt},
          finished_at = #{finishedAt},
          updated_at = #{now}
      WHERE id = #{taskItemId}
      """)
  void updateTaskItemRunResult(
      @Param("taskItemId") String taskItemId,
      @Param("status") String status,
      @Param("appOutput") String appOutput,
      @Param("appOutputStatus") String appOutputStatus,
      @Param("appErrorMessage") String appErrorMessage,
      @Param("startedAt") String startedAt,
      @Param("finishedAt") String finishedAt,
      @Param("now") String now
  );

  @Update("""
      UPDATE eval_task_item
      SET app_output = #{appOutput},
          app_output_status = #{appOutputStatus},
          app_error_message = #{appErrorMessage},
          updated_at = #{now}
      WHERE id = #{taskItemId}
      """)
  void updateTaskItemAppResult(
      @Param("taskItemId") String taskItemId,
      @Param("appOutput") String appOutput,
      @Param("appOutputStatus") String appOutputStatus,
      @Param("appErrorMessage") String appErrorMessage,
      @Param("now") String now
  );

  @Update("UPDATE eval_task_item SET status = #{status}, updated_at = #{now} WHERE id = #{taskItemId}")
  void updateTaskItemStatus(@Param("taskItemId") String taskItemId, @Param("status") String status, @Param("now") String now);

  @Update("""
      UPDATE eval_task_evaluator_result
      SET status = #{status},
          score = #{score},
          pass_result = #{passResult},
          result_value = #{resultValue},
          error_message = #{errorMessage},
          started_at = #{startedAt},
          finished_at = #{finishedAt},
          updated_at = #{now}
      WHERE task_item_id = #{taskItemId} AND task_evaluator_id = #{taskEvaluatorId}
      """)
  void updateEvaluatorResult(
      @Param("taskItemId") String taskItemId,
      @Param("taskEvaluatorId") String taskEvaluatorId,
      @Param("status") String status,
      @Param("score") BigDecimal score,
      @Param("passResult") String passResult,
      @Param("resultValue") String resultValue,
      @Param("errorMessage") String errorMessage,
      @Param("startedAt") String startedAt,
      @Param("finishedAt") String finishedAt,
      @Param("now") String now
  );

  @Update("""
      UPDATE eval_task_tag_result
      SET status = #{status},
          value_text = #{valueText},
          value_number = #{valueNumber},
          tag_option_id = #{tagOptionId},
          pass_result = #{passResult},
          annotator_id = #{annotatorId},
          annotator_name = #{annotatorName},
          annotated_at = #{annotatedAt},
          updated_at = #{now}
      WHERE task_item_id = #{taskItemId} AND task_tag_id = #{taskTagId}
      """)
  void updateTagResult(
      @Param("taskItemId") String taskItemId,
      @Param("taskTagId") String taskTagId,
      @Param("status") String status,
      @Param("valueText") String valueText,
      @Param("valueNumber") BigDecimal valueNumber,
      @Param("tagOptionId") String tagOptionId,
      @Param("passResult") String passResult,
      @Param("annotatorId") String annotatorId,
      @Param("annotatorName") String annotatorName,
      @Param("annotatedAt") String annotatedAt,
      @Param("now") String now
  );

  List<TaskEvaluatorBindingRecord> listTaskEvaluatorBindings(@Param("taskId") String taskId);

  List<TaskEvaluatorParamMappingRecord> listParamMappings(@Param("taskEvaluatorId") String taskEvaluatorId);

  List<TaskEvaluatorParamMappingRecord> listAllParamMappings(@Param("taskId") String taskId);

  List<TaskTagBindingRecord> listTaskTagBindings(@Param("taskId") String taskId);

  TaskItemRecord findTaskItem(@Param("taskItemId") String taskItemId);

  @Select("""
      SELECT id
      FROM eval_task_item
      WHERE task_id = #{taskId} AND row_no < #{rowNo}
      ORDER BY row_no DESC
      LIMIT 1
      """)
  String findPreviousTaskItemId(@Param("taskId") String taskId, @Param("rowNo") int rowNo);

  @Select("""
      SELECT id
      FROM eval_task_item
      WHERE task_id = #{taskId} AND row_no > #{rowNo}
      ORDER BY row_no ASC
      LIMIT 1
      """)
  String findNextTaskItemId(@Param("taskId") String taskId, @Param("rowNo") int rowNo);

  @Select("SELECT COUNT(*) FROM eval_task_tag_result WHERE task_tag_id = #{taskTagId}")
  int countTagResults(@Param("taskTagId") String taskTagId);

  @Select("SELECT COUNT(*) FROM eval_task_tag_result WHERE task_tag_id = #{taskTagId} AND status = 'completed'")
  int countCompletedTagResults(@Param("taskTagId") String taskTagId);

  @Select("""
      SELECT COUNT(*)
      FROM eval_task_tag_result
      WHERE task_item_id = #{taskItemId} AND status <> 'completed'
      """)
  int countUnfinishedTagResultsByItem(@Param("taskItemId") String taskItemId);

  @Select("""
      SELECT COUNT(*)
      FROM eval_task_evaluator_result
      WHERE task_item_id = #{taskItemId} AND status NOT IN ('completed', 'skipped')
      """)
  int countUnfinishedEvaluatorResultsByItem(@Param("taskItemId") String taskItemId);

  @Select("SELECT COUNT(*) FROM eval_task_item WHERE task_id = #{taskId} AND status <> 'completed'")
  int countUnfinishedTaskItems(@Param("taskId") String taskId);

  @Select("""
      SELECT COUNT(*)
      FROM eval_task_tag_result r
      JOIN eval_task_item i ON r.task_item_id = i.id
      WHERE i.task_id = #{taskId} AND r.status <> 'completed'
      """)
  int countUnfinishedTagResultsByTask(@Param("taskId") String taskId);

  @Select("""
      SELECT COUNT(*)
      FROM eval_task_evaluator_result r
      JOIN eval_task_item i ON r.task_item_id = i.id
      WHERE i.task_id = #{taskId} AND r.status NOT IN ('completed', 'skipped')
      """)
  int countUnfinishedEvaluatorResultsByTask(@Param("taskId") String taskId);

  record TaskItemRecord(
      String id,
      String taskId,
      String datasetVersionId,
      String datasetItemId,
      Integer rowNo,
      String status,
      String appOutput,
      String appOutputStatus,
      String appErrorMessage,
      String createdAt,
      String updatedAt
  ) {
  }

  record TaskAppFieldMappingRecord(
      String id,
      String taskId,
      String appInputId,
      String appInputName,
      String appInputType,
      String datasetVersionId,
      String datasetFieldId,
      Integer displayOrder
  ) {
  }

  record TaskEvaluatorBindingRecord(
      String id,
      String taskId,
      String evaluatorSource,
      String evaluatorId,
      String evaluatorVersionId,
      String status,
      Integer displayOrder
  ) {
  }

  record TaskEvaluatorParamMappingRecord(
      String id,
      String taskId,
      String taskEvaluatorId,
      String paramId,
      String paramName,
      String sourceType,
      String datasetVersionId,
      String datasetFieldId,
      String appOutputName,
      Integer displayOrder
  ) {
  }

  record TaskTagBindingRecord(
      String id,
      String taskId,
      String tagId,
      String tagName,
      String tagType,
      String description,
      Integer minValue,
      Integer maxValue,
      Integer passThreshold,
      String status,
      Integer displayOrder
  ) {
  }
}
