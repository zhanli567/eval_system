package com.evalsystem.evaluator.mapper;

import com.evalsystem.evaluator.dto.EvaluatorConfigBase;
import com.evalsystem.evaluator.dto.EvaluatorParamDto;
import com.evalsystem.evaluator.dto.EvaluatorSummary;
import com.evalsystem.evaluator.dto.EvaluatorVersionDto;
import com.evalsystem.evaluator.dto.PresetCategoryDto;
import com.evalsystem.evaluator.dto.PresetEvaluatorConfig;
import com.evalsystem.evaluator.dto.PresetEvaluatorSummary;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface EvaluatorMapper {
  List<EvaluatorSummary> listEvaluators(
      @Param("evaluatorType") String evaluatorType,
      @Param("like") String like,
      @Param("size") int size,
      @Param("offset") int offset
  );

  long countEvaluators(@Param("evaluatorType") String evaluatorType, @Param("like") String like);

  List<PresetCategoryDto> listPresetCategories();

  List<PresetEvaluatorSummary> listPresetEvaluators(
      @Param("categoryId") String categoryId,
      @Param("like") String like,
      @Param("size") int size,
      @Param("offset") int offset
  );

  long countPresetEvaluators(@Param("categoryId") String categoryId, @Param("like") String like);

  PresetEvaluatorConfig findPresetConfig(@Param("presetId") String presetId);

  List<EvaluatorVersionDto> listVersions(@Param("evaluatorId") String evaluatorId);

  EvaluatorConfigBase findVersionConfig(@Param("versionId") String versionId);

  @Insert("""
      INSERT INTO eval_evaluator
      (id, evaluator_name, evaluator_type, description, latest_version_id, is_deleted, created_at, updated_at)
      VALUES (#{evaluatorId}, #{evaluatorName}, #{evaluatorType}, #{description}, #{latestVersionId}, 0, #{now}, #{now})
      """)
  void insertEvaluator(
      @Param("evaluatorId") String evaluatorId,
      @Param("evaluatorName") String evaluatorName,
      @Param("evaluatorType") String evaluatorType,
      @Param("description") String description,
      @Param("latestVersionId") String latestVersionId,
      @Param("now") String now
  );

  @Update("""
      UPDATE eval_evaluator
      SET evaluator_name = #{evaluatorName},
          description = #{description},
          updated_at = #{now}
      WHERE id = #{evaluatorId} AND is_deleted = 0
      """)
  void updateEvaluatorBase(
      @Param("evaluatorId") String evaluatorId,
      @Param("evaluatorName") String evaluatorName,
      @Param("description") String description,
      @Param("now") String now
  );

  @Update("""
      UPDATE eval_evaluator
      SET latest_version_id = #{versionId}, updated_at = #{now}
      WHERE id = #{evaluatorId} AND is_deleted = 0
      """)
  void updateLatestVersion(@Param("evaluatorId") String evaluatorId, @Param("versionId") String versionId, @Param("now") String now);

  @Update("UPDATE eval_evaluator SET is_deleted = 1, updated_at = #{now} WHERE id = #{evaluatorId}")
  void softDeleteEvaluator(@Param("evaluatorId") String evaluatorId, @Param("now") String now);

  @Update("UPDATE eval_evaluator_version SET is_deleted = 1, updated_at = #{now} WHERE evaluator_id = #{evaluatorId}")
  void softDeleteVersionsByEvaluator(@Param("evaluatorId") String evaluatorId, @Param("now") String now);

  @Insert("""
      INSERT INTO eval_evaluator_version
      (id, evaluator_id, version_no, model_id, prompt, execute_code, score_min, score_max, pass_threshold,
       is_deleted, created_at, updated_at)
      VALUES
      (#{versionId}, #{evaluatorId}, #{versionNo}, #{modelId}, #{prompt}, #{executeCode}, #{scoreMin}, #{scoreMax}, #{passThreshold},
       0, #{now}, #{now})
      """)
  void insertVersion(
      @Param("versionId") String versionId,
      @Param("evaluatorId") String evaluatorId,
      @Param("versionNo") int versionNo,
      @Param("modelId") String modelId,
      @Param("prompt") String prompt,
      @Param("executeCode") String executeCode,
      @Param("scoreMin") BigDecimal scoreMin,
      @Param("scoreMax") BigDecimal scoreMax,
      @Param("passThreshold") BigDecimal passThreshold,
      @Param("now") String now
  );

  @Update("""
      UPDATE eval_evaluator_version
      SET model_id = #{modelId},
          prompt = #{prompt},
          execute_code = #{executeCode},
          score_min = #{scoreMin},
          score_max = #{scoreMax},
          pass_threshold = #{passThreshold},
          updated_at = #{now}
      WHERE id = #{versionId} AND version_no = 0 AND is_deleted = 0
      """)
  void updateDraftVersion(
      @Param("versionId") String versionId,
      @Param("modelId") String modelId,
      @Param("prompt") String prompt,
      @Param("executeCode") String executeCode,
      @Param("scoreMin") BigDecimal scoreMin,
      @Param("scoreMax") BigDecimal scoreMax,
      @Param("passThreshold") BigDecimal passThreshold,
      @Param("now") String now
  );

  @Select("""
      SELECT id
      FROM eval_evaluator_version
      WHERE evaluator_id = #{evaluatorId} AND version_no = 0 AND is_deleted = 0
      LIMIT 1
      """)
  String findDraftVersionId(@Param("evaluatorId") String evaluatorId);

  @Select("""
      SELECT COALESCE(MAX(version_no), 0) + 1
      FROM eval_evaluator_version
      WHERE evaluator_id = #{evaluatorId} AND is_deleted = 0
      """)
  int nextVersionNo(@Param("evaluatorId") String evaluatorId);

  @Select("SELECT evaluator_type FROM eval_evaluator WHERE id = #{evaluatorId} AND is_deleted = 0")
  String findEvaluatorType(@Param("evaluatorId") String evaluatorId);

  @Select("SELECT latest_version_id FROM eval_evaluator WHERE id = #{evaluatorId} AND is_deleted = 0")
  String findLatestVersionId(@Param("evaluatorId") String evaluatorId);

  @Insert("""
      INSERT INTO eval_evaluator_param
      (id, target_type, target_id, param_name, data_type, default_value, display_order, created_at, updated_at)
      VALUES (#{paramId}, #{targetType}, #{targetId}, #{paramName}, #{dataType}, #{defaultValue}, #{displayOrder}, #{now}, #{now})
      """)
  void insertParam(
      @Param("paramId") String paramId,
      @Param("targetType") String targetType,
      @Param("targetId") String targetId,
      @Param("paramName") String paramName,
      @Param("dataType") String dataType,
      @Param("defaultValue") String defaultValue,
      @Param("displayOrder") int displayOrder,
      @Param("now") String now
  );

  @Delete("DELETE FROM eval_evaluator_param WHERE target_type = #{targetType} AND target_id = #{targetId}")
  void deleteParams(@Param("targetType") String targetType, @Param("targetId") String targetId);

  @Select("""
      SELECT id AS id,
             target_type AS targetType,
             target_id AS targetId,
             param_name AS paramName,
             data_type AS dataType,
             default_value AS defaultValue,
             display_order AS displayOrder
      FROM eval_evaluator_param
      WHERE target_type = #{targetType} AND target_id = #{targetId}
      ORDER BY display_order ASC
      """)
  List<EvaluatorParamDto> listParams(@Param("targetType") String targetType, @Param("targetId") String targetId);
}
