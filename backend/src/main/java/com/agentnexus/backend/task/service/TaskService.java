package com.agentnexus.backend.task.service;

import com.agentnexus.backend.common.PageResponse;
import com.agentnexus.backend.common.dto.DescriptionUpdateRequest;
import com.agentnexus.backend.common.context.CurrentSpaceHolder;
import com.agentnexus.backend.common.context.CurrentUserHolder;
import com.agentnexus.backend.common.context.TaskCookieHolder;
import com.agentnexus.backend.common.security.CurrentUser;
import com.agentnexus.backend.dataset.api.dto.response.DatasetSummary;
import com.agentnexus.backend.dataset.api.dto.response.DatasetVersionDto;
import com.agentnexus.backend.dataset.api.dto.response.FieldDto;
import com.agentnexus.backend.dataset.repository.DatasetRepository;
import com.agentnexus.backend.dataset.repository.DatasetRowRecord;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorConfig;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorParamDto;
import com.agentnexus.backend.evaluator.api.dto.response.PresetEvaluatorDetail;
import com.agentnexus.backend.evaluator.service.EvaluatorService;
import com.agentnexus.backend.remoteCall.api.dto.request.AgentChatRequest;
import com.agentnexus.backend.remoteCall.api.dto.response.AgentChatResponse;
import com.agentnexus.backend.remoteCall.api.dto.request.AgentMessage;
import com.agentnexus.backend.remoteCall.api.dto.response.ModelChatResult;
import com.agentnexus.backend.remoteCall.service.RemoteCallService;
import com.agentnexus.backend.tag.api.dto.response.TagOptionDto;
import com.agentnexus.backend.tag.repository.TagRepository;
import com.agentnexus.backend.task.api.dto.response.AnnotationDetail;
import com.agentnexus.backend.task.api.dto.request.AppFieldMappingInput;
import com.agentnexus.backend.task.api.dto.request.CreateTaskRequest;
import com.agentnexus.backend.task.api.dto.request.SaveAnnotationRequest;
import com.agentnexus.backend.task.api.dto.request.TagAnnotationInput;
import com.agentnexus.backend.task.api.dto.response.TaskBase;
import com.agentnexus.backend.task.api.dto.response.TaskDetail;
import com.agentnexus.backend.task.api.dto.response.TaskEvaluatorDimension;
import com.agentnexus.backend.task.api.dto.response.TaskEvaluatorParamDisplay;
import com.agentnexus.backend.task.api.dto.request.TaskEvaluatorInput;
import com.agentnexus.backend.task.api.dto.request.TaskEvaluatorParamMappingInput;
import com.agentnexus.backend.task.api.dto.response.TaskEvaluatorResultDto;
import com.agentnexus.backend.task.api.dto.response.TaskItemDetail;
import com.agentnexus.backend.task.api.dto.response.TaskMetricDimensionScore;
import com.agentnexus.backend.task.api.dto.response.TaskMetricItemDistribution;
import com.agentnexus.backend.task.api.dto.response.TaskMetricOverview;
import com.agentnexus.backend.task.api.dto.response.TaskMetricProgress;
import com.agentnexus.backend.task.api.dto.response.TaskMetricScoreSummary;
import com.agentnexus.backend.task.api.dto.response.TaskSummary;
import com.agentnexus.backend.task.api.dto.response.TaskTagAnnotation;
import com.agentnexus.backend.task.api.dto.response.TaskTagDimension;
import com.agentnexus.backend.task.api.dto.response.TaskTagResultDto;
import com.agentnexus.backend.task.entity.EvalTask;
import com.agentnexus.backend.task.repository.TaskAppFieldMappingRecord;
import com.agentnexus.backend.task.repository.TaskEvaluatorBindingRecord;
import com.agentnexus.backend.task.repository.TaskEvaluatorParamMappingRecord;
import com.agentnexus.backend.task.repository.TaskItemRecord;
import com.agentnexus.backend.task.repository.TaskRepository;
import com.agentnexus.backend.task.repository.TaskTagBindingRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.core.task.TaskExecutor;

@Service
public class TaskService {
  private static final String STATUS_PENDING = "pending";
  private static final String STATUS_RUNNING = "running";
  private static final String STATUS_COMPLETED = "completed";
  private static final String STATUS_FAILED = "failed";
  private static final String STATUS_STOPPED = "stopped";
  private static final String ITEM_ANNOTATION_PENDING = "annotation_pending";
  private static final String RESULT_SKIPPED = "skipped";
  private static final String APP_NONE = "none";
  private static final String APP_AGENT = "agent";
  private static final String EVALUATOR_PRESET = "preset";
  private static final String EVALUATOR_CUSTOM = "custom";
  private static final String SOURCE_DATASET_FIELD = "dataset_field";
  private static final String SOURCE_APP_OUTPUT = "app_output";
  private static final String METRIC_EVALUATOR = "evaluator";
  private static final String METRIC_TAG = "tag";
  private static final List<String> SUPPORTED_FIELD_TYPES = List.of("string", "number", "boolean");
  private static final int MAX_DIMENSION_COUNT = 5;
  private static final Pattern PROMPT_PARAM_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][\\w]*)}");

  private final TaskRepository taskRepository;
  private final DatasetRepository datasetRepository;
  private final EvaluatorService evaluatorService;
  private final TagRepository tagRepository;
  private final RemoteCallService remoteCallService;
  private final ObjectMapper objectMapper;
  private final TaskExecutor taskExecutor;

  public TaskService(
      TaskRepository taskRepository,
      DatasetRepository datasetRepository,
      EvaluatorService evaluatorService,
      TagRepository tagRepository,
      RemoteCallService remoteCallService,
      ObjectMapper objectMapper,
      TaskExecutor taskExecutor
  ) {
    this.taskRepository = taskRepository;
    this.datasetRepository = datasetRepository;
    this.evaluatorService = evaluatorService;
    this.tagRepository = tagRepository;
    this.remoteCallService = remoteCallService;
    this.objectMapper = objectMapper;
    this.taskExecutor = taskExecutor;
  }

  public PageResponse<TaskSummary> listTasks(int page, int size, String status, String keyword, String sortBy, String sortOrder) {
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 100);
    int offset = (safePage - 1) * safeSize;
    String normalizedStatus = normalizeOptionalStatus(status);
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    String orderColumn = "createdDate".equals(sortBy) ? "t.created_date" : "t.last_updated_date";
    String orderDirection = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
    List<TaskSummary> records = taskRepository.listTaskBases(normalizedStatus, like, orderColumn, orderDirection, safeSize, offset)
        .stream()
        .map(this::toSummary)
        .toList();
    long total = taskRepository.countTaskBases(normalizedStatus, like);
    return new PageResponse<>(records, total, safePage, safeSize);
  }

  @Transactional
  public TaskDetail createTask(CreateTaskRequest request) {
    NormalizedTask normalized = normalizeCreateRequest(request);
    String taskId = id();
    String now = now();
    if (taskRepository.existsTaskName(normalized.taskName())) {
      throw new IllegalArgumentException("当前空间已存在同名评测任务");
    } else {
      taskRepository.insertTask(toEvalTask(taskId, normalized), now);
    }

    saveAppMappings(taskId, normalized, now);
    List<String> taskEvaluatorIds = saveEvaluators(taskId, normalized, now);
    List<String> taskTagIds = saveTags(taskId, normalized, now);
    saveTaskItems(taskId, normalized, taskEvaluatorIds, taskTagIds, now);
    return getTask(taskId, 1, 10);
  }

  public TaskDetail getTask(String taskId, int page, int size) {
    TaskBase base = findTask(taskId);
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 100);
    int offset = (safePage - 1) * safeSize;
    List<FieldDto> fields = datasetRepository.listFields(base.datasetVersionId());
    Map<String, FieldDto> fieldById = toFieldById(fields);
    List<TaskItemRecord> itemRecords = taskRepository.listTaskItems(taskId, safeSize, offset);
    long total = taskRepository.countTaskItems(taskId);
	    return new TaskDetail(
	        base,
	        fields,
	        buildEvaluatorDimensions(taskId, fieldById),
	        taskRepository.listTagDimensions(taskId),
	        new PageResponse<>(buildItems(itemRecords, fieldById), total, safePage, safeSize));
  }

  /**
   * 更新评测任务描述。
   *
   * @param taskId 评测任务ID
   * @param request 描述更新请求
   * @return 更新后的评测任务详情
   */
  @Transactional
  public TaskDetail updateDescription(String taskId, DescriptionUpdateRequest request) {
    TaskBase task = findTask(taskId);
    if (!taskRepository.isTaskCreatedByCurrentUser(task.id())) {
      throw new IllegalArgumentException("仅创建人可以修改评测任务描述");
    } else {
      String description = normalizeDescription(request == null ? null : request.description());
      taskRepository.updateTaskDescription(task.id(), description);
      return getTask(task.id(), 1, 10);
    }
  }

  /**
   * 查询评测任务指标统计概览。
   *
   * @param taskId 评测任务ID
   * @return 指标统计概览
   */
  public TaskMetricOverview getMetricOverview(String taskId) {
    findTask(taskId);
    long totalCount = taskRepository.countTaskItems(taskId);
    long incompleteCount = taskRepository.countUnfinishedTaskItems(taskId);
    long completedCount = Math.max(totalCount - incompleteCount, 0);
    List<TaskMetricDimensionScore> dimensions = buildMetricDimensions(taskId);
    long passCount = sumMetricPassCount(dimensions);
    long failedCount = sumMetricFailedCount(dimensions);
    return new TaskMetricOverview(
        percent(passCount, passCount + failedCount),
        Math.toIntExact(countScoredDimensions(dimensions)),
        new TaskMetricProgress(percent(completedCount, totalCount), totalCount, completedCount, incompleteCount));
  }

  /**
   * 查询评测任务得分汇总。
   *
   * @param taskId 评测任务ID
   * @return 得分汇总
   */
  public TaskMetricScoreSummary getMetricScoreSummary(String taskId) {
    findTask(taskId);
    return new TaskMetricScoreSummary(buildMetricDimensions(taskId));
  }

  /**
   * 查询评测任务数据项分布。
   *
   * @param taskId 评测任务ID
   * @return 数据项分布
   */
  public TaskMetricItemDistribution getMetricItemDistribution(String taskId) {
    findTask(taskId);
    return new TaskMetricItemDistribution(buildMetricDimensions(taskId));
  }

  public CreateTaskRequest getTaskCopyConfig(String taskId) {
    TaskBase base = findTask(taskId);
    Map<String, List<TaskEvaluatorParamMappingRecord>> mappingsByEvaluator = taskRepository.listAllParamMappings(taskId)
        .stream()
        .collect(Collectors.groupingBy(TaskEvaluatorParamMappingRecord::taskEvaluatorId));
    List<AppFieldMappingInput> appFieldMappings = taskRepository.listAppFieldMappings(taskId)
        .stream()
        .map(mapping -> new AppFieldMappingInput(
            mapping.appInputId(),
            mapping.appInputName(),
            mapping.appInputType(),
            mapping.datasetFieldId()))
        .toList();
    List<TaskEvaluatorInput> evaluators = taskRepository.listTaskEvaluatorBindings(taskId)
        .stream()
        .map(evaluator -> new TaskEvaluatorInput(
            evaluator.evaluatorSource(),
            evaluator.evaluatorId(),
            evaluator.evaluatorVersionId(),
            evaluator.modelId(),
            evaluator.modelName(),
            mappingsByEvaluator.getOrDefault(evaluator.id(), List.of())
                .stream()
                .map(mapping -> new TaskEvaluatorParamMappingInput(
                    mapping.paramId(),
                    mapping.paramName(),
                    mapping.sourceType(),
                    mapping.datasetFieldId(),
                    mapping.appOutputName()))
                .toList()))
        .toList();
    List<String> tagIds = taskRepository.listTaskTagBindings(taskId)
        .stream()
        .map(TaskTagBindingRecord::tagId)
        .toList();
    return new CreateTaskRequest(
        base.taskName() + "-副本",
        base.description(),
        base.datasetId(),
        base.datasetVersionId(),
        base.appType(),
        base.appId(),
        base.appName(),
        base.appVersionId(),
        base.appVersionName(),
        base.appAgentAlias(),
        appFieldMappings,
        evaluators,
        tagIds);
  }

  @Transactional
  public TaskDetail startTask(String taskId) {
    return startTask(taskId, "");
  }

  @Transactional
  public TaskDetail startTask(String taskId, String cookie) {
    TaskBase base = findTask(taskId);
    if (STATUS_RUNNING.equals(base.status())) {
      return getTask(taskId, 1, 10);
    }
    if (STATUS_COMPLETED.equals(base.status())) {
      throw new IllegalArgumentException("评测完成的任务不能重新开始");
    }

    String startedAt = now();
    String appOutputStatus = APP_AGENT.equals(base.appType()) ? STATUS_PENDING : RESULT_SKIPPED;
    taskRepository.prepareTaskItemsForRestart(taskId, startedAt);
    taskRepository.prepareAppOutputsForRestart(taskId, appOutputStatus, startedAt);
    taskRepository.prepareEvaluatorResultsForRestart(taskId, startedAt);
    taskRepository.prepareTaskTagsForRestart(taskId, startedAt);
    taskRepository.updateTaskStatus(taskId, STATUS_RUNNING, startedAt, null, startedAt);
    for (TaskEvaluatorBindingRecord evaluator : taskRepository.listTaskEvaluatorBindings(taskId)) {
      taskRepository.updateTaskEvaluatorStatus(evaluator.id(), STATUS_RUNNING, startedAt);
    }
    scheduleTaskExecution(taskId, cookie);
    return getTask(taskId, 1, 10);
  }

  /**
   * 停止正在运行的评测任务，并将未完成的子记录标记为已中止。
   *
   * @param taskId 评测任务ID
   * @return 停止后的评测任务详情
   */
  @Transactional
  public TaskDetail stopTask(String taskId) {
    TaskBase task = findTask(taskId);
    if (STATUS_STOPPED.equals(task.status())) {
      return getTask(taskId, 1, 10);
    }
    if (!STATUS_RUNNING.equals(task.status())) {
      throw new IllegalArgumentException("仅进行中的评测任务可以停止");
    }
    String stoppedAt = now();
    taskRepository.updateTaskStatus(taskId, STATUS_STOPPED, null, stoppedAt, stoppedAt);
    taskRepository.markTaskStoppedChildren(taskId, STATUS_STOPPED, stoppedAt);
    return getTask(taskId, 1, 10);
  }

  private void scheduleTaskExecution(String taskId, String cookie) {
    String spaceId = CurrentSpaceHolder.get();
    CurrentUser currentUser = CurrentUserHolder.get();
    String taskCookie = StringUtils.hasText(cookie) ? cookie.trim() : null;
    Runnable task = () -> taskExecutor.execute(() -> runWithRequestContext(spaceId, currentUser, taskCookie, () -> runTaskSafely(taskId)));
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          task.run();
        }
      });
    } else {
      task.run();
    }
  }

  private void runWithRequestContext(String spaceId, CurrentUser currentUser, String taskCookie, Runnable action) {
    CurrentSpaceHolder.set(spaceId);
    CurrentUserHolder.set(currentUser);
    TaskCookieHolder.set(taskCookie);
    try {
      action.run();
    } finally {
      CurrentUserHolder.clear();
      TaskCookieHolder.clear();
      CurrentSpaceHolder.clear();
    }
  }

  private void runTaskSafely(String taskId) {
    try {
      executeTask(taskId);
    } catch (Exception e) {
      markTaskExecutionFailed(taskId, e);
    }
  }

  private void executeTask(String taskId) {
    TaskExecutionContext context = loadTaskExecutionContext(taskId);
    if (context == null) {
      return;
    }
    TaskExecutionOutcome outcome = executeTaskItems(context);
    if (!shouldContinueTaskExecution(taskId)) {
      return;
    }
    completeTaskExecution(context, outcome);
  }

  private TaskExecutionContext loadTaskExecutionContext(String taskId) {
    TaskBase base = findTask(taskId);
    if (!STATUS_RUNNING.equals(base.status())) {
      return null;
    }
    TaskExecutionContext context = new TaskExecutionContext();
    context.base = base;
    context.items = taskRepository.listAllTaskItems(taskId);
    context.evaluators = taskRepository.listTaskEvaluatorBindings(taskId);
    context.appMappings = taskRepository.listAppFieldMappings(taskId);
    context.evaluatorConfigs = loadEvaluatorRuntimeConfigs(context.evaluators);
    context.mappingsByEvaluator = taskRepository.listAllParamMappings(taskId)
        .stream()
        .collect(Collectors.groupingBy(TaskEvaluatorParamMappingRecord::taskEvaluatorId));
    context.valuesByItem = loadDatasetValues(context.items);
    context.evaluatorResultsByItem = taskRepository.listEvaluatorResultsByTaskItemIds(context.items.stream().map(TaskItemRecord::id).toList())
        .stream()
        .collect(Collectors.groupingBy(TaskEvaluatorResultDto::taskItemId));
    return context;
  }

  private TaskExecutionOutcome executeTaskItems(TaskExecutionContext context) {
    boolean hasTaskFailure = false;
    Set<String> failedEvaluatorIds = new HashSet<>();
    for (TaskItemRecord item : context.items) {
      if (!shouldContinueTaskExecution(context.base.id())) {
        return new TaskExecutionOutcome(hasTaskFailure, failedEvaluatorIds);
      }
      TaskItemExecutionContext itemContext = createItemExecutionContext(context, item);
      if (!shouldExecuteTaskItem(itemContext)) {
        refreshItemAndTaskStatus(context.base.id(), item.id(), now());
        continue;
      }
      ItemExecutionResult itemResult = executeTaskItem(itemContext);
      if (itemResult.failed()) {
        hasTaskFailure = true;
      }
      failedEvaluatorIds.addAll(itemResult.failedEvaluatorIds());
    }
    return new TaskExecutionOutcome(hasTaskFailure, failedEvaluatorIds);
  }

  private TaskItemExecutionContext createItemExecutionContext(TaskExecutionContext context, TaskItemRecord item) {
    TaskItemExecutionContext itemContext = new TaskItemExecutionContext();
    itemContext.task = context;
    itemContext.item = item;
    itemContext.rowValues = context.valuesByItem.getOrDefault(item.datasetItemId(), Map.of());
    itemContext.evaluatorResults = context.evaluatorResultsByItem.getOrDefault(item.id(), List.of())
        .stream()
        .collect(Collectors.toMap(TaskEvaluatorResultDto::taskEvaluatorId, Function.identity(), (left, right) -> left));
    return itemContext;
  }

  private boolean shouldExecuteTaskItem(TaskItemExecutionContext context) {
    return shouldInvokeAgent(context) || context.task.evaluators.stream().anyMatch(evaluator -> shouldExecuteEvaluator(context, evaluator));
  }

  private void completeTaskExecution(TaskExecutionContext context, TaskExecutionOutcome outcome) {
    updateEvaluatorStatuses(context, outcome.failedEvaluatorIds());
    if (!shouldContinueTaskExecution(context.base.id())) {
      return;
    }
    String finalStatus = resolveFinalTaskStatus(context.base.id(), outcome.hasTaskFailure());
    boolean finished = STATUS_COMPLETED.equals(finalStatus) || STATUS_FAILED.equals(finalStatus);
    taskRepository.updateTaskStatus(context.base.id(), finalStatus, null, finished ? now() : null, now());
  }

  private void updateEvaluatorStatuses(TaskExecutionContext context, Set<String> failedEvaluatorIds) {
    for (TaskEvaluatorBindingRecord evaluator : context.evaluators) {
      if (!shouldContinueTaskExecution(context.base.id())) {
        return;
      }
      String evaluatorStatus = failedEvaluatorIds.contains(evaluator.id()) ? STATUS_FAILED : STATUS_COMPLETED;
      taskRepository.updateTaskEvaluatorStatus(evaluator.id(), evaluatorStatus, now());
    }
  }

  private String resolveFinalTaskStatus(String taskId, boolean hasTaskFailure) {
    if (hasTaskFailure) {
      return STATUS_FAILED;
    }
    boolean allChildrenCompleted = taskRepository.countUnfinishedTagResultsByTask(taskId) == 0
        && taskRepository.countUnfinishedEvaluatorResultsByTask(taskId) == 0
        && taskRepository.countUnfinishedTaskItems(taskId) == 0;
    return allChildrenCompleted ? STATUS_COMPLETED : STATUS_RUNNING;
  }

  private ItemExecutionResult executeTaskItem(TaskItemExecutionContext context) {
    Set<String> failedEvaluatorIds = new HashSet<>();
    if (!shouldContinueTaskExecution(context.task.base.id())) {
      return new ItemExecutionResult(false, failedEvaluatorIds);
    }
    String itemStartedAt = now();
    taskRepository.updateTaskItemStatus(context.item.id(), STATUS_RUNNING, itemStartedAt);
    if (!shouldContinueTaskExecution(context.task.base.id())) {
      return new ItemExecutionResult(false, failedEvaluatorIds);
    }

    AgentInvocationResult agentResult = resolveAgentResult(context);
    if (!shouldContinueTaskExecution(context.task.base.id())) {
      return new ItemExecutionResult(false, failedEvaluatorIds);
    }
    if (shouldInvokeAgent(context)) {
      updateTaskItemAppResult(context.item, agentResult);
    }
    if (!shouldContinueTaskExecution(context.task.base.id())) {
      return new ItemExecutionResult(false, failedEvaluatorIds);
    }
    boolean hasFailedEvaluator = evaluateItemEvaluators(context, agentResult, itemStartedAt, failedEvaluatorIds);
    return finishTaskItem(context, agentResult, hasFailedEvaluator, failedEvaluatorIds, itemStartedAt);
  }

  private AgentInvocationResult resolveAgentResult(TaskItemExecutionContext context) {
    if (shouldInvokeAgent(context)) {
      return invokeAgentSafely(context);
    } else {
      return new AgentInvocationResult(
          context.item.appOutput() == null ? "" : context.item.appOutput(),
          firstNonBlank(context.item.appOutputStatus(), RESULT_SKIPPED),
          context.item.appErrorMessage() == null ? "" : context.item.appErrorMessage(),
          extractStoredAppOutputs(context.item.appOutput()));
    }
  }

  private boolean shouldInvokeAgent(TaskItemExecutionContext context) {
    return APP_AGENT.equals(context.task.base.appType()) && !STATUS_COMPLETED.equals(context.item.appOutputStatus());
  }

  private AgentInvocationResult invokeAgentSafely(TaskItemExecutionContext context) {
    try {
      return invokeAgent(
          context.task.base,
          context.item,
          context.task.appMappings,
          context.rowValues);
    } catch (Exception e) {
      return failedAgentResult("Agent execution failed: " + safeErrorMessage(e));
    }
  }

  private void updateTaskItemAppResult(TaskItemRecord item, AgentInvocationResult agentResult) {
    taskRepository.updateTaskItemAppResult(
        item.id(),
        agentResult.content(),
        agentResult.status(),
        agentResult.errorMessage(),
        now());
  }

  private boolean evaluateItemEvaluators(
      TaskItemExecutionContext context,
      AgentInvocationResult agentResult,
      String itemStartedAt,
      Set<String> failedEvaluatorIds
  ) {
    boolean hasFailedEvaluator = STATUS_FAILED.equals(agentResult.status());
    if (hasFailedEvaluator) {
      skipEvaluatorsAfterAgentFailure(context, failedEvaluatorIds, agentResult, itemStartedAt);
    } else {
      hasFailedEvaluator = executeBoundEvaluators(context, agentResult, itemStartedAt, failedEvaluatorIds);
    }
    return hasFailedEvaluator;
  }

  private boolean executeBoundEvaluators(
      TaskItemExecutionContext context,
      AgentInvocationResult agentResult,
      String itemStartedAt,
      Set<String> failedEvaluatorIds
  ) {
    boolean hasFailedEvaluator = false;
    for (TaskEvaluatorBindingRecord evaluator : context.task.evaluators) {
      if (!shouldExecuteEvaluator(context, evaluator)) {
        continue;
      }
      if (!shouldContinueTaskExecution(context.task.base.id())) {
        return hasFailedEvaluator;
      }
      EvaluationSimulationResult result = evaluateBoundEvaluator(context, evaluator, agentResult);
      hasFailedEvaluator = markFailedEvaluatorIfNeeded(evaluator, result, failedEvaluatorIds) || hasFailedEvaluator;
      if (!shouldContinueTaskExecution(context.task.base.id())) {
        return hasFailedEvaluator;
      }
      updateEvaluatorResult(context.item.id(), evaluator.id(), result, itemStartedAt);
      if (!shouldContinueTaskExecution(context.task.base.id())) {
        return hasFailedEvaluator;
      }
    }
    return hasFailedEvaluator;
  }

  private boolean shouldExecuteEvaluator(TaskItemExecutionContext context, TaskEvaluatorBindingRecord evaluator) {
    TaskEvaluatorResultDto result = context.evaluatorResults.get(evaluator.id());
    return result == null || !STATUS_COMPLETED.equals(result.status());
  }

  private EvaluationSimulationResult evaluateBoundEvaluator(
      TaskItemExecutionContext context,
      TaskEvaluatorBindingRecord evaluator,
      AgentInvocationResult agentResult
  ) {
    EvaluationRuntimeConfig config = context.task.evaluatorConfigs.get(evaluator.id());
    List<TaskEvaluatorParamMappingRecord> mappings = context.task.mappingsByEvaluator.getOrDefault(evaluator.id(), List.of());
    try {
      return evaluateWithRemoteCall(config, mappings, context.rowValues, agentResult.outputs());
    } catch (Exception e) {
      return failedEvaluationResult("Evaluator execution failed: " + safeErrorMessage(e));
    }
  }

  private boolean markFailedEvaluatorIfNeeded(
      TaskEvaluatorBindingRecord evaluator,
      EvaluationSimulationResult result,
      Set<String> failedEvaluatorIds
  ) {
    if (STATUS_FAILED.equals(result.status())) {
      failedEvaluatorIds.add(evaluator.id());
      return true;
    } else {
      return false;
    }
  }

  private ItemExecutionResult finishTaskItem(
      TaskItemExecutionContext context,
      AgentInvocationResult agentResult,
      boolean hasFailedEvaluator,
      Set<String> failedEvaluatorIds,
      String itemStartedAt
  ) {
    if (!shouldContinueTaskExecution(context.task.base.id())) {
      return new ItemExecutionResult(hasFailedEvaluator, failedEvaluatorIds);
    }
    String itemFinishedAt = now();
    String itemStatus = hasFailedEvaluator ? STATUS_FAILED : ITEM_ANNOTATION_PENDING;
    if (taskRepository.countUnfinishedTagResultsByItem(context.item.id()) == 0 && !hasFailedEvaluator) {
      itemStatus = STATUS_COMPLETED;
    }
    taskRepository.updateTaskItemRunResult(
        context.item.id(),
        itemStatus,
        agentResult.content(),
        agentResult.status(),
        agentResult.errorMessage(),
        itemStartedAt,
        itemFinishedAt,
        itemFinishedAt);
    if (!shouldContinueTaskExecution(context.task.base.id())) {
      return new ItemExecutionResult(hasFailedEvaluator, failedEvaluatorIds);
    }
    return new ItemExecutionResult(hasFailedEvaluator, failedEvaluatorIds);
  }

  private void skipEvaluatorsAfterAgentFailure(
      TaskItemExecutionContext context,
      Set<String> failedEvaluatorIds,
      AgentInvocationResult agentResult,
      String itemStartedAt
  ) {
    for (TaskEvaluatorBindingRecord evaluator : context.task.evaluators) {
      if (!shouldExecuteEvaluator(context, evaluator)) {
        continue;
      }
      if (!shouldContinueTaskExecution(context.task.base.id())) {
        return;
      }
      failedEvaluatorIds.add(evaluator.id());
      String finishedAt = now();
      taskRepository.updateEvaluatorResult(
          context.item.id(),
          evaluator.id(),
          RESULT_SKIPPED,
          null,
          "",
          "",
          "Agent execution failed, evaluator skipped: " + agentResult.errorMessage(),
          itemStartedAt,
          finishedAt,
          finishedAt);
    }
  }

  private void updateEvaluatorResult(
      String taskItemId,
      String taskEvaluatorId,
      EvaluationSimulationResult result,
      String startedAt
  ) {
    String finishedAt = now();
    taskRepository.updateEvaluatorResult(
        taskItemId,
        taskEvaluatorId,
        result.status(),
        result.score(),
        result.passResult(),
        result.resultValue(),
        result.errorMessage(),
        startedAt,
        finishedAt,
        finishedAt);
  }

  private void markTaskExecutionFailed(String taskId, Exception error) {
    if (!shouldContinueTaskExecution(taskId)) {
      return;
    }
    String now = now();
    for (TaskEvaluatorBindingRecord evaluator : taskRepository.listTaskEvaluatorBindings(taskId)) {
      taskRepository.updateTaskEvaluatorStatus(evaluator.id(), STATUS_FAILED, now);
    }
    taskRepository.updateTaskStatus(
        taskId,
        STATUS_FAILED,
        null,
        now,
        now);
  }

  @Transactional
  public void deleteTask(String taskId) {
    TaskBase task = findTask(taskId);
    if (!taskRepository.isTaskCreatedByCurrentUser(taskId)) {
      throw new IllegalArgumentException("仅创建人可以删除评测任务");
    } else if (!List.of(STATUS_PENDING, STATUS_COMPLETED, STATUS_FAILED, STATUS_STOPPED).contains(task.status())) {
      throw new IllegalArgumentException("仅待执行、评测完成、评测失败和已中止的任务可删除");
    } else {
      taskRepository.deleteTask(taskId);
    }
  }

  /**
   * 为评测任务添加标签并初始化标签结果。
   *
   * @param taskId 评测任务ID
   * @param tagId 标签ID
   * @return 添加标签后的评测任务详情
   */
  @Transactional
  public TaskDetail addTaskTag(String taskId, String tagId) {
    TaskBase task = findTask(taskId);
    String safeTagId = requireText(tagId, "请选择标签");
    if (tagRepository.findTagConfig(safeTagId) == null) {
      throw new IllegalArgumentException("标签不存在");
    } else if (taskRepository.existsTaskTag(taskId, safeTagId)) {
      throw new IllegalArgumentException("该任务已添加此标签");
    } else if (taskRepository.listTaskTagBindings(taskId).size() >= MAX_DIMENSION_COUNT) {
      throw new IllegalArgumentException("标签最多添加5个");
    } else {
      String now = now();
      String taskTagId = id();
      int displayOrder = taskRepository.nextTaskTagDisplayOrder(taskId);
      taskRepository.insertTaskTag(taskTagId, taskId, safeTagId, STATUS_PENDING, displayOrder, now);
      for (TaskItemRecord item : taskRepository.listAllTaskItems(taskId)) {
        taskRepository.insertTagResult(id(), taskId, item.id(), taskTagId, STATUS_PENDING, now);
      }
      refreshTaskAfterTagChange(task, now);
      return getTask(taskId, 1, 10);
    }
  }

  /**
   * 删除评测任务已绑定的标签及其结果。
   *
   * @param taskId 评测任务ID
   * @param taskTagId 任务标签ID
   * @return 删除标签后的评测任务详情
   */
  @Transactional
  public TaskDetail deleteTaskTag(String taskId, String taskTagId) {
    TaskBase task = findTask(taskId);
    String safeTaskTagId = requireText(taskTagId, "请选择标签");
    if (taskRepository.findTaskTagBinding(taskId, safeTaskTagId) == null) {
      throw new IllegalArgumentException("任务标签不存在");
    }
    ensureTaskKeepsEvaluationDimension(taskId, safeTaskTagId);
    String now = now();
    taskRepository.deleteTaskTag(taskId, safeTaskTagId);
    refreshTaskAfterTagChange(task, now);
    return getTask(taskId, 1, 10);
  }

  public AnnotationDetail getAnnotation(String taskId, String taskItemId) {
    TaskBase task = findTask(taskId);
    TaskItemRecord itemRecord = findTaskItem(taskId, taskItemId);
    List<FieldDto> fields = datasetRepository.listFields(task.datasetVersionId());
    Map<String, FieldDto> fieldById = toFieldById(fields);
    TaskItemDetail item = buildItems(List.of(itemRecord), fieldById).getFirst();
    List<TaskTagAnnotation> tags = buildTagAnnotations(taskId, itemRecord);
    return new AnnotationDetail(
        task,
        item,
        fields,
        tags,
        item.evaluatorResults(),
        taskRepository.findPreviousTaskItemId(taskId, itemRecord.rowNo()),
        taskRepository.findNextTaskItemId(taskId, itemRecord.rowNo()));
  }

  @Transactional
  public AnnotationDetail saveAnnotation(String taskId, String taskItemId, SaveAnnotationRequest request) {
    TaskBase task = findTask(taskId);
    if (STATUS_STOPPED.equals(task.status())) {
      throw new IllegalArgumentException("已中止的评测任务不能继续标注");
    }
    TaskItemRecord item = findTaskItem(taskId, taskItemId);
    if (request == null || request.tags() == null || request.tags().isEmpty()) {
      throw new IllegalArgumentException("请提交标注结果");
    }
    Map<String, TaskTagBindingRecord> tagsById = taskRepository.listTaskTagBindings(taskId)
        .stream()
        .collect(Collectors.toMap(TaskTagBindingRecord::id, Function.identity()));
    String annotatedAt = now();
    Set<String> touchedTagIds = new HashSet<>();
    for (TagAnnotationInput input : request.tags()) {
      if (input == null || !StringUtils.hasText(input.taskTagId())) {
        continue;
      }
      TaskTagBindingRecord tag = tagsById.get(input.taskTagId());
      if (tag == null) {
        throw new IllegalArgumentException("标签不属于当前任务");
      }
      NormalizedAnnotation annotation = normalizeAnnotation(tag, input);
      taskRepository.updateTagResult(
          item.id(),
          tag.id(),
          STATUS_COMPLETED,
          annotation.valueText(),
          annotation.valueNumber(),
          annotation.tagOptionId(),
          annotation.passResult(),
          "",
          "",
          annotatedAt,
          annotatedAt);
      touchedTagIds.add(tag.id());
    }
    for (String taskTagId : touchedTagIds) {
      refreshTaskTagStatus(taskTagId, annotatedAt);
    }
    refreshItemAndTaskStatus(task.id(), item.id(), annotatedAt);
    return getAnnotation(task.id(), item.id());
  }

  private NormalizedTask normalizeCreateRequest(CreateTaskRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("评测任务参数不能为空");
    }
    String taskName = requireText(request.taskName(), "任务名称不能为空");
    if (taskName.length() > 50) {
      throw new IllegalArgumentException("任务名称不能超过50个字符");
    }
    String description = normalizeDescription(request.description());
    String datasetId = requireText(request.datasetId(), "请选择评测集");
    String datasetVersionId = requireText(request.datasetVersionId(), "请选择评测集版本");
    DatasetVersionDto version = datasetRepository.findVersion(datasetVersionId);
    if (version == null || !datasetId.equals(version.datasetId())) {
      throw new IllegalArgumentException("评测集版本不存在");
    }
    if (Boolean.TRUE.equals(version.draft())) {
      throw new IllegalArgumentException("评测任务请选择已发布的评测集版本");
    }
    DatasetSummary dataset = datasetRepository.findDatasetSummary(datasetId);
    if (dataset == null) {
      throw new IllegalArgumentException("评测集不存在");
    }
    List<FieldDto> fields = datasetRepository.listFields(datasetVersionId);
    Map<String, FieldDto> fieldById = fields.stream()
        .filter(field -> StringUtils.hasText(field.id()))
        .collect(Collectors.toMap(FieldDto::id, Function.identity()));
    List<DatasetRowRecord> rows = datasetRepository.listAllRows(datasetVersionId);
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("评测集版本中暂无数据，不能创建任务");
    }

    String appType = normalizeAppType(request.appType());
    String appId = APP_AGENT.equals(appType) ? requireText(request.appId(), "请选择智能体应用") : "";
    String appName = APP_AGENT.equals(appType) ? savedNameText(request.appName(), appId, 128) : "";
    String appVersionId = APP_AGENT.equals(appType) ? requireText(request.appVersionId(), "请选择智能体应用版本") : "";
    String appVersionName = APP_AGENT.equals(appType) ? savedNameText(request.appVersionName(), appVersionId, 128) : "";
    String appAgentAlias = APP_AGENT.equals(appType) && StringUtils.hasText(request.appAgentAlias())
        ? request.appAgentAlias().trim()
        : "";
    List<AppFieldMappingInput> appMappings = normalizeAppMappings(appType, request.appFieldMappings(), fieldById);
    List<NormalizedEvaluator> evaluators = normalizeEvaluators(appType, request.evaluators(), fieldById, datasetVersionId);
    List<String> tagIds = normalizeTags(request.tagIds());
    if (evaluators.isEmpty() && tagIds.isEmpty()) {
      throw new IllegalArgumentException("请至少添加一个评估器或标签");
    }
    return new NormalizedTask(
        taskName,
        description,
        datasetId,
        datasetVersionId,
        appType,
        appId,
        appName,
        appVersionId,
        appVersionName,
        appAgentAlias,
        appMappings,
        evaluators,
        tagIds,
        rows);
  }

  private EvalTask toEvalTask(String taskId, NormalizedTask normalized) {
    EvalTask task = new EvalTask();
    task.setId(taskId);
    task.setTaskName(normalized.taskName());
    task.setStatus(STATUS_PENDING);
    task.setDescription(normalized.description());
    task.setDatasetId(normalized.datasetId());
    task.setDatasetVersionId(normalized.datasetVersionId());
    task.setItemCount(normalized.rows().size());
    task.setAppType(normalized.appType());
    task.setAppId(normalized.appId());
    task.setAppName(normalized.appName());
    task.setAppVersionId(normalized.appVersionId());
    task.setAppVersionName(normalized.appVersionName());
    task.setAppAgentAlias(normalized.appAgentAlias());
    task.setStartedAt("");
    task.setFinishedAt("");
    return task;
  }

  private List<AppFieldMappingInput> normalizeAppMappings(
      String appType,
      List<AppFieldMappingInput> mappings,
      Map<String, FieldDto> fieldById
  ) {
    if (!APP_AGENT.equals(appType)) {
      return List.of();
    }
    if (mappings == null || mappings.isEmpty()) {
      return List.of();
    }
    List<AppFieldMappingInput> normalized = new ArrayList<>();
    Set<String> appInputNames = new HashSet<>();
    for (AppFieldMappingInput mapping : mappings) {
      if (mapping == null || !StringUtils.hasText(mapping.appInputName())) {
        continue;
      }
      String appInputName = mapping.appInputName().trim();
      if (!appInputNames.add(appInputName)) {
        throw new IllegalArgumentException("应用入参不能重复映射");
      }
      String appInputType = StringUtils.hasText(mapping.appInputType()) ? mapping.appInputType().trim() : "string";
      if (!SUPPORTED_FIELD_TYPES.contains(appInputType)) {
        throw new IllegalArgumentException("应用入参类型仅支持string/number/boolean");
      }
      String fieldId = requireText(mapping.datasetFieldId(), "请选择应用入参映射的评测集字段");
      if (!fieldById.containsKey(fieldId)) {
        throw new IllegalArgumentException("应用入参映射的评测集字段不存在");
      }
      normalized.add(new AppFieldMappingInput(
          mapping.appInputId() == null ? "" : mapping.appInputId().trim(),
          appInputName,
          appInputType,
          fieldId));
    }
    return normalized;
  }

  private List<NormalizedEvaluator> normalizeEvaluators(
      String appType,
      List<TaskEvaluatorInput> evaluators,
      Map<String, FieldDto> fieldById,
      String datasetVersionId
  ) {
    if (evaluators == null || evaluators.isEmpty()) {
      return List.of();
    }
    if (evaluators.size() > MAX_DIMENSION_COUNT) {
      throw new IllegalArgumentException("评估器最多添加5个");
    }
    List<NormalizedEvaluator> normalized = new ArrayList<>();
    Set<String> duplicateGuard = new HashSet<>();
    int order = 1;
    for (TaskEvaluatorInput input : evaluators) {
      if (input == null) {
        continue;
      }
      String source = normalizeEvaluatorSource(input.evaluatorSource());
      String evaluatorId = requireText(input.evaluatorId(), "请选择评估器");
      EvaluatorRuntimeDefinition definition;
      String evaluatorVersionId = "";
      String modelId = "";
      String modelName = "";
      if (EVALUATOR_PRESET.equals(source)) {
        PresetEvaluatorDetail preset = evaluatorService.getPresetEvaluator(evaluatorId);
        if ("code".equals(preset.evaluatorType())) {
          throw new IllegalArgumentException("暂不支持Code型评估器");
        }
        if ("llm".equals(preset.evaluatorType())) {
          modelId = requireText(input.modelId(), "\u8bf7\u9009\u62e9\u9884\u7f6e\u8bc4\u4f30\u5668\u6a21\u578b");
          modelName = requireText(input.modelName(), "请选择预置评估器模型");
        }
        definition = new EvaluatorRuntimeDefinition(
            preset.evaluatorName(),
            preset.evaluatorType(),
            preset.scoreMin(),
            preset.scoreMax(),
            preset.passThreshold(),
            preset.params());
      } else {
        evaluatorVersionId = requireText(input.evaluatorVersionId(), "请选择自定义评估器版本");
        EvaluatorConfig config = evaluatorService.getVersion(evaluatorVersionId);
        if (Boolean.TRUE.equals(config.draft())) {
          throw new IllegalArgumentException("评测任务请选择已发布的自定义评估器版本");
        }
        modelId = config.modelId();
        modelName = config.modelName();
        if ("code".equals(config.evaluatorType())) {
          throw new IllegalArgumentException("暂不支持Code型评估器");
        }
        if (!evaluatorId.equals(config.evaluatorId())) {
          throw new IllegalArgumentException("自定义评估器版本不属于所选评估器");
        }
        definition = new EvaluatorRuntimeDefinition(
            config.evaluatorName(),
            config.evaluatorType(),
            config.scoreMin(),
            config.scoreMax(),
            config.passThreshold(),
            config.params());
      }
      String duplicateKey = source + ":" + evaluatorId + ":" + evaluatorVersionId;
      if (!duplicateGuard.add(duplicateKey)) {
        throw new IllegalArgumentException("评估器不能重复添加");
      }
      List<NormalizedParamMapping> paramMappings = normalizeParamMappings(
          appType,
          definition.params(),
          input.paramMappings(),
          fieldById,
          datasetVersionId);
      normalized.add(new NormalizedEvaluator(source, evaluatorId, evaluatorVersionId, modelId, modelName, definition, paramMappings, order++));
    }
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("请至少添加一个评估器");
    }
    return normalized;
  }

  private List<NormalizedParamMapping> normalizeParamMappings(
      String appType,
      List<EvaluatorParamDto> params,
      List<TaskEvaluatorParamMappingInput> mappings,
      Map<String, FieldDto> fieldById,
      String datasetVersionId
  ) {
    Map<String, TaskEvaluatorParamMappingInput> provided = new LinkedHashMap<>();
    if (mappings != null) {
      for (TaskEvaluatorParamMappingInput mapping : mappings) {
        if (mapping == null) {
          continue;
        }
        String key = paramKey(mapping.paramId(), mapping.paramName());
        if (StringUtils.hasText(key)) {
          provided.put(key, mapping);
        }
      }
    }
    List<NormalizedParamMapping> normalized = new ArrayList<>();
    int order = 1;
    for (EvaluatorParamDto param : params) {
      String key = paramKey(param.id(), param.paramName());
      TaskEvaluatorParamMappingInput mapping = provided.get(key);
      if (Boolean.TRUE.equals(param.required()) && mapping == null) {
        throw new IllegalArgumentException("请完成评估器必填字段映射：" + param.paramName());
      }
      if (mapping == null) {
        continue;
      }
      if (!Boolean.TRUE.equals(param.required()) && isBlankOptionalParamMapping(mapping)) {
        continue;
      }
      String sourceType = normalizeSourceType(mapping.sourceType());
      String datasetFieldId = "";
      String appOutputName = "";
      if (SOURCE_DATASET_FIELD.equals(sourceType)) {
        datasetFieldId = requireText(mapping.datasetFieldId(), "请选择评测集字段");
        if (!fieldById.containsKey(datasetFieldId)) {
          throw new IllegalArgumentException("评估器字段映射的评测集字段不存在");
        }
      } else {
        if (!APP_AGENT.equals(appType)) {
          throw new IllegalArgumentException("未关联应用时不能映射到应用输出");
        }
        appOutputName = mapping.appOutputName() == null ? "" : mapping.appOutputName().trim();
      }
      normalized.add(new NormalizedParamMapping(
          param.id() == null ? "" : param.id(),
          param.paramName(),
          sourceType,
          datasetVersionId,
          datasetFieldId,
          appOutputName,
          order++));
    }
    return normalized;
  }

  private boolean isBlankOptionalParamMapping(TaskEvaluatorParamMappingInput mapping) {
    if (mapping == null || !StringUtils.hasText(mapping.sourceType())) {
      return true;
    }
    return SOURCE_DATASET_FIELD.equals(mapping.sourceType().trim())
        && !StringUtils.hasText(mapping.datasetFieldId());
  }

  private List<String> normalizeTags(List<String> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return List.of();
    }
    if (tagIds.size() > MAX_DIMENSION_COUNT) {
      throw new IllegalArgumentException("标签最多添加5个");
    }
    List<String> normalized = new ArrayList<>();
    Set<String> duplicateGuard = new HashSet<>();
    for (String tagId : tagIds) {
      String id = requireText(tagId, "请选择标签");
      if (!duplicateGuard.add(id)) {
        throw new IllegalArgumentException("标签不能重复添加");
      }
      if (tagRepository.findTagConfig(id) == null) {
        throw new IllegalArgumentException("标签不存在");
      }
      normalized.add(id);
    }
    return normalized;
  }

  private void saveAppMappings(String taskId, NormalizedTask task, String now) {
    int order = 1;
    for (AppFieldMappingInput mapping : task.appMappings()) {
      taskRepository.insertAppFieldMapping(
          id(),
          taskId,
          mapping.appInputId(),
          mapping.appInputName(),
          mapping.appInputType(),
          task.datasetVersionId(),
          mapping.datasetFieldId(),
          order++,
          now);
    }
  }

  private List<String> saveEvaluators(String taskId, NormalizedTask task, String now) {
    List<String> taskEvaluatorIds = new ArrayList<>();
    for (NormalizedEvaluator evaluator : task.evaluators()) {
      String taskEvaluatorId = id();
      taskEvaluatorIds.add(taskEvaluatorId);
      taskRepository.insertTaskEvaluator(
          taskEvaluatorId,
          taskId,
          evaluator.source(),
          evaluator.evaluatorId(),
          evaluator.evaluatorVersionId(),
          evaluator.modelId(),
          evaluator.modelName(),
          STATUS_PENDING,
          evaluator.displayOrder(),
          now);
      for (NormalizedParamMapping mapping : evaluator.paramMappings()) {
        taskRepository.insertParamMapping(
            id(),
            taskId,
            taskEvaluatorId,
            mapping.paramId(),
            mapping.paramName(),
            mapping.sourceType(),
            mapping.datasetVersionId(),
            mapping.datasetFieldId(),
            mapping.appOutputName(),
            mapping.displayOrder(),
            now);
      }
    }
    return taskEvaluatorIds;
  }

  private List<String> saveTags(String taskId, NormalizedTask task, String now) {
    List<String> taskTagIds = new ArrayList<>();
    int order = 1;
    for (String tagId : task.tagIds()) {
      String taskTagId = id();
      taskTagIds.add(taskTagId);
      taskRepository.insertTaskTag(taskTagId, taskId, tagId, STATUS_PENDING, order++, now);
    }
    return taskTagIds;
  }

  private void saveTaskItems(
      String taskId,
      NormalizedTask task,
      List<String> taskEvaluatorIds,
      List<String> taskTagIds,
      String now
  ) {
    String appOutputStatus = APP_AGENT.equals(task.appType()) ? STATUS_PENDING : RESULT_SKIPPED;
    for (DatasetRowRecord row : task.rows()) {
      String taskItemId = id();
      taskRepository.insertTaskItem(
          taskItemId,
          taskId,
          task.datasetVersionId(),
          row.id(),
          row.rowNo(),
          STATUS_PENDING,
          appOutputStatus,
          now);
      for (String taskEvaluatorId : taskEvaluatorIds) {
        taskRepository.insertEvaluatorResult(id(), taskId, taskItemId, taskEvaluatorId, STATUS_PENDING, now);
      }
      for (String taskTagId : taskTagIds) {
        taskRepository.insertTagResult(id(), taskId, taskItemId, taskTagId, STATUS_PENDING, now);
      }
    }
  }

  private TaskSummary toSummary(TaskBase base) {
    return new TaskSummary(
        base,
        buildEvaluatorDimensions(base.id()),
        taskRepository.listTagDimensions(base.id()));
  }

  private List<TaskEvaluatorDimension> buildEvaluatorDimensions(String taskId) {
    return taskRepository.listEvaluatorDimensions(taskId)
        .stream()
        .map(this::attachPresetEvaluatorDisplay)
        .toList();
  }

  private List<TaskEvaluatorDimension> buildEvaluatorDimensions(String taskId, Map<String, FieldDto> fieldById) {
    Map<String, List<TaskEvaluatorParamMappingRecord>> mappingsByEvaluator = taskRepository.listAllParamMappings(taskId)
        .stream()
        .collect(Collectors.groupingBy(TaskEvaluatorParamMappingRecord::taskEvaluatorId));
    return buildEvaluatorDimensions(taskId)
        .stream()
        .map(dimension -> dimension.withParams(buildParamDisplays(
            mappingsByEvaluator.getOrDefault(dimension.taskEvaluatorId(), List.of()),
            fieldById,
            Map.of(),
            Map.of())))
        .toList();
  }

  private List<TaskMetricDimensionScore> buildMetricDimensions(String taskId) {
    List<TaskMetricDimensionScore> dimensions = new ArrayList<>();
    buildEvaluatorDimensions(taskId).forEach(evaluator -> dimensions.add(toEvaluatorMetric(evaluator)));
    taskRepository.listTagDimensions(taskId).forEach(tag -> dimensions.add(toTagMetric(tag)));
    return dimensions;
  }

  private TaskMetricDimensionScore toEvaluatorMetric(TaskEvaluatorDimension evaluator) {
    int passCount = safeCount(evaluator.passCount());
    int completedCount = safeCount(evaluator.completedCount());
    int totalCount = safeCount(evaluator.totalCount());
    return new TaskMetricDimensionScore(
        METRIC_EVALUATOR,
        evaluator.taskEvaluatorId(),
        safeText(evaluator.evaluatorName()),
        safeText(evaluator.versionName()),
        metricDisplayName(evaluator.evaluatorName(), evaluator.versionName()),
        passCount,
        failedCount(passCount, completedCount),
        pendingCount(completedCount, totalCount),
        completedCount,
        totalCount,
        evaluator.passRate(),
        evaluator.displayOrder());
  }

  private TaskMetricDimensionScore toTagMetric(TaskTagDimension tag) {
    int passCount = safeCount(tag.passCount());
    int completedCount = safeCount(tag.completedCount());
    int totalCount = safeCount(tag.totalCount());
    return new TaskMetricDimensionScore(
        METRIC_TAG,
        tag.taskTagId(),
        safeText(tag.tagName()),
        safeText(tag.tagType()),
        safeText(tag.tagName()),
        passCount,
        failedCount(passCount, completedCount),
        pendingCount(completedCount, totalCount),
        completedCount,
        totalCount,
        tag.passRate(),
        tag.displayOrder());
  }

  private String metricDisplayName(String name, String versionName) {
    if (!StringUtils.hasText(versionName)) {
      return safeText(name);
    } else {
      return safeText(name) + " / " + versionName;
    }
  }

  private String safeText(String value) {
    return StringUtils.hasText(value) ? value : "-";
  }

  private int safeCount(Integer value) {
    return value == null ? 0 : value;
  }

  private int failedCount(int passCount, int completedCount) {
    return Math.max(completedCount - passCount, 0);
  }

  private int pendingCount(int completedCount, int totalCount) {
    return Math.max(totalCount - completedCount, 0);
  }

  private long sumMetricPassCount(List<TaskMetricDimensionScore> dimensions) {
    return dimensions.stream()
        .map(TaskMetricDimensionScore::passCount)
        .mapToLong(this::safeCount)
        .sum();
  }

  private long sumMetricFailedCount(List<TaskMetricDimensionScore> dimensions) {
    return dimensions.stream()
        .map(TaskMetricDimensionScore::failedCount)
        .mapToLong(this::safeCount)
        .sum();
  }

  private long countScoredDimensions(List<TaskMetricDimensionScore> dimensions) {
    return dimensions.stream()
        .filter(dimension -> safeCount(dimension.passCount()) + safeCount(dimension.failedCount()) > 0)
        .count();
  }

  private Double percent(long numerator, long denominator) {
    if (denominator <= 0) {
      return null;
    } else {
      return roundPercent(numerator * 100.0 / denominator);
    }
  }

  private Double roundPercent(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  private TaskEvaluatorDimension attachPresetEvaluatorDisplay(TaskEvaluatorDimension dimension) {
    if (!EVALUATOR_PRESET.equals(dimension.evaluatorSource())) {
      return dimension;
    }
    PresetEvaluatorDetail preset = findPresetQuietly(dimension.evaluatorId());
    if (preset == null) {
      return dimension;
    }
    return dimension.withDisplay(preset.evaluatorName(), preset.evaluatorType(), "预置");
  }

  private List<TaskItemDetail> buildItems(List<TaskItemRecord> itemRecords, Map<String, FieldDto> fieldById) {
    if (itemRecords.isEmpty()) {
      return List.of();
    }
    List<String> taskItemIds = itemRecords.stream().map(TaskItemRecord::id).toList();
    Map<String, TaskEvaluatorBindingRecord> evaluatorBindings = taskRepository.listTaskEvaluatorBindings(itemRecords.getFirst().taskId())
        .stream()
        .collect(Collectors.toMap(TaskEvaluatorBindingRecord::id, Function.identity()));
    Map<String, List<TaskEvaluatorParamMappingRecord>> mappingsByEvaluator = taskRepository.listAllParamMappings(itemRecords.getFirst().taskId())
        .stream()
        .collect(Collectors.groupingBy(TaskEvaluatorParamMappingRecord::taskEvaluatorId));
    Map<String, Map<String, String>> valuesByDatasetItem = loadDatasetValues(itemRecords);
    Map<String, List<TaskEvaluatorResultDto>> evaluatorResults = taskRepository.listEvaluatorResultsByTaskItemIds(taskItemIds)
        .stream()
        .map(result -> attachPresetEvaluatorResultDisplay(result, evaluatorBindings))
        .collect(Collectors.groupingBy(TaskEvaluatorResultDto::taskItemId));
    Map<String, List<TaskTagResultDto>> tagResults = taskRepository.listTagResultsByTaskItemIds(taskItemIds)
        .stream()
        .collect(Collectors.groupingBy(TaskTagResultDto::taskItemId));
    return itemRecords.stream()
        .map(item -> new TaskItemDetail(
            item.id(),
            item.datasetItemId(),
            item.rowNo(),
            item.status(),
            valuesByDatasetItem.getOrDefault(item.datasetItemId(), Map.of()),
            item.appOutput(),
            item.appOutputStatus(),
            item.appErrorMessage(),
            attachEvaluatorParamValues(
                evaluatorResults.getOrDefault(item.id(), List.of()),
                mappingsByEvaluator,
                fieldById,
                valuesByDatasetItem.getOrDefault(item.datasetItemId(), Map.of()),
                extractStoredAppOutputs(item.appOutput())),
            tagResults.getOrDefault(item.id(), List.of()),
            item.createdDate(),
            item.lastUpdatedDate()))
        .toList();
  }

  private TaskEvaluatorResultDto attachPresetEvaluatorResultDisplay(
      TaskEvaluatorResultDto result,
      Map<String, TaskEvaluatorBindingRecord> evaluatorBindings
  ) {
    TaskEvaluatorBindingRecord binding = evaluatorBindings.get(result.taskEvaluatorId());
    if (binding == null || !EVALUATOR_PRESET.equals(binding.evaluatorSource())) {
      return result;
    }
    PresetEvaluatorDetail preset = findPresetQuietly(binding.evaluatorId());
    if (preset == null) {
      return result;
    }
    return result.withDisplay(preset.evaluatorName(), preset.evaluatorType(), "预置");
  }

  private PresetEvaluatorDetail findPresetQuietly(String presetId) {
    try {
      return evaluatorService.getPresetEvaluator(presetId);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private Map<String, FieldDto> toFieldById(List<FieldDto> fields) {
    if (fields == null || fields.isEmpty()) {
      return Map.of();
    } else {
      return fields.stream()
          .filter(field -> field != null && StringUtils.hasText(field.id()))
          .collect(Collectors.toMap(FieldDto::id, Function.identity(), (left, right) -> left));
    }
  }

  private List<TaskEvaluatorResultDto> attachEvaluatorParamValues(
      List<TaskEvaluatorResultDto> results,
      Map<String, List<TaskEvaluatorParamMappingRecord>> mappingsByEvaluator,
      Map<String, FieldDto> fieldById,
      Map<String, String> values,
      Map<String, String> appOutputs
  ) {
    return results.stream()
        .map(result -> result.withParams(buildParamDisplays(
            mappingsByEvaluator.getOrDefault(result.taskEvaluatorId(), List.of()),
            fieldById,
            values,
            appOutputs)))
        .toList();
  }

  private List<TaskEvaluatorParamDisplay> buildParamDisplays(
      List<TaskEvaluatorParamMappingRecord> mappings,
      Map<String, FieldDto> fieldById,
      Map<String, String> values,
      Map<String, String> appOutputs
  ) {
    return mappings.stream()
        .map(mapping -> new TaskEvaluatorParamDisplay(
            mapping.paramId(),
            mapping.paramName(),
            mapping.sourceType(),
            paramSourceName(mapping, fieldById),
            mapping.datasetFieldId(),
            mapping.appOutputName(),
            resolveMappingValue(mapping, values, appOutputs),
            mapping.displayOrder()))
        .toList();
  }

  private String paramSourceName(TaskEvaluatorParamMappingRecord mapping, Map<String, FieldDto> fieldById) {
    if (SOURCE_DATASET_FIELD.equals(mapping.sourceType())) {
      FieldDto field = fieldById.get(mapping.datasetFieldId());
      if (field == null) {
        return firstNonBlank(mapping.datasetFieldId(), "评测集字段");
      } else {
        return field.fieldName();
      }
    } else {
      return firstNonBlank(mapping.appOutputName(), "应用输出");
    }
  }

  private Map<String, String> extractStoredAppOutputs(String appOutput) {
    if (!StringUtils.hasText(appOutput)) {
      return Map.of();
    } else {
      Map<String, String> outputs = new LinkedHashMap<>();
      boolean parsedJson = putStoredJsonOutputs(outputs, appOutput);
      if (!parsedJson) {
        putIfText(outputs, "rawText", appOutput);
        putIfText(outputs, "text", appOutput);
      }
      putIfText(outputs, "answer", firstNonBlank(outputs.get("answer"), outputs.get("text"), outputs.get("content")));
      putIfText(outputs, "content", firstNonBlank(outputs.get("content"), outputs.get("text"), outputs.get("answer")));
      return outputs;
    }
  }

  private boolean putStoredJsonOutputs(Map<String, String> outputs, String appOutput) {
    try {
      JsonNode root = objectMapper.readTree(appOutput);
      if (root != null && root.isObject()) {
        root.fields().forEachRemaining(entry -> putIfText(outputs, entry.getKey(), jsonNodeDisplayValue(entry.getValue())));
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  private String jsonNodeDisplayValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return "";
    } else if (node.isTextual()) {
      return node.asText();
    } else if (node.isNumber() || node.isBoolean()) {
      return node.asText();
    } else {
      return prettyJson(node);
    }
  }

  private String prettyJson(JsonNode node) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    } catch (Exception e) {
      return node.toString();
    }
  }

  private Map<String, Map<String, String>> loadDatasetValues(List<TaskItemRecord> itemRecords) {
    List<String> datasetItemIds = itemRecords.stream().map(TaskItemRecord::datasetItemId).toList();
    return datasetRepository.loadValues(datasetItemIds);
  }

  private List<TaskTagAnnotation> buildTagAnnotations(String taskId, TaskItemRecord itemRecord) {
    List<TaskTagResultDto> results = taskRepository.listTagResultsByTaskItemIds(List.of(itemRecord.id()));
    Map<String, TaskTagResultDto> resultByTaskTag = results.stream()
        .collect(Collectors.toMap(TaskTagResultDto::taskTagId, Function.identity()));
    return taskRepository.listTaskTagBindings(taskId).stream()
        .map(tag -> new TaskTagAnnotation(
            tag.id(),
            tag.tagId(),
            tag.tagName(),
            tag.tagType(),
            tag.description(),
            tag.minValue(),
            tag.maxValue(),
            tag.passThreshold(),
            tagRepository.listOptions(tag.tagId()),
            resultByTaskTag.get(tag.id())))
        .toList();
  }

  private Map<String, EvaluationRuntimeConfig> loadEvaluatorRuntimeConfigs(List<TaskEvaluatorBindingRecord> evaluators) {
    Map<String, EvaluationRuntimeConfig> configs = new HashMap<>();
    for (TaskEvaluatorBindingRecord evaluator : evaluators) {
      if (EVALUATOR_PRESET.equals(evaluator.evaluatorSource())) {
        PresetEvaluatorDetail preset = evaluatorService.getPresetEvaluator(evaluator.evaluatorId());
        configs.put(evaluator.id(), new EvaluationRuntimeConfig(
            preset.evaluatorName(),
            preset.evaluatorType(),
            evaluator.modelId(),
            evaluator.modelName(),
            preset.prompt(),
            preset.executeCode(),
            preset.scoreMin(),
            preset.scoreMax(),
            preset.passThreshold(),
            preset.params()));
      } else {
        EvaluatorConfig config = evaluatorService.getVersion(evaluator.evaluatorVersionId());
        configs.put(evaluator.id(), new EvaluationRuntimeConfig(
            config.evaluatorName(),
            config.evaluatorType(),
            evaluator.modelId(),
            evaluator.modelName(),
            config.prompt(),
            config.executeCode(),
            config.scoreMin(),
            config.scoreMax(),
            config.passThreshold(),
            config.params()));
      }
    }
    return configs;
  }

  private AgentInvocationResult failedAgentResult(String message) {
    String error = StringUtils.hasText(message) ? message : "Agent execution failed";
    Map<String, String> outputs = Map.of("error", error, "rawText", error);
    return new AgentInvocationResult(serializeAgentOutputs(outputs), STATUS_FAILED, error, outputs);
  }

  private EvaluationSimulationResult failedEvaluationResult(String message) {
    return new EvaluationSimulationResult(
        STATUS_FAILED,
        null,
        "",
        "",
        StringUtils.hasText(message) ? message : "Evaluator execution failed");
  }

  private AgentInvocationResult invokeAgent(
      TaskBase base,
      TaskItemRecord item,
      List<TaskAppFieldMappingRecord> appMappings,
      Map<String, String> values
  ) {
    if (!APP_AGENT.equals(base.appType())) {
      return new AgentInvocationResult("", RESULT_SKIPPED, "", Map.of());
    }
    String content = buildAgentMessageContent(appMappings, values);
    AgentChatResponse response = remoteCallService.invokeAgent(
        base.appId(),
        base.appVersionId(),
        base.appAgentAlias(),
        new AgentChatRequest(
            base.id() + "-" + item.id(),
            List.of(new AgentMessage("user", content)),
            true));
    if (response == null) {
      return failedAgentResult("Super智能体未返回结果");
    }
    Map<String, String> outputs = extractAgentOutputs(response);
    String storedContent = serializeAgentOutputs(outputs);
    if (STATUS_FAILED.equals(response.status())) {
      return new AgentInvocationResult(
          storedContent,
          STATUS_FAILED,
          firstNonBlank(response.errorMessage(), outputs.get("error"), outputs.get("rawText")),
          outputs);
    }
    if (StringUtils.hasText(outputs.get("error"))) {
      return new AgentInvocationResult(
          storedContent,
          STATUS_FAILED,
          outputs.get("error"),
          outputs);
    }
    return new AgentInvocationResult(
        storedContent,
        STATUS_COMPLETED,
        "",
        outputs);
  }

  private String buildAgentMessageContent(
      List<TaskAppFieldMappingRecord> appMappings,
      Map<String, String> values
  ) {
    Map<String, String> inputs = new LinkedHashMap<>();
    if (appMappings != null && !appMappings.isEmpty()) {
      for (TaskAppFieldMappingRecord mapping : appMappings) {
        inputs.put(mapping.appInputName(), values.getOrDefault(mapping.datasetFieldId(), ""));
      }
    } else {
      inputs.putAll(values);
    }
    if (inputs.isEmpty()) {
      return "空输入";
    }
    return inputs.entrySet().stream()
        .map(entry -> entry.getKey() + ": " + entry.getValue())
        .collect(Collectors.joining("\n"));
  }

  private EvaluationSimulationResult evaluateWithRemoteCall(
      EvaluationRuntimeConfig config,
      List<TaskEvaluatorParamMappingRecord> mappings,
      Map<String, String> values,
      Map<String, String> appOutputs
  ) {
    if (config == null) {
      return new EvaluationSimulationResult(
          STATUS_FAILED,
          null,
          "",
          "",
          "评估器配置不存在");
    }
    if ("code".equals(config.evaluatorType())) {
      return new EvaluationSimulationResult(
          STATUS_FAILED,
          null,
          "",
          "",
          "Code评估器暂未接入真实代码执行接口");
    }
    if (!"llm".equals(config.evaluatorType())) {
      return new EvaluationSimulationResult(
          STATUS_FAILED,
          null,
          "",
          "",
          "评估器类型不支持：" + config.evaluatorType());
    }
    if (!StringUtils.hasText(config.modelId())) {
      return new EvaluationSimulationResult(
          STATUS_FAILED,
          null,
          "",
          "",
          "LLM评估器未绑定模型");
    }
    if (!StringUtils.hasText(config.modelName())) {
      return new EvaluationSimulationResult(
          STATUS_FAILED,
          null,
          "",
          "",
          "LLM评估器未绑定模型名称");
    }
    Map<String, Object> preparedParams = prepareEvaluationInput(config, mappings, values, appOutputs);
    String renderedPrompt = renderPrompt(config.prompt(), preparedParams);
    ModelChatResult response = remoteCallService.chatModel(config.modelId(), config.modelName(), renderedPrompt);
    if (response == null || !StringUtils.hasText(response.outputText())) {
      return new EvaluationSimulationResult(
          STATUS_FAILED,
          null,
          "",
          "",
          "模型对话接口未返回评估结果");
    }
    EvaluationParseResult parsed = parseEvaluationOutput(response.outputText());
    if (StringUtils.hasText(parsed.errorMessage())) {
      return new EvaluationSimulationResult(
          STATUS_FAILED,
          null,
          "",
          response.outputText(),
          parsed.errorMessage());
    }
    BigDecimal score = parsed.score();
    if (score == null) {
      return new EvaluationSimulationResult(
          STATUS_FAILED,
          null,
          "",
          "",
          "模型评估结果中的score为空");
    }
    String resultValue = score.compareTo(config.scoreMin()) < 0 || score.compareTo(config.scoreMax()) > 0
        ? appendEvaluationNotice(parsed.reason(), "模型评估结果中的score超出评分范围")
        : parsed.reason();
    String passResult = score.compareTo(config.passThreshold()) >= 0 ? "pass" : "fail";
    return new EvaluationSimulationResult(
        STATUS_COMPLETED,
        score,
        passResult,
        resultValue,
        "");
  }

  private String appendEvaluationNotice(String reason, String notice) {
    if (!StringUtils.hasText(notice)) {
      return reason == null ? "" : reason;
    }
    if (!StringUtils.hasText(reason)) {
      return notice;
    }
    return reason + "\n" + notice;
  }

  private Map<String, Object> prepareEvaluationInput(
      EvaluationRuntimeConfig config,
      List<TaskEvaluatorParamMappingRecord> mappings,
      Map<String, String> values,
      Map<String, String> appOutputs
  ) {
    Map<String, Object> params = new LinkedHashMap<>();
    Map<String, TaskEvaluatorParamMappingRecord> mappingByParam = mappings.stream()
        .collect(Collectors.toMap(mapping -> paramKey(mapping.paramId(), mapping.paramName()), Function.identity(), (a, b) -> a));
    for (EvaluatorParamDto param : config.params()) {
      TaskEvaluatorParamMappingRecord mapping = mappingByParam.get(paramKey(param.id(), param.paramName()));
      String value = resolveMappingValue(mapping, values, appOutputs);
      if (!StringUtils.hasText(value) && StringUtils.hasText(param.defaultValue())) {
        value = param.defaultValue();
      }
      if (!StringUtils.hasText(value)) {
        value = "";
      }
      params.put(param.paramName(), value);
    }
    return params;
  }

  private String resolveMappingValue(
      TaskEvaluatorParamMappingRecord mapping,
      Map<String, String> values,
      Map<String, String> appOutputs
  ) {
    if (mapping == null) {
      return "";
    }
    if (SOURCE_APP_OUTPUT.equals(mapping.sourceType())) {
      if (!StringUtils.hasText(mapping.appOutputName())) {
        return "";
      } else {
        return appOutputs.getOrDefault(mapping.appOutputName(), "");
      }
    }
    return values.getOrDefault(mapping.datasetFieldId(), "");
  }

  private Map<String, String> extractAgentOutputs(AgentChatResponse response) {
    Map<String, String> outputs = new LinkedHashMap<>();
    if (response.outputs() != null) {
      response.outputs().forEach((key, value) -> {
        if (StringUtils.hasText(key)) {
          outputs.put(key, value == null ? "" : value);
        }
      });
    }
    putIfText(outputs, "rawText", firstNonBlank(outputs.get("rawText"), response.rawOutput()));
    putIfText(outputs, "answer", firstNonBlank(outputs.get("answer"), outputs.get("text"), outputs.get("content")));
    putIfText(outputs, "content", firstNonBlank(outputs.get("content"), outputs.get("text"), outputs.get("answer")));
    return outputs;
  }

  private String formatAgentOutputs(Map<String, String> outputs) {
    return AgentOutputFormatter.toDisplayText(outputs);
  }

  private String serializeAgentOutputs(Map<String, String> outputs) {
    if (outputs == null || outputs.isEmpty()) {
      return "";
    }
    try {
      return objectMapper.writeValueAsString(outputs);
    } catch (Exception e) {
      return formatAgentOutputs(outputs);
    }
  }

  private void putIfText(Map<String, String> outputs, String key, String value) {
    if (StringUtils.hasText(value)) {
      outputs.put(key, value);
    }
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return "";
  }

  private String firstNonEmpty(String... values) {
    for (String value : values) {
      if (value != null && !value.isEmpty()) {
        return value;
      }
    }
    return "";
  }

  private String renderPrompt(String prompt, Map<String, Object> params) {
    if (!StringUtils.hasText(prompt)) {
      return "";
    }
    Matcher matcher = PROMPT_PARAM_PATTERN.matcher(prompt);
    StringBuffer rendered = new StringBuffer();
    while (matcher.find()) {
      Object value = params.get(matcher.group(1));
      matcher.appendReplacement(rendered, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
    }
    matcher.appendTail(rendered);
    return rendered.toString();
  }

  private EvaluationParseResult parseEvaluationOutput(String outputText) {
    if (!StringUtils.hasText(outputText)) {
      return new EvaluationParseResult(null, "", "模型评估结果为空");
    }
    String json = extractJson(outputText);
    if (!StringUtils.hasText(json)) {
      return new EvaluationParseResult(null, "", "模型评估结果不是JSON格式");
    }
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode scoreNode = root.get("score");
      if (scoreNode == null || scoreNode.isNull()) {
        return new EvaluationParseResult(null, "", "模型评估结果缺少score字段");
      }
      BigDecimal score = scoreNode.isNumber()
          ? scoreNode.decimalValue()
          : new BigDecimal(scoreNode.asText().trim());
      String reason = root.hasNonNull("reason") ? root.get("reason").asText() : outputText;
      return new EvaluationParseResult(score, reason, "");
    } catch (Exception e) {
      return new EvaluationParseResult(null, "", "模型评估结果解析失败：" + e.getMessage());
    }
  }

  private String extractJson(String outputText) {
    String trimmed = outputText == null ? "" : outputText.trim();
    if (trimmed.startsWith("```")) {
      trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
      int fenceIndex = trimmed.lastIndexOf("```");
      if (fenceIndex >= 0) {
        trimmed = trimmed.substring(0, fenceIndex).trim();
      }
    }
    int start = trimmed.indexOf('{');
    int end = trimmed.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return trimmed.substring(start, end + 1);
    }
    return trimmed.startsWith("{") && trimmed.endsWith("}") ? trimmed : "";
  }

  private NormalizedAnnotation normalizeAnnotation(TaskTagBindingRecord tag, TagAnnotationInput input) {
    if ("text".equals(tag.tagType())) {
      String value = requireText(input.valueText(), "请输入文本标签：" + tag.tagName());
      return new NormalizedAnnotation(value, null, "", "pass");
    }
    if ("number".equals(tag.tagType())) {
      BigDecimal value = input.valueNumber();
      if (value == null) {
        throw new IllegalArgumentException("请输入数字标签：" + tag.tagName());
      }
      if (tag.minValue() != null && value.compareTo(BigDecimal.valueOf(tag.minValue())) < 0) {
        throw new IllegalArgumentException("数字标签不能小于最小值：" + tag.tagName());
      }
      if (tag.maxValue() != null && value.compareTo(BigDecimal.valueOf(tag.maxValue())) > 0) {
        throw new IllegalArgumentException("数字标签不能大于最大值：" + tag.tagName());
      }
      String passResult = tag.passThreshold() != null && value.compareTo(BigDecimal.valueOf(tag.passThreshold())) >= 0 ? "pass" : "fail";
      return new NormalizedAnnotation("", value, "", passResult);
    }
    String optionId = requireText(input.tagOptionId(), "请选择标签选项：" + tag.tagName());
    TagOptionDto option = tagRepository.listOptions(tag.tagId()).stream()
        .filter(item -> optionId.equals(item.id()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("标签选项不存在：" + tag.tagName()));
    String optionName = normalizeBooleanOptionText(tag.tagType(), option.optionName());
    return new NormalizedAnnotation(optionName, null, option.id(), option.optionGroup());
  }

  private void ensureTaskKeepsEvaluationDimension(String taskId, String removedTaskTagId) {
    boolean hasEvaluator = !taskRepository.listTaskEvaluatorBindings(taskId).isEmpty();
    boolean hasRemainingTag = taskRepository.listTaskTagBindings(taskId)
        .stream()
        .anyMatch(tag -> !Objects.equals(tag.id(), removedTaskTagId));
    if (!hasEvaluator && !hasRemainingTag) {
      throw new IllegalArgumentException("评测任务至少需要保留一个评估器或标签");
    }
  }

  private void refreshTaskAfterTagChange(TaskBase task, String now) {
    if (!List.of(STATUS_RUNNING, STATUS_COMPLETED).contains(task.status())) {
      taskRepository.updateTaskStatus(task.id(), task.status(), null, null, now);
    } else {
      for (TaskItemRecord item : taskRepository.listAllTaskItems(task.id())) {
        refreshTaskItemStatusAfterTagChange(item.id(), now);
      }
      String status = resolveFinalTaskStatus(task.id(), false);
      String finishedAt = STATUS_COMPLETED.equals(status) ? now : "";
      taskRepository.updateTaskStatus(task.id(), status, null, finishedAt, now);
    }
  }

  private void refreshTaskItemStatusAfterTagChange(String taskItemId, String now) {
    int unfinishedEvaluators = taskRepository.countUnfinishedEvaluatorResultsByItem(taskItemId);
    int unfinishedTags = taskRepository.countUnfinishedTagResultsByItem(taskItemId);
    if (unfinishedEvaluators == 0 && unfinishedTags == 0) {
      taskRepository.updateTaskItemStatus(taskItemId, STATUS_COMPLETED, now);
    } else if (unfinishedEvaluators == 0) {
      taskRepository.updateTaskItemStatus(taskItemId, ITEM_ANNOTATION_PENDING, now);
    } else {
      // 评估器尚未结束时保留当前数据行状态，避免覆盖失败或执行中状态。
    }
  }

  private String normalizeBooleanOptionText(String tagType, String optionName) {
    if ("boolean".equals(tagType) && StringUtils.hasText(optionName)) {
      return optionName.trim().toUpperCase(Locale.ROOT);
    } else {
      return optionName;
    }
  }

  private void refreshTaskTagStatus(String taskTagId, String now) {
    int total = taskRepository.countTagResults(taskTagId);
    int completed = taskRepository.countCompletedTagResults(taskTagId);
    String status = total > 0 && completed >= total ? STATUS_COMPLETED : STATUS_PENDING;
    taskRepository.updateTaskTagStatus(taskTagId, status, now);
  }

  private void refreshItemAndTaskStatus(String taskId, String taskItemId, String now) {
    TaskBase task = findTask(taskId);
    if (STATUS_STOPPED.equals(task.status())) {
      return;
    }
    int unfinishedEvaluators = taskRepository.countUnfinishedEvaluatorResultsByItem(taskItemId);
    int unfinishedTags = taskRepository.countUnfinishedTagResultsByItem(taskItemId);
    if (unfinishedEvaluators == 0 && unfinishedTags == 0) {
      taskRepository.updateTaskItemStatus(taskItemId, STATUS_COMPLETED, now);
    } else if (unfinishedEvaluators == 0) {
      taskRepository.updateTaskItemStatus(taskItemId, ITEM_ANNOTATION_PENDING, now);
    }
    if (taskRepository.countUnfinishedTaskItems(taskId) == 0) {
      taskRepository.updateTaskStatus(taskId, STATUS_COMPLETED, null, now, now);
    } else {
      if (STATUS_PENDING.equals(task.status())) {
        taskRepository.updateTaskStatus(taskId, STATUS_RUNNING, now, null, now);
      }
    }
  }

  private TaskBase findTask(String taskId) {
    if (!StringUtils.hasText(taskId)) {
      throw new IllegalArgumentException("评测任务ID不能为空");
    }
    TaskBase task = taskRepository.findTaskBase(taskId);
    if (task == null) {
      throw new IllegalArgumentException("评测任务不存在");
    }
    return task;
  }

  private TaskItemRecord findTaskItem(String taskId, String taskItemId) {
    if (!StringUtils.hasText(taskItemId)) {
      throw new IllegalArgumentException("任务数据行ID不能为空");
    }
    TaskItemRecord item = taskRepository.findTaskItem(taskItemId);
    if (item == null || !taskId.equals(item.taskId())) {
      throw new IllegalArgumentException("任务数据行不存在");
    }
    return item;
  }

  private String normalizeOptionalStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return null;
    }
    String normalized = status.trim();
    if (!List.of(STATUS_PENDING, STATUS_RUNNING, STATUS_COMPLETED, STATUS_FAILED, STATUS_STOPPED).contains(normalized)) {
      throw new IllegalArgumentException("评测状态不支持");
    }
    return normalized;
  }

  private String normalizeAppType(String appType) {
    if (!StringUtils.hasText(appType)) {
      return APP_NONE;
    }
    String normalized = appType.trim();
    if (!List.of(APP_NONE, APP_AGENT).contains(normalized)) {
      throw new IllegalArgumentException("应用类型仅支持不关联应用或智能体");
    }
    return normalized;
  }

  private String normalizeDescription(String description) {
    String normalized = description == null ? "" : description.trim();
    if (normalized.length() > 200) {
      throw new IllegalArgumentException("描述不能超过200个字符");
    } else {
      return normalized;
    }
  }

  private String normalizeEvaluatorSource(String source) {
    String normalized = requireText(source, "请选择评估器类型");
    if (!List.of(EVALUATOR_PRESET, EVALUATOR_CUSTOM).contains(normalized)) {
      throw new IllegalArgumentException("评估器类型仅支持预置或自定义");
    }
    return normalized;
  }

  private String normalizeSourceType(String sourceType) {
    String normalized = requireText(sourceType, "请选择字段映射来源");
    if (!List.of(SOURCE_DATASET_FIELD, SOURCE_APP_OUTPUT).contains(normalized)) {
      throw new IllegalArgumentException("字段映射来源不支持");
    }
    return normalized;
  }

  private String requireText(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }

  private String safeErrorMessage(Exception error) {
    if (error == null) {
      return "";
    }
    return StringUtils.hasText(error.getMessage()) ? error.getMessage() : error.getClass().getSimpleName();
  }

  private boolean shouldContinueTaskExecution(String taskId) {
    if (taskRepository.isTaskRunning(taskId)) {
      return true;
    }
    TaskBase task = taskRepository.findTaskBase(taskId);
    if (task != null && STATUS_STOPPED.equals(task.status())) {
      taskRepository.markTaskStoppedChildren(taskId, STATUS_STOPPED, now());
    }
    return false;
  }

  private String paramKey(String paramId, String paramName) {
    if (StringUtils.hasText(paramId)) {
      return "id:" + paramId.trim();
    }
    if (StringUtils.hasText(paramName)) {
      return "name:" + paramName.trim();
    }
    return "";
  }

  private String truncate(String value, int maxLength) {
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  private String savedNameText(String value, String fallback, int maxLength) {
    String source = StringUtils.hasText(value) ? value.trim() : fallback;
    return truncate(source, maxLength);
  }

  private String id() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private String now() {
    return String.valueOf(System.currentTimeMillis());
  }

  private record NormalizedTask(
      String taskName,
      String description,
      String datasetId,
      String datasetVersionId,
      String appType,
      String appId,
      String appName,
      String appVersionId,
      String appVersionName,
      String appAgentAlias,
      List<AppFieldMappingInput> appMappings,
      List<NormalizedEvaluator> evaluators,
      List<String> tagIds,
      List<DatasetRowRecord> rows
  ) {
  }

  private record NormalizedEvaluator(
      String source,
      String evaluatorId,
      String evaluatorVersionId,
      String modelId,
      String modelName,
      EvaluatorRuntimeDefinition definition,
      List<NormalizedParamMapping> paramMappings,
      Integer displayOrder
  ) {
  }

  private record NormalizedParamMapping(
      String paramId,
      String paramName,
      String sourceType,
      String datasetVersionId,
      String datasetFieldId,
      String appOutputName,
      Integer displayOrder
  ) {
  }

  private record EvaluatorRuntimeDefinition(
      String evaluatorName,
      String evaluatorType,
      BigDecimal scoreMin,
      BigDecimal scoreMax,
      BigDecimal passThreshold,
      List<EvaluatorParamDto> params
  ) {
  }

  private record EvaluationRuntimeConfig(
      String evaluatorName,
      String evaluatorType,
      String modelId,
      String modelName,
      String prompt,
      String executeCode,
      BigDecimal scoreMin,
      BigDecimal scoreMax,
      BigDecimal passThreshold,
      List<EvaluatorParamDto> params
  ) {
  }

  private record AgentInvocationResult(
      String content,
      String status,
      String errorMessage,
      Map<String, String> outputs
  ) {
  }

  private static final class TaskExecutionContext {
    private TaskBase base;
    private List<TaskItemRecord> items;
    private List<TaskEvaluatorBindingRecord> evaluators;
    private List<TaskAppFieldMappingRecord> appMappings;
    private Map<String, EvaluationRuntimeConfig> evaluatorConfigs;
    private Map<String, List<TaskEvaluatorParamMappingRecord>> mappingsByEvaluator;
    private Map<String, Map<String, String>> valuesByItem;
    private Map<String, List<TaskEvaluatorResultDto>> evaluatorResultsByItem;
  }

  private static final class TaskItemExecutionContext {
    private TaskExecutionContext task;
    private TaskItemRecord item;
    private Map<String, String> rowValues;
    private Map<String, TaskEvaluatorResultDto> evaluatorResults;
  }

  private record TaskExecutionOutcome(
      boolean hasTaskFailure,
      Set<String> failedEvaluatorIds
  ) {
  }

  private record ItemExecutionResult(
      boolean failed,
      Set<String> failedEvaluatorIds
  ) {
  }

  private record EvaluationParseResult(
      BigDecimal score,
      String reason,
      String errorMessage
  ) {
  }

  private record EvaluationSimulationResult(
      String status,
      BigDecimal score,
      String passResult,
      String resultValue,
      String errorMessage
  ) {
  }

  private record NormalizedAnnotation(
      String valueText,
      BigDecimal valueNumber,
      String tagOptionId,
      String passResult
  ) {
  }
}
