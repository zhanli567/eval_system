package com.evalsystem.evaluator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evalsystem.evaluator.api.dto.response.EvaluatorConfigBase;
import com.evalsystem.evaluator.api.dto.response.EvaluatorSummary;
import com.evalsystem.evaluator.entity.EvalEvaluator;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface EvaluatorMapper extends BaseMapper<EvalEvaluator> {
  List<EvaluatorSummary> listEvaluators(
      @Param("evaluatorType") String evaluatorType,
      @Param("like") String like,
      @Param("size") int size,
      @Param("offset") int offset
  );

  EvaluatorConfigBase findVersionConfig(@Param("versionId") String versionId);
}
