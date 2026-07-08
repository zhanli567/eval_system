package com.agentnexus.backend.evaluator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorConfigBase;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorSummary;
import com.agentnexus.backend.evaluator.entity.EvalEvaluator;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface EvaluatorMapper extends BaseMapper<EvalEvaluator> {
  List<EvaluatorSummary> listEvaluators(
      @Param("spaceId") String spaceId,
      @Param("evaluatorType") String evaluatorType,
      @Param("like") String like,
      @Param("orderColumn") String orderColumn,
      @Param("orderDirection") String orderDirection,
      @Param("size") int size,
      @Param("offset") int offset
  );

  EvaluatorConfigBase findVersionConfig(@Param("spaceId") String spaceId, @Param("versionId") String versionId);
}
