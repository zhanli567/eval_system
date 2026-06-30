package com.agentnexus.backend.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.agentnexus.backend.task.api.dto.response.TaskBase;
import com.agentnexus.backend.task.api.dto.response.TaskEvaluatorDimension;
import com.agentnexus.backend.task.api.dto.response.TaskEvaluatorResultDto;
import com.agentnexus.backend.task.api.dto.response.TaskTagDimension;
import com.agentnexus.backend.task.api.dto.response.TaskTagResultDto;
import com.agentnexus.backend.task.entity.EvalTask;
import com.agentnexus.backend.task.repository.TaskTagBindingRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TaskMapper extends BaseMapper<EvalTask> {
  List<TaskBase> listTaskBases(
      @Param("spaceId") String spaceId,
      @Param("status") String status,
      @Param("like") String like,
      @Param("orderColumn") String orderColumn,
      @Param("orderDirection") String orderDirection,
      @Param("size") int size,
      @Param("offset") int offset
  );

  TaskBase findTaskBase(@Param("spaceId") String spaceId, @Param("taskId") String taskId);

  List<TaskEvaluatorDimension> listEvaluatorDimensions(@Param("spaceId") String spaceId, @Param("taskId") String taskId);

  List<TaskTagDimension> listTagDimensions(@Param("spaceId") String spaceId, @Param("taskId") String taskId);

  List<TaskEvaluatorResultDto> listEvaluatorResultsByTaskItemIds(
      @Param("spaceId") String spaceId,
      @Param("taskItemIds") List<String> taskItemIds
  );

  List<TaskTagResultDto> listTagResultsByTaskItemIds(
      @Param("spaceId") String spaceId,
      @Param("taskItemIds") List<String> taskItemIds
  );

  List<TaskTagBindingRecord> listTaskTagBindings(@Param("spaceId") String spaceId, @Param("taskId") String taskId);
}
