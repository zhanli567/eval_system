package com.agentnexus.backend.evaluator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.agentnexus.backend.evaluator.entity.EvalEvaluatorVersion;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface EvaluatorVersionMapper extends BaseMapper<EvalEvaluatorVersion> {
  @Select("""
      SELECT COUNT(*)
      FROM t_eval_task_evaluator
      WHERE space_id = #{spaceId}
        AND evaluator_version_id = #{versionId}
      """)
  long countTaskBindings(@Param("spaceId") String spaceId, @Param("versionId") String versionId);
}
