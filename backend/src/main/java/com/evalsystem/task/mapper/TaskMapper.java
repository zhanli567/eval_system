package com.evalsystem.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evalsystem.task.api.dto.response.TaskBase;
import com.evalsystem.task.api.dto.response.TaskEvaluatorDimension;
import com.evalsystem.task.api.dto.response.TaskEvaluatorResultDto;
import com.evalsystem.task.api.dto.response.TaskTagDimension;
import com.evalsystem.task.api.dto.response.TaskTagResultDto;
import com.evalsystem.task.entity.EvalTask;
import com.evalsystem.task.repository.TaskTagBindingRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TaskMapper extends BaseMapper<EvalTask> {
  List<TaskBase> listTaskBases(
      @Param("status") String status,
      @Param("like") String like,
      @Param("orderColumn") String orderColumn,
      @Param("orderDirection") String orderDirection,
      @Param("size") int size,
      @Param("offset") int offset
  );

  TaskBase findTaskBase(@Param("taskId") String taskId);

  List<TaskEvaluatorDimension> listEvaluatorDimensions(@Param("taskId") String taskId);

  List<TaskTagDimension> listTagDimensions(@Param("taskId") String taskId);

  List<TaskEvaluatorResultDto> listEvaluatorResultsByTaskItemIds(@Param("taskItemIds") List<String> taskItemIds);

  List<TaskTagResultDto> listTagResultsByTaskItemIds(@Param("taskItemIds") List<String> taskItemIds);

  List<TaskTagBindingRecord> listTaskTagBindings(@Param("taskId") String taskId);
}
