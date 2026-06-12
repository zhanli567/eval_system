package com.evalsystem.task.service.impl;

import com.evalsystem.common.PageResponse;
import com.evalsystem.dataset.dto.DatasetSummary;
import com.evalsystem.dataset.dto.DatasetVersionDto;
import com.evalsystem.dataset.dto.FieldDto;
import com.evalsystem.dataset.mapper.DatasetMapper;
import com.evalsystem.evaluator.dto.EvaluatorConfig;
import com.evalsystem.evaluator.dto.EvaluatorParamDto;
import com.evalsystem.evaluator.dto.PresetEvaluatorDetail;
import com.evalsystem.evaluator.service.EvaluatorService;
import com.evalsystem.integration.dto.PlatformAgentChatRequest;
import com.evalsystem.integration.dto.PlatformAgentChatResponse;
import com.evalsystem.integration.dto.PlatformAgentChoice;
import com.evalsystem.integration.dto.PlatformAgentContentBlock;
import com.evalsystem.integration.dto.PlatformAgentMessage;
import com.evalsystem.integration.dto.PlatformModelChatResult;
import com.evalsystem.integration.service.PlatformIntegrationService;
import com.evalsystem.tag.dto.TagConfig;
import com.evalsystem.tag.dto.TagOptionDto;
import com.evalsystem.tag.mapper.TagMapper;
import com.evalsystem.task.dto.AnnotationDetail;
import com.evalsystem.task.dto.AppFieldMappingInput;
import com.evalsystem.task.dto.CreateTaskRequest;
import com.evalsystem.task.dto.SaveAnnotationRequest;
import com.evalsystem.task.dto.TagAnnotationInput;
import com.evalsystem.task.dto.TaskBase;
import com.evalsystem.task.dto.TaskDetail;
import com.evalsystem.task.dto.TaskEvaluatorDimension;
import com.evalsystem.task.dto.TaskEvaluatorInput;
import com.evalsystem.task.dto.TaskEvaluatorParamMappingInput;
import com.evalsystem.task.dto.TaskEvaluatorResultDto;
import com.evalsystem.task.dto.TaskItemDetail;
import com.evalsystem.task.dto.TaskSummary;
import com.evalsystem.task.dto.TaskTagAnnotation;
import com.evalsystem.task.dto.TaskTagDimension;
import com.evalsystem.task.dto.TaskTagResultDto;
import com.evalsystem.task.mapper.TaskMapper;
import com.evalsystem.task.service.TaskService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.util.StringUtils;

@Service
public class TaskServiceImpl implements TaskService {
  private static final String STATUS_PENDING = "pending";
  private static final String STATUS_RUNNING = "running";
  private static final String STATUS_COMPLETED = "completed";
  private static final String STATUS_TERMINATED = "terminated";
  private static final String STATUS_FAILED = "failed";
  private static final String ITEM_ANNOTATION_PENDING = "annotation_pending";
  private static final String RESULT_SKIPPED = "skipped";
  private static final String APP_NONE = "none";
  private static final String APP_AGENT = "agent";
  private static final String EVALUATOR_PRESET = "preset";
  private static final String EVALUATOR_CUSTOM = "custom";
  private static final String SOURCE_DATASET_FIELD = "dataset_field";
  private static final String SOURCE_APP_OUTPUT = "app_output";
  private static final String DEFAULT_AGENT_ALIAS = "router-agent";
  private static final int MAX_DIMENSION_COUNT = 5;
  private static final Pattern PROMPT_PARAM_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][\\w]*)}");

  private final TaskMapper taskMapper;
  private final DatasetMapper datasetMapper;
  private final EvaluatorService evaluatorService;
  private final TagMapper tagMapper;
  private final PlatformIntegrationService integrationService;
  private final ObjectMapper objectMapper;

  public TaskServiceImpl(
      TaskMapper taskMapper,
      DatasetMapper datasetMapper,
      EvaluatorService evaluatorService,
      TagMapper tagMapper,
      PlatformIntegrationService integrationService,
      ObjectMapper objectMapper
  ) {
    this.taskMapper = taskMapper;
    this.datasetMapper = datasetMapper;
    this.evaluatorService = evaluatorService;
    this.tagMapper = tagMapper;
    this.integrationService = integrationService;
    this.objectMapper = objectMapper;
  }

  public PageResponse<TaskSummary> listTasks(int page, int size, String status, String keyword, String sortBy, String sortOrder) {
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 100);
    int offset = (safePage - 1) * safeSize;
    String normalizedStatus = normalizeOptionalStatus(status);
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    String orderColumn = "createdAt".equals(sortBy) ? "t.created_at" : "t.updated_at";
    String orderDirection = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
    List<TaskSummary> records = taskMapper.listTaskBases(normalizedStatus, like, orderColumn, orderDirection, safeSize, offset)
        .stream()
        .map(this::toSummary)
        .toList();
    long total = taskMapper.countTaskBases(normalizedStatus, like);
    return new PageResponse<>(records, total, safePage, safeSize);
  }

  @Transactional
  public TaskDetail createTask(CreateTaskRequest request) {
    NormalizedTask normalized = normalizeCreateRequest(request);
    String taskId = id();
    String now = now();
    taskMapper.insertTask(
        taskId,
        normalized.taskName(),
        STATUS_PENDING,
        normalized.description(),
        normalized.datasetId(),
        normalized.datasetVersionId(),
        normalized.rows().size(),
        normalized.appType(),
        normalized.appId(),
        normalized.appVersionId(),
        now);

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
    List<FieldDto> fields = datasetMapper.listFields(base.datasetVersionId());
    List<TaskMapper.TaskItemRecord> itemRecords = taskMapper.listTaskItems(taskId, safeSize, offset);
    long total = taskMapper.countTaskItems(taskId);
    return new TaskDetail(
        base,
        fields,
        taskMapper.listEvaluatorDimensions(taskId),
        taskMapper.listTagDimensions(taskId),
        new PageResponse<>(buildItems(itemRecords), total, safePage, safeSize));
  }

  @Transactional
  public TaskDetail startTask(String taskId) {
    TaskBase base = findTask(taskId);
    if (STATUS_RUNNING.equals(base.status())) {
      return getTask(taskId, 1, 10);
    }
    if (STATUS_COMPLETED.equals(base.status())) {
      throw new IllegalArgumentException("评测完成的任务不能重新开始");
    }

    String startedAt = now();
    taskMapper.updateTaskStatus(taskId, STATUS_RUNNING, startedAt, null, startedAt);
    List<TaskMapper.TaskItemRecord> items = taskMapper.listAllTaskItems(taskId);
    List<TaskMapper.TaskEvaluatorBindingRecord> evaluators = taskMapper.listTaskEvaluatorBindings(taskId);
    List<TaskMapper.TaskAppFieldMappingRecord> appMappings = taskMapper.listAppFieldMappings(taskId);
    Map<String, EvaluationRuntimeConfig> evaluatorConfigs = loadEvaluatorRuntimeConfigs(evaluators);
    Map<String, List<TaskMapper.TaskEvaluatorParamMappingRecord>> mappingsByEvaluator = taskMapper.listAllParamMappings(taskId)
        .stream()
        .collect(Collectors.groupingBy(TaskMapper.TaskEvaluatorParamMappingRecord::taskEvaluatorId));

    Map<String, Map<String, String>> valuesByItem = loadDatasetValues(items);
    for (TaskMapper.TaskEvaluatorBindingRecord evaluator : evaluators) {
      taskMapper.updateTaskEvaluatorStatus(evaluator.id(), STATUS_RUNNING, startedAt);
    }

    boolean hasTaskFailure = false;
    Set<String> failedEvaluatorIds = new HashSet<>();
    for (TaskMapper.TaskItemRecord item : items) {
      String itemStartedAt = now();
      Map<String, String> rowValues = valuesByItem.getOrDefault(item.datasetItemId(), Map.of());
      AgentInvocationResult agentResult = invokeAgent(base, item, appMappings, rowValues);
      boolean hasFailedEvaluator = STATUS_FAILED.equals(agentResult.status());
      if (hasFailedEvaluator) {
        hasTaskFailure = true;
        for (TaskMapper.TaskEvaluatorBindingRecord evaluator : evaluators) {
          failedEvaluatorIds.add(evaluator.id());
          String finishedAt = now();
          taskMapper.updateEvaluatorResult(
              item.id(),
              evaluator.id(),
              RESULT_SKIPPED,
              null,
              "",
              "",
              "智能体调用失败，跳过评估：" + agentResult.errorMessage(),
              itemStartedAt,
              finishedAt,
              finishedAt);
        }
      } else {
        for (TaskMapper.TaskEvaluatorBindingRecord evaluator : evaluators) {
          EvaluationRuntimeConfig config = evaluatorConfigs.get(evaluator.id());
          List<TaskMapper.TaskEvaluatorParamMappingRecord> mappings = mappingsByEvaluator.getOrDefault(evaluator.id(), List.of());
          EvaluationSimulationResult result = evaluateWithIntegration(config, mappings, rowValues, agentResult.outputs());
          if (STATUS_FAILED.equals(result.status())) {
            hasFailedEvaluator = true;
            hasTaskFailure = true;
            failedEvaluatorIds.add(evaluator.id());
          }
          String finishedAt = now();
          taskMapper.updateEvaluatorResult(
              item.id(),
              evaluator.id(),
              result.status(),
              result.score(),
              result.passResult(),
              result.resultValue(),
              result.errorMessage(),
              itemStartedAt,
              finishedAt,
              finishedAt);
        }
      }
      String itemFinishedAt = now();
      String itemStatus = hasFailedEvaluator ? STATUS_FAILED : ITEM_ANNOTATION_PENDING;
      if (taskMapper.countUnfinishedTagResultsByItem(item.id()) == 0 && !hasFailedEvaluator) {
        itemStatus = STATUS_COMPLETED;
      }
      taskMapper.updateTaskItemRunResult(
          item.id(),
          itemStatus,
          agentResult.content(),
          agentResult.status(),
          agentResult.errorMessage(),
          itemStartedAt,
          itemFinishedAt,
          itemFinishedAt);
    }

    for (TaskMapper.TaskEvaluatorBindingRecord evaluator : evaluators) {
      String evaluatorStatus = failedEvaluatorIds.contains(evaluator.id()) ? STATUS_FAILED : STATUS_COMPLETED;
      taskMapper.updateTaskEvaluatorStatus(evaluator.id(), evaluatorStatus, now());
    }
    String finalStatus = hasTaskFailure
        ? STATUS_FAILED
        : (taskMapper.countUnfinishedTagResultsByTask(taskId) == 0
            && taskMapper.countUnfinishedEvaluatorResultsByTask(taskId) == 0
            && taskMapper.countUnfinishedTaskItems(taskId) == 0
            ? STATUS_COMPLETED
            : STATUS_RUNNING);
    boolean finished = STATUS_COMPLETED.equals(finalStatus) || STATUS_FAILED.equals(finalStatus);
    taskMapper.updateTaskStatus(taskId, finalStatus, null, finished ? now() : null, now());
    return getTask(taskId, 1, 10);
  }

  @Transactional
  public TaskDetail terminateTask(String taskId) {
    TaskBase base = findTask(taskId);
    if (STATUS_COMPLETED.equals(base.status())) {
      throw new IllegalArgumentException("评测完成的任务不能终止");
    }
    String now = now();
    taskMapper.updateTaskStatus(taskId, STATUS_TERMINATED, null, now, now);
    return getTask(taskId, 1, 10);
  }

  @Transactional
  public void deleteTask(String taskId) {
    findTask(taskId);
    taskMapper.softDeleteTask(taskId, now());
  }

  public AnnotationDetail getAnnotation(String taskId, String taskItemId) {
    TaskBase task = findTask(taskId);
    TaskMapper.TaskItemRecord itemRecord = findTaskItem(taskId, taskItemId);
    List<FieldDto> fields = datasetMapper.listFields(task.datasetVersionId());
    TaskItemDetail item = buildItems(List.of(itemRecord)).getFirst();
    List<TaskTagAnnotation> tags = buildTagAnnotations(taskId, itemRecord);
    return new AnnotationDetail(
        task,
        item,
        fields,
        tags,
        item.evaluatorResults(),
        taskMapper.findPreviousTaskItemId(taskId, itemRecord.rowNo()),
        taskMapper.findNextTaskItemId(taskId, itemRecord.rowNo()));
  }

  @Transactional
  public AnnotationDetail saveAnnotation(String taskId, String taskItemId, SaveAnnotationRequest request) {
    TaskBase task = findTask(taskId);
    TaskMapper.TaskItemRecord item = findTaskItem(taskId, taskItemId);
    if (request == null || request.tags() == null || request.tags().isEmpty()) {
      throw new IllegalArgumentException("请提交标注结果");
    }
    Map<String, TaskMapper.TaskTagBindingRecord> tagsById = taskMapper.listTaskTagBindings(taskId)
        .stream()
        .collect(Collectors.toMap(TaskMapper.TaskTagBindingRecord::id, Function.identity()));
    String annotatedAt = now();
    Set<String> touchedTagIds = new HashSet<>();
    for (TagAnnotationInput input : request.tags()) {
      if (input == null || !StringUtils.hasText(input.taskTagId())) {
        continue;
      }
      TaskMapper.TaskTagBindingRecord tag = tagsById.get(input.taskTagId());
      if (tag == null) {
        throw new IllegalArgumentException("标签不属于当前任务");
      }
      NormalizedAnnotation annotation = normalizeAnnotation(tag, input);
      taskMapper.updateTagResult(
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
    String description = request.description() == null ? "" : request.description().trim();
    if (description.length() > 200) {
      throw new IllegalArgumentException("描述不能超过200个字符");
    }
    String datasetId = requireText(request.datasetId(), "请选择评测集");
    String datasetVersionId = requireText(request.datasetVersionId(), "请选择评测集版本");
    DatasetVersionDto version = datasetMapper.findVersion(datasetVersionId);
    if (version == null || !datasetId.equals(version.datasetId())) {
      throw new IllegalArgumentException("评测集版本不存在");
    }
    if (Boolean.TRUE.equals(version.draft())) {
      throw new IllegalArgumentException("评测任务请选择已发布的评测集版本");
    }
    DatasetSummary dataset = datasetMapper.findDatasetSummary(datasetId);
    if (dataset == null) {
      throw new IllegalArgumentException("评测集不存在");
    }
    List<FieldDto> fields = datasetMapper.listFields(datasetVersionId);
    Map<String, FieldDto> fieldById = fields.stream()
        .filter(field -> StringUtils.hasText(field.id()))
        .collect(Collectors.toMap(FieldDto::id, Function.identity()));
    List<DatasetMapper.RowRecord> rows = datasetMapper.listAllRows(datasetVersionId);
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("评测集版本中暂无数据，不能创建任务");
    }

    String appType = normalizeAppType(request.appType());
    String appId = APP_AGENT.equals(appType) ? requireText(request.appId(), "请选择智能体应用") : "";
    String appVersionId = APP_AGENT.equals(appType) ? requireText(request.appVersionId(), "请选择智能体应用版本") : "";
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
        appVersionId,
        appMappings,
        evaluators,
        tagIds,
        rows);
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
    for (AppFieldMappingInput mapping : mappings) {
      if (mapping == null || !StringUtils.hasText(mapping.appInputName())) {
        continue;
      }
      String fieldId = requireText(mapping.datasetFieldId(), "请选择应用入参映射的评测集字段");
      if (!fieldById.containsKey(fieldId)) {
        throw new IllegalArgumentException("应用入参映射的评测集字段不存在");
      }
      normalized.add(new AppFieldMappingInput(
          mapping.appInputId() == null ? "" : mapping.appInputId().trim(),
          mapping.appInputName().trim(),
          StringUtils.hasText(mapping.appInputType()) ? mapping.appInputType().trim() : "string",
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
      if (EVALUATOR_PRESET.equals(source)) {
        PresetEvaluatorDetail preset = evaluatorService.getPresetEvaluator(evaluatorId);
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
      normalized.add(new NormalizedEvaluator(source, evaluatorId, evaluatorVersionId, definition, paramMappings, order++));
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
      if (tagMapper.findTagConfig(id) == null) {
        throw new IllegalArgumentException("标签不存在");
      }
      normalized.add(id);
    }
    return normalized;
  }

  private void saveAppMappings(String taskId, NormalizedTask task, String now) {
    int order = 1;
    for (AppFieldMappingInput mapping : task.appMappings()) {
      taskMapper.insertAppFieldMapping(
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
      taskMapper.insertTaskEvaluator(
          taskEvaluatorId,
          taskId,
          evaluator.source(),
          evaluator.evaluatorId(),
          evaluator.evaluatorVersionId(),
          STATUS_PENDING,
          evaluator.displayOrder(),
          now);
      for (NormalizedParamMapping mapping : evaluator.paramMappings()) {
        taskMapper.insertParamMapping(
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
      taskMapper.insertTaskTag(taskTagId, taskId, tagId, STATUS_PENDING, order++, now);
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
    for (DatasetMapper.RowRecord row : task.rows()) {
      String taskItemId = id();
      taskMapper.insertTaskItem(
          taskItemId,
          taskId,
          task.datasetVersionId(),
          row.id(),
          row.rowNo(),
          STATUS_PENDING,
          appOutputStatus,
          now);
      for (String taskEvaluatorId : taskEvaluatorIds) {
        taskMapper.insertEvaluatorResult(id(), taskId, taskItemId, taskEvaluatorId, STATUS_PENDING, now);
      }
      for (String taskTagId : taskTagIds) {
        taskMapper.insertTagResult(id(), taskId, taskItemId, taskTagId, STATUS_PENDING, now);
      }
    }
  }

  private TaskSummary toSummary(TaskBase base) {
    return new TaskSummary(
        base,
        taskMapper.listEvaluatorDimensions(base.id()),
        taskMapper.listTagDimensions(base.id()));
  }

  private List<TaskItemDetail> buildItems(List<TaskMapper.TaskItemRecord> itemRecords) {
    if (itemRecords.isEmpty()) {
      return List.of();
    }
    List<String> taskItemIds = itemRecords.stream().map(TaskMapper.TaskItemRecord::id).toList();
    Map<String, Map<String, String>> valuesByDatasetItem = loadDatasetValues(itemRecords);
    Map<String, List<TaskEvaluatorResultDto>> evaluatorResults = taskMapper.listEvaluatorResultsByTaskItemIds(taskItemIds)
        .stream()
        .collect(Collectors.groupingBy(TaskEvaluatorResultDto::taskItemId));
    Map<String, List<TaskTagResultDto>> tagResults = taskMapper.listTagResultsByTaskItemIds(taskItemIds)
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
            evaluatorResults.getOrDefault(item.id(), List.of()),
            tagResults.getOrDefault(item.id(), List.of()),
            item.createdAt(),
            item.updatedAt()))
        .toList();
  }

  private Map<String, Map<String, String>> loadDatasetValues(List<TaskMapper.TaskItemRecord> itemRecords) {
    List<String> datasetItemIds = itemRecords.stream().map(TaskMapper.TaskItemRecord::datasetItemId).toList();
    return datasetMapper.loadValues(datasetItemIds);
  }

  private List<TaskTagAnnotation> buildTagAnnotations(String taskId, TaskMapper.TaskItemRecord itemRecord) {
    List<TaskTagResultDto> results = taskMapper.listTagResultsByTaskItemIds(List.of(itemRecord.id()));
    Map<String, TaskTagResultDto> resultByTaskTag = results.stream()
        .collect(Collectors.toMap(TaskTagResultDto::taskTagId, Function.identity()));
    return taskMapper.listTaskTagBindings(taskId).stream()
        .map(tag -> new TaskTagAnnotation(
            tag.id(),
            tag.tagId(),
            tag.tagName(),
            tag.tagType(),
            tag.description(),
            tag.minValue(),
            tag.maxValue(),
            tag.passThreshold(),
            tagMapper.listOptions(tag.tagId()),
            resultByTaskTag.get(tag.id())))
        .toList();
  }

  private Map<String, EvaluationRuntimeConfig> loadEvaluatorRuntimeConfigs(List<TaskMapper.TaskEvaluatorBindingRecord> evaluators) {
    Map<String, EvaluationRuntimeConfig> configs = new HashMap<>();
    for (TaskMapper.TaskEvaluatorBindingRecord evaluator : evaluators) {
      if (EVALUATOR_PRESET.equals(evaluator.evaluatorSource())) {
        PresetEvaluatorDetail preset = evaluatorService.getPresetEvaluator(evaluator.evaluatorId());
        configs.put(evaluator.id(), new EvaluationRuntimeConfig(
            preset.evaluatorName(),
            preset.evaluatorType(),
            preset.modelId(),
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
            config.modelId(),
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

  private AgentInvocationResult invokeAgent(
      TaskBase base,
      TaskMapper.TaskItemRecord item,
      List<TaskMapper.TaskAppFieldMappingRecord> appMappings,
      Map<String, String> values
  ) {
    if (!APP_AGENT.equals(base.appType())) {
      return new AgentInvocationResult("", RESULT_SKIPPED, "", Map.of());
    }
    String content = buildAgentMessageContent(appMappings, values);
    PlatformAgentChatResponse response = integrationService.invokeAgent(
        resolveAgentAlias(base),
        new PlatformAgentChatRequest(
            base.id() + "-" + item.id(),
            List.of(new PlatformAgentMessage("user", content)),
            true));
    if (response == null) {
      return new AgentInvocationResult("", STATUS_FAILED, "Super智能体未返回结果", Map.of());
    }
    Map<String, String> outputs = extractAgentOutputs(response);
    if (STATUS_FAILED.equals(response.status())) {
      return new AgentInvocationResult(
          formatAgentOutputs(outputs),
          STATUS_FAILED,
          firstNonBlank(response.errorMessage(), outputs.get("error"), outputs.get("rawText")),
          outputs);
    }
    if (StringUtils.hasText(outputs.get("error"))) {
      return new AgentInvocationResult(
          formatAgentOutputs(outputs),
          STATUS_FAILED,
          outputs.get("error"),
          outputs);
    }
    return new AgentInvocationResult(
        formatAgentOutputs(outputs),
        STATUS_COMPLETED,
        "",
        outputs);
  }

  private String resolveAgentAlias(TaskBase base) {
    return StringUtils.hasText(base.appId()) ? base.appId() : DEFAULT_AGENT_ALIAS;
  }

  private String buildAgentMessageContent(
      List<TaskMapper.TaskAppFieldMappingRecord> appMappings,
      Map<String, String> values
  ) {
    Map<String, String> inputs = new LinkedHashMap<>();
    if (appMappings != null && !appMappings.isEmpty()) {
      for (TaskMapper.TaskAppFieldMappingRecord mapping : appMappings) {
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

  private EvaluationSimulationResult evaluateWithIntegration(
      EvaluationRuntimeConfig config,
      List<TaskMapper.TaskEvaluatorParamMappingRecord> mappings,
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
    PreparedEvaluationInput prepared = prepareEvaluationInput(config, mappings, values, appOutputs);
    if (StringUtils.hasText(prepared.errorMessage())) {
      return new EvaluationSimulationResult(
          STATUS_FAILED,
          null,
          "",
          "",
          prepared.errorMessage());
    }
    String renderedPrompt = renderPrompt(config.prompt(), prepared.params());
    PlatformModelChatResult response = integrationService.chatModel(config.modelId(), renderedPrompt);
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
    if (score.compareTo(config.scoreMin()) < 0 || score.compareTo(config.scoreMax()) > 0) {
      return new EvaluationSimulationResult(
          STATUS_FAILED,
          score,
          "",
          parsed.reason(),
          "模型评估结果中的score超出评分范围");
    }
    String passResult = score.compareTo(config.passThreshold()) >= 0 ? "pass" : "fail";
    return new EvaluationSimulationResult(
        STATUS_COMPLETED,
        score,
        passResult,
        parsed.reason(),
        "");
  }

  private PreparedEvaluationInput prepareEvaluationInput(
      EvaluationRuntimeConfig config,
      List<TaskMapper.TaskEvaluatorParamMappingRecord> mappings,
      Map<String, String> values,
      Map<String, String> appOutputs
  ) {
    Map<String, Object> params = new LinkedHashMap<>();
    Map<String, TaskMapper.TaskEvaluatorParamMappingRecord> mappingByParam = mappings.stream()
        .collect(Collectors.toMap(mapping -> paramKey(mapping.paramId(), mapping.paramName()), Function.identity(), (a, b) -> a));
    for (EvaluatorParamDto param : config.params()) {
      TaskMapper.TaskEvaluatorParamMappingRecord mapping = mappingByParam.get(paramKey(param.id(), param.paramName()));
      String value = resolveMappingValue(mapping, values, appOutputs);
      if (!StringUtils.hasText(value) && StringUtils.hasText(param.defaultValue())) {
        value = param.defaultValue();
      }
      if (!StringUtils.hasText(value)) {
        if (Boolean.TRUE.equals(param.required())) {
          return new PreparedEvaluationInput(
              params,
              "必填参数未完成映射或数据为空：" + param.paramName());
        }
        continue;
      }
      params.put(param.paramName(), value);
    }
    return new PreparedEvaluationInput(params, "");
  }

  private String resolveMappingValue(
      TaskMapper.TaskEvaluatorParamMappingRecord mapping,
      Map<String, String> values,
      Map<String, String> appOutputs
  ) {
    if (mapping == null) {
      return "";
    }
    if (SOURCE_APP_OUTPUT.equals(mapping.sourceType())) {
      if (StringUtils.hasText(mapping.appOutputName()) && appOutputs.containsKey(mapping.appOutputName())) {
        return appOutputs.get(mapping.appOutputName());
      }
      if (appOutputs.containsKey("text")) {
        return appOutputs.get("text");
      }
      if (appOutputs.containsKey("answer")) {
        return appOutputs.get("answer");
      }
      if (appOutputs.containsKey("content")) {
        return appOutputs.get("content");
      }
      return appOutputs.getOrDefault("rawText", "");
    }
    return values.getOrDefault(mapping.datasetFieldId(), "");
  }

  private Map<String, String> extractAgentOutputs(PlatformAgentChatResponse response) {
    Map<String, String> outputs = new LinkedHashMap<>();
    List<String> debugParts = new ArrayList<>();
    List<String> reasoningParts = new ArrayList<>();
    List<String> textParts = new ArrayList<>();
    List<String> errorParts = new ArrayList<>();
    if (response.choices() != null) {
      for (PlatformAgentChoice choice : response.choices()) {
        if (choice == null || choice.delta() == null || choice.delta().content() == null) {
          continue;
        }
        for (PlatformAgentContentBlock content : choice.delta().content()) {
          appendAgentContent(debugParts, reasoningParts, textParts, errorParts, content);
        }
      }
    }

    putIfText(outputs, "debug", joinLines(debugParts));
    putIfText(outputs, "reasoning", joinLines(reasoningParts));
    putIfText(outputs, "text", joinLines(textParts));
    putIfText(outputs, "error", joinLines(errorParts));
    if (response.outputs() != null) {
      response.outputs().forEach((key, value) -> {
        if (StringUtils.hasText(key) && !outputs.containsKey(key)) {
          outputs.put(key, value == null ? "" : value);
        }
      });
    }
    putIfText(outputs, "rawText", firstNonBlank(
        outputs.get("rawText"),
        joinLines(List.of(
            outputs.getOrDefault("debug", ""),
            outputs.getOrDefault("reasoning", ""),
            outputs.getOrDefault("text", ""),
            outputs.getOrDefault("error", ""))),
        response.rawOutput()));
    putIfText(outputs, "answer", firstNonBlank(outputs.get("answer"), outputs.get("text"), outputs.get("content")));
    putIfText(outputs, "content", firstNonBlank(outputs.get("content"), outputs.get("text"), outputs.get("answer")));
    return outputs;
  }

  private void appendAgentContent(
      List<String> debugParts,
      List<String> reasoningParts,
      List<String> textParts,
      List<String> errorParts,
      PlatformAgentContentBlock content
  ) {
    if (content == null || !StringUtils.hasText(content.type())) {
      return;
    }
    String type = content.type().trim();
    String value = firstNonBlank(content.text(), content.reasoning(), content.error());
    if (!StringUtils.hasText(value)) {
      return;
    }
    if ("debug".equals(type)) {
      debugParts.add(value);
    } else if ("reasoning".equals(type)) {
      reasoningParts.add(value);
    } else if ("text".equals(type)) {
      textParts.add(value);
    } else if ("error".equals(type)) {
      errorParts.add(value);
    }
  }

  private String formatAgentOutputs(Map<String, String> outputs) {
    if (outputs == null || outputs.isEmpty()) {
      return "";
    }
    List<String> fields = new ArrayList<>();
    addJsonField(fields, "debug", outputs.get("debug"));
    addJsonField(fields, "reasoning", outputs.get("reasoning"));
    addJsonField(fields, "text", outputs.get("text"));
    addJsonField(fields, "error", outputs.get("error"));
    addJsonField(fields, "rawText", outputs.get("rawText"));
    return "{" + String.join(",", fields) + "}";
  }

  private void addJsonField(List<String> fields, String key, String value) {
    fields.add("\"" + escapeJson(key) + "\":\"" + escapeJson(value == null ? "" : value) + "\"");
  }

  private void putIfText(Map<String, String> outputs, String key, String value) {
    if (StringUtils.hasText(value)) {
      outputs.put(key, value);
    }
  }

  private String joinLines(List<String> parts) {
    if (parts == null || parts.isEmpty()) {
      return "";
    }
    return parts.stream()
        .filter(StringUtils::hasText)
        .collect(Collectors.joining("\n"));
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return "";
  }

  private String escapeJson(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
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

  private NormalizedAnnotation normalizeAnnotation(TaskMapper.TaskTagBindingRecord tag, TagAnnotationInput input) {
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
    TagOptionDto option = tagMapper.listOptions(tag.tagId()).stream()
        .filter(item -> optionId.equals(item.id()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("标签选项不存在：" + tag.tagName()));
    return new NormalizedAnnotation(option.optionName(), null, option.id(), option.optionGroup());
  }

  private void refreshTaskTagStatus(String taskTagId, String now) {
    int total = taskMapper.countTagResults(taskTagId);
    int completed = taskMapper.countCompletedTagResults(taskTagId);
    String status = completed == 0 ? STATUS_PENDING : (completed >= total ? STATUS_COMPLETED : "annotating");
    taskMapper.updateTaskTagStatus(taskTagId, status, now);
  }

  private void refreshItemAndTaskStatus(String taskId, String taskItemId, String now) {
    int unfinishedEvaluators = taskMapper.countUnfinishedEvaluatorResultsByItem(taskItemId);
    int unfinishedTags = taskMapper.countUnfinishedTagResultsByItem(taskItemId);
    if (unfinishedEvaluators == 0 && unfinishedTags == 0) {
      taskMapper.updateTaskItemStatus(taskItemId, STATUS_COMPLETED, now);
    } else if (unfinishedEvaluators == 0) {
      taskMapper.updateTaskItemStatus(taskItemId, ITEM_ANNOTATION_PENDING, now);
    }
    if (taskMapper.countUnfinishedTaskItems(taskId) == 0) {
      taskMapper.updateTaskStatus(taskId, STATUS_COMPLETED, null, now, now);
    } else {
      TaskBase task = findTask(taskId);
      if (STATUS_PENDING.equals(task.status())) {
        taskMapper.updateTaskStatus(taskId, STATUS_RUNNING, now, null, now);
      }
    }
  }

  private TaskBase findTask(String taskId) {
    if (!StringUtils.hasText(taskId)) {
      throw new IllegalArgumentException("评测任务ID不能为空");
    }
    TaskBase task = taskMapper.findTaskBase(taskId);
    if (task == null) {
      throw new IllegalArgumentException("评测任务不存在");
    }
    return task;
  }

  private TaskMapper.TaskItemRecord findTaskItem(String taskId, String taskItemId) {
    if (!StringUtils.hasText(taskItemId)) {
      throw new IllegalArgumentException("任务数据行ID不能为空");
    }
    TaskMapper.TaskItemRecord item = taskMapper.findTaskItem(taskItemId);
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
    if (!List.of(STATUS_PENDING, STATUS_RUNNING, STATUS_COMPLETED, STATUS_TERMINATED, STATUS_FAILED).contains(normalized)) {
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
      String appVersionId,
      List<AppFieldMappingInput> appMappings,
      List<NormalizedEvaluator> evaluators,
      List<String> tagIds,
      List<DatasetMapper.RowRecord> rows
  ) {
  }

  private record NormalizedEvaluator(
      String source,
      String evaluatorId,
      String evaluatorVersionId,
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

  private record PreparedEvaluationInput(
      Map<String, Object> params,
      String errorMessage
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
