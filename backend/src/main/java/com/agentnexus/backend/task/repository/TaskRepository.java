package com.agentnexus.backend.task.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.agentnexus.backend.common.context.CurrentSpaceHolder;
import com.agentnexus.backend.common.context.CurrentUserHolder;
import com.agentnexus.backend.common.security.CurrentUser;
import com.agentnexus.backend.task.api.dto.response.TaskBase;
import com.agentnexus.backend.task.api.dto.response.TaskEvaluatorDimension;
import com.agentnexus.backend.task.api.dto.response.TaskEvaluatorResultDto;
import com.agentnexus.backend.task.api.dto.response.TaskTagDimension;
import com.agentnexus.backend.task.api.dto.response.TaskTagResultDto;
import com.agentnexus.backend.task.mapper.TaskAppFieldMappingMapper;
import com.agentnexus.backend.task.mapper.TaskEvaluatorMapper;
import com.agentnexus.backend.task.mapper.TaskEvaluatorParamMappingMapper;
import com.agentnexus.backend.task.mapper.TaskEvaluatorResultMapper;
import com.agentnexus.backend.task.mapper.TaskItemMapper;
import com.agentnexus.backend.task.mapper.TaskMapper;
import com.agentnexus.backend.task.mapper.TaskTagMapper;
import com.agentnexus.backend.task.mapper.TaskTagResultMapper;
import com.agentnexus.backend.task.entity.EvalTask;
import com.agentnexus.backend.task.entity.EvalTaskAppFieldMapping;
import com.agentnexus.backend.task.entity.EvalTaskEvaluator;
import com.agentnexus.backend.task.entity.EvalTaskEvaluatorParamMapping;
import com.agentnexus.backend.task.entity.EvalTaskEvaluatorResult;
import com.agentnexus.backend.task.entity.EvalTaskItem;
import com.agentnexus.backend.task.entity.EvalTaskTag;
import com.agentnexus.backend.task.entity.EvalTaskTagResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class TaskRepository {
  private final TaskMapper taskMapper;
  private final TaskAppFieldMappingMapper appFieldMappingMapper;
  private final TaskEvaluatorMapper taskEvaluatorMapper;
  private final TaskEvaluatorParamMappingMapper paramMappingMapper;
  private final TaskTagMapper taskTagMapper;
  private final TaskItemMapper taskItemMapper;
  private final TaskEvaluatorResultMapper evaluatorResultMapper;
  private final TaskTagResultMapper tagResultMapper;

  public TaskRepository(
      TaskMapper taskMapper,
      TaskAppFieldMappingMapper appFieldMappingMapper,
      TaskEvaluatorMapper taskEvaluatorMapper,
      TaskEvaluatorParamMappingMapper paramMappingMapper,
      TaskTagMapper taskTagMapper,
      TaskItemMapper taskItemMapper,
      TaskEvaluatorResultMapper evaluatorResultMapper,
      TaskTagResultMapper tagResultMapper
  ) {
    this.taskMapper = taskMapper;
    this.appFieldMappingMapper = appFieldMappingMapper;
    this.taskEvaluatorMapper = taskEvaluatorMapper;
    this.paramMappingMapper = paramMappingMapper;
    this.taskTagMapper = taskTagMapper;
    this.taskItemMapper = taskItemMapper;
    this.evaluatorResultMapper = evaluatorResultMapper;
    this.tagResultMapper = tagResultMapper;
  }

  public List<TaskBase> listTaskBases(
      String status,
      String like,
      String orderColumn,
      String orderDirection,
      int size,
      int offset
  ) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        taskMapper.listTaskBases(currentSpaceId(), status, like, orderColumn, orderDirection, size, offset));
  }

  public long countTaskBases(String status, String like) {
    return taskMapper.selectCount(taskQuery(status, like));
  }

  public boolean existsTaskName(String taskName) {
    return taskMapper.selectCount(new LambdaQueryWrapper<EvalTask>()
        .eq(EvalTask::getSpaceId, currentSpaceId())
        .eq(EvalTask::getTaskName, taskName)) > 0;
  }

  public TaskBase findTaskBase(String taskId) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () -> taskMapper.findTaskBase(currentSpaceId(), taskId));
  }

  public List<TaskEvaluatorDimension> listEvaluatorDimensions(String taskId) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        taskMapper.listEvaluatorDimensions(currentSpaceId(), taskId));
  }

  public List<TaskTagDimension> listTagDimensions(String taskId) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        taskMapper.listTagDimensions(currentSpaceId(), taskId));
  }

  public void insertTask(
      String taskId,
      String taskName,
      String status,
      String description,
      String datasetId,
      String datasetVersionId,
      int itemCount,
      String appType,
      String appId,
      String appVersionId,
      String appAgentAlias,
      String now
  ) {
    EvalTask task = new EvalTask();
    task.setId(taskId);
    task.setTaskName(taskName);
    task.setStatus(status);
    task.setDescription(description);
    task.setDatasetId(datasetId);
    task.setDatasetVersionId(datasetVersionId);
    task.setItemCount(itemCount);
    task.setAppType(appType);
    task.setAppId(appId);
    task.setAppVersionId(appVersionId);
    task.setAppAgentAlias(appAgentAlias);
    task.setStartedAt("");
    task.setFinishedAt("");
    task.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(task);
    taskMapper.insert(task);
  }

  public void insertAppFieldMapping(
      String id,
      String taskId,
      String appInputId,
      String appInputName,
      String appInputType,
      String datasetVersionId,
      String datasetFieldId,
      int displayOrder,
      String now
  ) {
    EvalTaskAppFieldMapping mapping = new EvalTaskAppFieldMapping();
    mapping.setId(id);
    mapping.setTaskId(taskId);
    mapping.setAppInputId(appInputId);
    mapping.setAppInputName(appInputName);
    mapping.setAppInputType(appInputType);
    mapping.setDatasetVersionId(datasetVersionId);
    mapping.setDatasetFieldId(datasetFieldId);
    mapping.setDisplayOrder(displayOrder);
    mapping.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(mapping);
    appFieldMappingMapper.insert(mapping);
  }

  public void insertTaskEvaluator(
      String id,
      String taskId,
      String evaluatorSource,
      String evaluatorId,
      String evaluatorVersionId,
      String modelId,
      String modelName,
      String status,
      int displayOrder,
      String now
  ) {
    EvalTaskEvaluator evaluator = new EvalTaskEvaluator();
    evaluator.setId(id);
    evaluator.setTaskId(taskId);
    evaluator.setEvaluatorSource(evaluatorSource);
    evaluator.setEvaluatorId(evaluatorId);
    evaluator.setEvaluatorVersionId(evaluatorVersionId);
    evaluator.setModelId(modelId);
    evaluator.setModelName(modelName);
    evaluator.setStatus(status);
    evaluator.setDisplayOrder(displayOrder);
    evaluator.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(evaluator);
    taskEvaluatorMapper.insert(evaluator);
  }

  public void insertParamMapping(
      String id,
      String taskId,
      String taskEvaluatorId,
      String paramId,
      String paramName,
      String sourceType,
      String datasetVersionId,
      String datasetFieldId,
      String appOutputName,
      int displayOrder,
      String now
  ) {
    EvalTaskEvaluatorParamMapping mapping = new EvalTaskEvaluatorParamMapping();
    mapping.setId(id);
    mapping.setTaskId(taskId);
    mapping.setTaskEvaluatorId(taskEvaluatorId);
    mapping.setParamId(paramId);
    mapping.setParamName(paramName);
    mapping.setSourceType(sourceType);
    mapping.setDatasetVersionId(datasetVersionId);
    mapping.setDatasetFieldId(datasetFieldId);
    mapping.setAppOutputName(appOutputName);
    mapping.setDisplayOrder(displayOrder);
    mapping.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(mapping);
    paramMappingMapper.insert(mapping);
  }

  public void insertTaskTag(String id, String taskId, String tagId, String status, int displayOrder, String now) {
    EvalTaskTag tag = new EvalTaskTag();
    tag.setId(id);
    tag.setTaskId(taskId);
    tag.setTagId(tagId);
    tag.setStatus(status);
    tag.setDisplayOrder(displayOrder);
    tag.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(tag);
    taskTagMapper.insert(tag);
  }

  public void insertTaskItem(
      String id,
      String taskId,
      String datasetVersionId,
      String datasetItemId,
      int rowNo,
      String status,
      String appOutputStatus,
      String now
  ) {
    EvalTaskItem item = new EvalTaskItem();
    item.setId(id);
    item.setTaskId(taskId);
    item.setDatasetVersionId(datasetVersionId);
    item.setDatasetItemId(datasetItemId);
    item.setRowNo(rowNo);
    item.setStatus(status);
    item.setAppOutput("");
    item.setAppOutputStatus(appOutputStatus);
    item.setAppErrorMessage("");
    item.setStartedAt("");
    item.setFinishedAt("");
    item.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(item);
    taskItemMapper.insert(item);
  }

  public void insertEvaluatorResult(String id, String taskId, String taskItemId, String taskEvaluatorId, String status, String now) {
    EvalTaskEvaluatorResult result = new EvalTaskEvaluatorResult();
    result.setId(id);
    result.setTaskId(taskId);
    result.setTaskItemId(taskItemId);
    result.setTaskEvaluatorId(taskEvaluatorId);
    result.setStatus(status);
    result.setScore(null);
    result.setPassResult("");
    result.setResultValue("");
    result.setErrorMessage("");
    result.setStartedAt("");
    result.setFinishedAt("");
    result.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(result);
    evaluatorResultMapper.insert(result);
  }

  public void insertTagResult(String id, String taskId, String taskItemId, String taskTagId, String status, String now) {
    EvalTaskTagResult result = new EvalTaskTagResult();
    result.setId(id);
    result.setTaskId(taskId);
    result.setTaskItemId(taskItemId);
    result.setTaskTagId(taskTagId);
    result.setStatus(status);
    result.setValueText("");
    result.setValueNumber(null);
    result.setTagOptionId("");
    result.setPassResult("");
    result.setAnnotatorId("");
    result.setAnnotatorName("");
    result.setAnnotatedAt("");
    result.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(result);
    tagResultMapper.insert(result);
  }

  public void deleteTask(String taskId) {
    evaluatorResultMapper.delete(new LambdaQueryWrapper<EvalTaskEvaluatorResult>()
        .eq(EvalTaskEvaluatorResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskEvaluatorResult::getTaskId, taskId));
    tagResultMapper.delete(new LambdaQueryWrapper<EvalTaskTagResult>()
        .eq(EvalTaskTagResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskTagResult::getTaskId, taskId));
    paramMappingMapper.delete(new LambdaQueryWrapper<EvalTaskEvaluatorParamMapping>()
        .eq(EvalTaskEvaluatorParamMapping::getSpaceId, currentSpaceId())
        .eq(EvalTaskEvaluatorParamMapping::getTaskId, taskId));
    appFieldMappingMapper.delete(new LambdaQueryWrapper<EvalTaskAppFieldMapping>()
        .eq(EvalTaskAppFieldMapping::getSpaceId, currentSpaceId())
        .eq(EvalTaskAppFieldMapping::getTaskId, taskId));
    taskEvaluatorMapper.delete(new LambdaQueryWrapper<EvalTaskEvaluator>()
        .eq(EvalTaskEvaluator::getSpaceId, currentSpaceId())
        .eq(EvalTaskEvaluator::getTaskId, taskId));
    taskTagMapper.delete(new LambdaQueryWrapper<EvalTaskTag>()
        .eq(EvalTaskTag::getSpaceId, currentSpaceId())
        .eq(EvalTaskTag::getTaskId, taskId));
    taskItemMapper.delete(new LambdaQueryWrapper<EvalTaskItem>()
        .eq(EvalTaskItem::getSpaceId, currentSpaceId())
        .eq(EvalTaskItem::getTaskId, taskId));
    taskMapper.delete(new LambdaQueryWrapper<EvalTask>()
        .eq(EvalTask::getSpaceId, currentSpaceId())
        .eq(EvalTask::getId, taskId));
  }

  public void updateTaskStatus(String taskId, String status, String startedAt, String finishedAt, String now) {
    taskMapper.update(null, new LambdaUpdateWrapper<EvalTask>()
        .eq(EvalTask::getSpaceId, currentSpaceId())
        .eq(EvalTask::getId, taskId)
        .set(EvalTask::getStatus, status)
        .set(startedAt != null, EvalTask::getStartedAt, startedAt)
        .set(finishedAt != null, EvalTask::getFinishedAt, finishedAt)
        .set(EvalTask::getLastUpdatedBy, currentUserId())
        .set(EvalTask::getLastUpdatedByName, currentUserName())
        .set(EvalTask::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void updateTaskEvaluatorStatus(String taskEvaluatorId, String status, String now) {
    taskEvaluatorMapper.update(null, new LambdaUpdateWrapper<EvalTaskEvaluator>()
        .eq(EvalTaskEvaluator::getSpaceId, currentSpaceId())
        .eq(EvalTaskEvaluator::getId, taskEvaluatorId)
        .set(EvalTaskEvaluator::getStatus, status)
        .set(EvalTaskEvaluator::getLastUpdatedBy, currentUserId())
        .set(EvalTaskEvaluator::getLastUpdatedByName, currentUserName())
        .set(EvalTaskEvaluator::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void updateTaskTagStatus(String taskTagId, String status, String now) {
    taskTagMapper.update(null, new LambdaUpdateWrapper<EvalTaskTag>()
        .eq(EvalTaskTag::getSpaceId, currentSpaceId())
        .eq(EvalTaskTag::getId, taskTagId)
        .set(EvalTaskTag::getStatus, status)
        .set(EvalTaskTag::getLastUpdatedBy, currentUserId())
        .set(EvalTaskTag::getLastUpdatedByName, currentUserName())
        .set(EvalTaskTag::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void resetTaskItemsForRestart(String taskId, String appOutputStatus, String now) {
    taskItemMapper.update(null, new LambdaUpdateWrapper<EvalTaskItem>()
        .eq(EvalTaskItem::getSpaceId, currentSpaceId())
        .eq(EvalTaskItem::getTaskId, taskId)
        .set(EvalTaskItem::getStatus, "pending")
        .set(EvalTaskItem::getAppOutput, "")
        .set(EvalTaskItem::getAppOutputStatus, appOutputStatus)
        .set(EvalTaskItem::getAppErrorMessage, "")
        .set(EvalTaskItem::getStartedAt, "")
        .set(EvalTaskItem::getFinishedAt, "")
        .set(EvalTaskItem::getLastUpdatedBy, currentUserId())
        .set(EvalTaskItem::getLastUpdatedByName, currentUserName())
        .set(EvalTaskItem::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void resetEvaluatorResultsForRestart(String taskId, String now) {
    evaluatorResultMapper.update(null, new LambdaUpdateWrapper<EvalTaskEvaluatorResult>()
        .eq(EvalTaskEvaluatorResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskEvaluatorResult::getTaskId, taskId)
        .set(EvalTaskEvaluatorResult::getStatus, "pending")
        .set(EvalTaskEvaluatorResult::getScore, null)
        .set(EvalTaskEvaluatorResult::getPassResult, "")
        .set(EvalTaskEvaluatorResult::getResultValue, "")
        .set(EvalTaskEvaluatorResult::getErrorMessage, "")
        .set(EvalTaskEvaluatorResult::getStartedAt, "")
        .set(EvalTaskEvaluatorResult::getFinishedAt, "")
        .set(EvalTaskEvaluatorResult::getLastUpdatedBy, currentUserId())
        .set(EvalTaskEvaluatorResult::getLastUpdatedByName, currentUserName())
        .set(EvalTaskEvaluatorResult::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void resetTaskTagsForRestart(String taskId, String now) {
    taskTagMapper.update(null, new LambdaUpdateWrapper<EvalTaskTag>()
        .eq(EvalTaskTag::getSpaceId, currentSpaceId())
        .eq(EvalTaskTag::getTaskId, taskId)
        .set(EvalTaskTag::getStatus, "pending")
        .set(EvalTaskTag::getLastUpdatedBy, currentUserId())
        .set(EvalTaskTag::getLastUpdatedByName, currentUserName())
        .set(EvalTaskTag::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void resetTagResultsForRestart(String taskId, String now) {
    tagResultMapper.update(null, new LambdaUpdateWrapper<EvalTaskTagResult>()
        .eq(EvalTaskTagResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskTagResult::getTaskId, taskId)
        .set(EvalTaskTagResult::getStatus, "pending")
        .set(EvalTaskTagResult::getValueText, "")
        .set(EvalTaskTagResult::getValueNumber, null)
        .set(EvalTaskTagResult::getTagOptionId, "")
        .set(EvalTaskTagResult::getPassResult, "")
        .set(EvalTaskTagResult::getAnnotatorId, "")
        .set(EvalTaskTagResult::getAnnotatorName, "")
        .set(EvalTaskTagResult::getAnnotatedAt, "")
        .set(EvalTaskTagResult::getLastUpdatedBy, currentUserId())
        .set(EvalTaskTagResult::getLastUpdatedByName, currentUserName())
        .set(EvalTaskTagResult::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void updateTaskItemRunResult(
      String taskItemId,
      String status,
      String appOutput,
      String appOutputStatus,
      String appErrorMessage,
      String startedAt,
      String finishedAt,
      String now
  ) {
    taskItemMapper.update(null, new LambdaUpdateWrapper<EvalTaskItem>()
        .eq(EvalTaskItem::getSpaceId, currentSpaceId())
        .eq(EvalTaskItem::getId, taskItemId)
        .set(EvalTaskItem::getStatus, status)
        .set(EvalTaskItem::getAppOutput, appOutput)
        .set(EvalTaskItem::getAppOutputStatus, appOutputStatus)
        .set(EvalTaskItem::getAppErrorMessage, appErrorMessage)
        .set(EvalTaskItem::getStartedAt, startedAt)
        .set(EvalTaskItem::getFinishedAt, finishedAt)
        .set(EvalTaskItem::getLastUpdatedBy, currentUserId())
        .set(EvalTaskItem::getLastUpdatedByName, currentUserName())
        .set(EvalTaskItem::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void updateTaskItemAppResult(String taskItemId, String appOutput, String appOutputStatus, String appErrorMessage, String now) {
    taskItemMapper.update(null, new LambdaUpdateWrapper<EvalTaskItem>()
        .eq(EvalTaskItem::getSpaceId, currentSpaceId())
        .eq(EvalTaskItem::getId, taskItemId)
        .set(EvalTaskItem::getAppOutput, appOutput)
        .set(EvalTaskItem::getAppOutputStatus, appOutputStatus)
        .set(EvalTaskItem::getAppErrorMessage, appErrorMessage)
        .set(EvalTaskItem::getLastUpdatedBy, currentUserId())
        .set(EvalTaskItem::getLastUpdatedByName, currentUserName())
        .set(EvalTaskItem::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void updateTaskItemStatus(String taskItemId, String status, String now) {
    taskItemMapper.update(null, new LambdaUpdateWrapper<EvalTaskItem>()
        .eq(EvalTaskItem::getSpaceId, currentSpaceId())
        .eq(EvalTaskItem::getId, taskItemId)
        .set(EvalTaskItem::getStatus, status)
        .set(EvalTaskItem::getLastUpdatedBy, currentUserId())
        .set(EvalTaskItem::getLastUpdatedByName, currentUserName())
        .set(EvalTaskItem::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void updateEvaluatorResult(
      String taskItemId,
      String taskEvaluatorId,
      String status,
      BigDecimal score,
      String passResult,
      String resultValue,
      String errorMessage,
      String startedAt,
      String finishedAt,
      String now
  ) {
    evaluatorResultMapper.update(null, new LambdaUpdateWrapper<EvalTaskEvaluatorResult>()
        .eq(EvalTaskEvaluatorResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskEvaluatorResult::getTaskItemId, taskItemId)
        .eq(EvalTaskEvaluatorResult::getTaskEvaluatorId, taskEvaluatorId)
        .set(EvalTaskEvaluatorResult::getStatus, status)
        .set(EvalTaskEvaluatorResult::getScore, score)
        .set(EvalTaskEvaluatorResult::getPassResult, passResult)
        .set(EvalTaskEvaluatorResult::getResultValue, resultValue)
        .set(EvalTaskEvaluatorResult::getErrorMessage, errorMessage)
        .set(EvalTaskEvaluatorResult::getStartedAt, startedAt)
        .set(EvalTaskEvaluatorResult::getFinishedAt, finishedAt)
        .set(EvalTaskEvaluatorResult::getLastUpdatedBy, currentUserId())
        .set(EvalTaskEvaluatorResult::getLastUpdatedByName, currentUserName())
        .set(EvalTaskEvaluatorResult::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void updateTagResult(
      String taskItemId,
      String taskTagId,
      String status,
      String valueText,
      BigDecimal valueNumber,
      String tagOptionId,
      String passResult,
      String annotatorId,
      String annotatorName,
      String annotatedAt,
      String now
  ) {
    tagResultMapper.update(null, new LambdaUpdateWrapper<EvalTaskTagResult>()
        .eq(EvalTaskTagResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskTagResult::getTaskItemId, taskItemId)
        .eq(EvalTaskTagResult::getTaskTagId, taskTagId)
        .set(EvalTaskTagResult::getStatus, status)
        .set(EvalTaskTagResult::getValueText, valueText)
        .set(EvalTaskTagResult::getValueNumber, valueNumber)
        .set(EvalTaskTagResult::getTagOptionId, tagOptionId)
        .set(EvalTaskTagResult::getPassResult, passResult)
        .set(EvalTaskTagResult::getAnnotatorId, annotatorId)
        .set(EvalTaskTagResult::getAnnotatorName, annotatorName)
        .set(EvalTaskTagResult::getAnnotatedAt, annotatedAt)
        .set(EvalTaskTagResult::getLastUpdatedBy, currentUserId())
        .set(EvalTaskTagResult::getLastUpdatedByName, currentUserName())
        .set(EvalTaskTagResult::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public List<TaskItemRecord> listTaskItems(String taskId, int size, int offset) {
    return taskItemMapper.selectList(new LambdaQueryWrapper<EvalTaskItem>()
            .eq(EvalTaskItem::getSpaceId, currentSpaceId())
            .eq(EvalTaskItem::getTaskId, taskId)
            .orderByAsc(EvalTaskItem::getRowNo)
            .last("LIMIT " + size + " OFFSET " + offset))
        .stream()
        .map(this::toTaskItemRecord)
        .toList();
  }

  public List<TaskItemRecord> listAllTaskItems(String taskId) {
    return taskItemMapper.selectList(new LambdaQueryWrapper<EvalTaskItem>()
            .eq(EvalTaskItem::getSpaceId, currentSpaceId())
            .eq(EvalTaskItem::getTaskId, taskId)
            .orderByAsc(EvalTaskItem::getRowNo))
        .stream()
        .map(this::toTaskItemRecord)
        .toList();
  }

  public long countTaskItems(String taskId) {
    return taskItemMapper.selectCount(new LambdaQueryWrapper<EvalTaskItem>()
        .eq(EvalTaskItem::getSpaceId, currentSpaceId())
        .eq(EvalTaskItem::getTaskId, taskId));
  }

  public List<TaskEvaluatorResultDto> listEvaluatorResultsByTaskItemIds(List<String> taskItemIds) {
    return taskItemIds == null || taskItemIds.isEmpty() ? List.of() : CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        taskMapper.listEvaluatorResultsByTaskItemIds(currentSpaceId(), taskItemIds));
  }

  public List<TaskTagResultDto> listTagResultsByTaskItemIds(List<String> taskItemIds) {
    return taskItemIds == null || taskItemIds.isEmpty() ? List.of() : CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        taskMapper.listTagResultsByTaskItemIds(currentSpaceId(), taskItemIds));
  }

  public List<TaskAppFieldMappingRecord> listAppFieldMappings(String taskId) {
    return appFieldMappingMapper.selectList(new LambdaQueryWrapper<EvalTaskAppFieldMapping>()
            .eq(EvalTaskAppFieldMapping::getSpaceId, currentSpaceId())
            .eq(EvalTaskAppFieldMapping::getTaskId, taskId)
            .orderByAsc(EvalTaskAppFieldMapping::getDisplayOrder))
        .stream()
        .map(this::toAppFieldMappingRecord)
        .toList();
  }

  public List<TaskEvaluatorBindingRecord> listTaskEvaluatorBindings(String taskId) {
    return taskEvaluatorMapper.selectList(new LambdaQueryWrapper<EvalTaskEvaluator>()
            .eq(EvalTaskEvaluator::getSpaceId, currentSpaceId())
            .eq(EvalTaskEvaluator::getTaskId, taskId)
            .orderByAsc(EvalTaskEvaluator::getDisplayOrder))
        .stream()
        .map(this::toEvaluatorBindingRecord)
        .toList();
  }

  public List<TaskEvaluatorParamMappingRecord> listParamMappings(String taskEvaluatorId) {
    return paramMappingMapper.selectList(new LambdaQueryWrapper<EvalTaskEvaluatorParamMapping>()
            .eq(EvalTaskEvaluatorParamMapping::getSpaceId, currentSpaceId())
            .eq(EvalTaskEvaluatorParamMapping::getTaskEvaluatorId, taskEvaluatorId)
            .orderByAsc(EvalTaskEvaluatorParamMapping::getDisplayOrder))
        .stream()
        .map(this::toParamMappingRecord)
        .toList();
  }

  public List<TaskEvaluatorParamMappingRecord> listAllParamMappings(String taskId) {
    return paramMappingMapper.selectList(new LambdaQueryWrapper<EvalTaskEvaluatorParamMapping>()
            .eq(EvalTaskEvaluatorParamMapping::getSpaceId, currentSpaceId())
            .eq(EvalTaskEvaluatorParamMapping::getTaskId, taskId)
            .orderByAsc(EvalTaskEvaluatorParamMapping::getTaskEvaluatorId, EvalTaskEvaluatorParamMapping::getDisplayOrder))
        .stream()
        .map(this::toParamMappingRecord)
        .toList();
  }

  public List<TaskTagBindingRecord> listTaskTagBindings(String taskId) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        taskMapper.listTaskTagBindings(currentSpaceId(), taskId));
  }

  public TaskItemRecord findTaskItem(String taskItemId) {
    EvalTaskItem item = taskItemMapper.selectOne(new LambdaQueryWrapper<EvalTaskItem>()
        .eq(EvalTaskItem::getSpaceId, currentSpaceId())
        .eq(EvalTaskItem::getId, taskItemId)
        .last("LIMIT 1"));
    return item == null ? null : toTaskItemRecord(item);
  }

  public String findPreviousTaskItemId(String taskId, int rowNo) {
    EvalTaskItem item = taskItemMapper.selectOne(new LambdaQueryWrapper<EvalTaskItem>()
        .select(EvalTaskItem::getId)
        .eq(EvalTaskItem::getSpaceId, currentSpaceId())
        .eq(EvalTaskItem::getTaskId, taskId)
        .lt(EvalTaskItem::getRowNo, rowNo)
        .orderByDesc(EvalTaskItem::getRowNo)
        .last("LIMIT 1"));
    return item == null ? null : item.getId();
  }

  public String findNextTaskItemId(String taskId, int rowNo) {
    EvalTaskItem item = taskItemMapper.selectOne(new LambdaQueryWrapper<EvalTaskItem>()
        .select(EvalTaskItem::getId)
        .eq(EvalTaskItem::getSpaceId, currentSpaceId())
        .eq(EvalTaskItem::getTaskId, taskId)
        .gt(EvalTaskItem::getRowNo, rowNo)
        .orderByAsc(EvalTaskItem::getRowNo)
        .last("LIMIT 1"));
    return item == null ? null : item.getId();
  }

  public int countTagResults(String taskTagId) {
    return Math.toIntExact(tagResultMapper.selectCount(new LambdaQueryWrapper<EvalTaskTagResult>()
        .eq(EvalTaskTagResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskTagResult::getTaskTagId, taskTagId)));
  }

  public int countCompletedTagResults(String taskTagId) {
    return Math.toIntExact(tagResultMapper.selectCount(new LambdaQueryWrapper<EvalTaskTagResult>()
        .eq(EvalTaskTagResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskTagResult::getTaskTagId, taskTagId)
        .eq(EvalTaskTagResult::getStatus, "completed")));
  }

  public int countUnfinishedTagResultsByItem(String taskItemId) {
    return Math.toIntExact(tagResultMapper.selectCount(new LambdaQueryWrapper<EvalTaskTagResult>()
        .eq(EvalTaskTagResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskTagResult::getTaskItemId, taskItemId)
        .ne(EvalTaskTagResult::getStatus, "completed")));
  }

  public int countUnfinishedEvaluatorResultsByItem(String taskItemId) {
    return Math.toIntExact(evaluatorResultMapper.selectCount(new LambdaQueryWrapper<EvalTaskEvaluatorResult>()
        .eq(EvalTaskEvaluatorResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskEvaluatorResult::getTaskItemId, taskItemId)
        .notIn(EvalTaskEvaluatorResult::getStatus, List.of("completed", "skipped"))));
  }

  public int countUnfinishedTaskItems(String taskId) {
    return Math.toIntExact(taskItemMapper.selectCount(new LambdaQueryWrapper<EvalTaskItem>()
        .eq(EvalTaskItem::getSpaceId, currentSpaceId())
        .eq(EvalTaskItem::getTaskId, taskId)
        .ne(EvalTaskItem::getStatus, "completed")));
  }

  public int countUnfinishedTagResultsByTask(String taskId) {
    return Math.toIntExact(tagResultMapper.selectCount(new LambdaQueryWrapper<EvalTaskTagResult>()
        .eq(EvalTaskTagResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskTagResult::getTaskId, taskId)
        .ne(EvalTaskTagResult::getStatus, "completed")));
  }

  public int countUnfinishedEvaluatorResultsByTask(String taskId) {
    return Math.toIntExact(evaluatorResultMapper.selectCount(new LambdaQueryWrapper<EvalTaskEvaluatorResult>()
        .eq(EvalTaskEvaluatorResult::getSpaceId, currentSpaceId())
        .eq(EvalTaskEvaluatorResult::getTaskId, taskId)
        .notIn(EvalTaskEvaluatorResult::getStatus, List.of("completed", "skipped"))));
  }

  private LambdaQueryWrapper<EvalTask> taskQuery(String status, String like) {
    return new LambdaQueryWrapper<EvalTask>()
        .eq(EvalTask::getSpaceId, currentSpaceId())
        .eq(StringUtils.hasText(status), EvalTask::getStatus, status)
        .like(hasLikeText(like), EvalTask::getTaskName, likeText(like));
  }

  private TaskItemRecord toTaskItemRecord(EvalTaskItem item) {
    return new TaskItemRecord(
        item.getId(),
        item.getTaskId(),
        item.getDatasetVersionId(),
        item.getDatasetItemId(),
        item.getRowNo(),
        item.getStatus(),
        item.getAppOutput(),
        item.getAppOutputStatus(),
        item.getAppErrorMessage(),
        item.getCreatedDate(),
        item.getLastUpdatedDate());
  }

  private TaskAppFieldMappingRecord toAppFieldMappingRecord(EvalTaskAppFieldMapping mapping) {
    return new TaskAppFieldMappingRecord(
        mapping.getId(),
        mapping.getTaskId(),
        mapping.getAppInputId(),
        mapping.getAppInputName(),
        mapping.getAppInputType(),
        mapping.getDatasetVersionId(),
        mapping.getDatasetFieldId(),
        mapping.getDisplayOrder());
  }

  private TaskEvaluatorBindingRecord toEvaluatorBindingRecord(EvalTaskEvaluator evaluator) {
    return new TaskEvaluatorBindingRecord(
        evaluator.getId(),
        evaluator.getTaskId(),
        evaluator.getEvaluatorSource(),
        evaluator.getEvaluatorId(),
        evaluator.getEvaluatorVersionId(),
        evaluator.getModelId(),
        evaluator.getModelName(),
        evaluator.getStatus(),
        evaluator.getDisplayOrder());
  }

  private TaskEvaluatorParamMappingRecord toParamMappingRecord(EvalTaskEvaluatorParamMapping mapping) {
    return new TaskEvaluatorParamMappingRecord(
        mapping.getId(),
        mapping.getTaskId(),
        mapping.getTaskEvaluatorId(),
        mapping.getParamId(),
        mapping.getParamName(),
        mapping.getSourceType(),
        mapping.getDatasetVersionId(),
        mapping.getDatasetFieldId(),
        mapping.getAppOutputName(),
        mapping.getDisplayOrder());
  }

  private void fillCreated(EvalTask task) {
    task.setSpaceId(currentSpaceId());
    task.setCreatedBy(currentUserId());
    task.setCreatedByName(currentUserName());
    task.setLastUpdatedBy(currentUserId());
    task.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalTaskAppFieldMapping mapping) {
    mapping.setSpaceId(currentSpaceId());
    mapping.setCreatedBy(currentUserId());
    mapping.setCreatedByName(currentUserName());
    mapping.setLastUpdatedBy(currentUserId());
    mapping.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalTaskEvaluator evaluator) {
    evaluator.setSpaceId(currentSpaceId());
    evaluator.setCreatedBy(currentUserId());
    evaluator.setCreatedByName(currentUserName());
    evaluator.setLastUpdatedBy(currentUserId());
    evaluator.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalTaskEvaluatorParamMapping mapping) {
    mapping.setSpaceId(currentSpaceId());
    mapping.setCreatedBy(currentUserId());
    mapping.setCreatedByName(currentUserName());
    mapping.setLastUpdatedBy(currentUserId());
    mapping.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalTaskTag tag) {
    tag.setSpaceId(currentSpaceId());
    tag.setCreatedBy(currentUserId());
    tag.setCreatedByName(currentUserName());
    tag.setLastUpdatedBy(currentUserId());
    tag.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalTaskItem item) {
    item.setSpaceId(currentSpaceId());
    item.setCreatedBy(currentUserId());
    item.setCreatedByName(currentUserName());
    item.setLastUpdatedBy(currentUserId());
    item.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalTaskEvaluatorResult result) {
    result.setSpaceId(currentSpaceId());
    result.setCreatedBy(currentUserId());
    result.setCreatedByName(currentUserName());
    result.setLastUpdatedBy(currentUserId());
    result.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalTaskTagResult result) {
    result.setSpaceId(currentSpaceId());
    result.setCreatedBy(currentUserId());
    result.setCreatedByName(currentUserName());
    result.setLastUpdatedBy(currentUserId());
    result.setLastUpdatedByName(currentUserName());
  }

  private String currentSpaceId() {
    return Objects.toString(CurrentSpaceHolder.get(), "");
  }

  private String currentUserId() {
    CurrentUser user = CurrentUserHolder.get();
    return user == null ? "" : Objects.toString(user.userId(), "");
  }

  private String currentUserName() {
    CurrentUser user = CurrentUserHolder.get();
    return user == null ? "" : Objects.toString(user.displayName(), "");
  }

  private boolean hasLikeText(String like) {
    return StringUtils.hasText(like) && !"%%".equals(like);
  }

  private String likeText(String like) {
    return hasLikeText(like) && like.length() > 1 ? like.substring(1, like.length() - 1) : "";
  }

  private LocalDateTime toLastUpdatedDate(String now) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(now)), ZoneId.systemDefault());
  }
}
