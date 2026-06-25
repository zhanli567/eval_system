package com.evalsystem.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.evalsystem.dataset.api.dto.response.DatasetSummary;
import com.evalsystem.dataset.api.dto.response.DatasetVersionDto;
import com.evalsystem.dataset.api.dto.response.FieldDto;
import com.evalsystem.dataset.repository.DatasetRepository;
import com.evalsystem.dataset.repository.DatasetRowRecord;
import com.evalsystem.evaluator.preset.PresetEvaluatorStore;
import com.evalsystem.evaluator.service.EvaluatorService;
import com.evalsystem.integration.service.PlatformIntegrationService;
import com.evalsystem.tag.repository.TagRepository;
import com.evalsystem.task.api.dto.request.AppFieldMappingInput;
import com.evalsystem.task.api.dto.request.CreateTaskRequest;
import com.evalsystem.task.api.dto.response.TaskBase;
import com.evalsystem.task.api.dto.response.TaskEvaluatorDimension;
import com.evalsystem.task.api.dto.request.TaskEvaluatorInput;
import com.evalsystem.task.api.dto.request.TaskEvaluatorParamMappingInput;
import com.evalsystem.task.api.dto.response.TaskEvaluatorResultDto;
import com.evalsystem.task.repository.TaskEvaluatorBindingRecord;
import com.evalsystem.task.repository.TaskItemRecord;
import com.evalsystem.task.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskServicePresetDisplayTest {
  private static final String ANSWER_CONSISTENCY_NAME = "\u56de\u590d\u4e00\u81f4\u6027";
  private static final String PRESET_MODEL_REQUIRED_MESSAGE = "\u8bf7\u9009\u62e9\u9884\u7f6e\u8bc4\u4f30\u5668\u6a21\u578b";
  private static final LocalDateTime CREATED_DATE = LocalDateTime.of(2025, 1, 1, 0, 0);
  private static final LocalDateTime LAST_UPDATED_DATE = LocalDateTime.of(2026, 1, 1, 0, 0);

  private TaskRepository taskRepository;
  private DatasetRepository datasetRepository;
  private EvaluatorService evaluatorService;
  private TagRepository tagRepository;
  private PlatformIntegrationService integrationService;
  private PresetEvaluatorStore presetStore;
  private TaskService service;

  @BeforeEach
  void setUp() {
    taskRepository = mock(TaskRepository.class);
    datasetRepository = mock(DatasetRepository.class);
    evaluatorService = mock(EvaluatorService.class);
    tagRepository = mock(TagRepository.class);
    integrationService = mock(PlatformIntegrationService.class);
    presetStore = new PresetEvaluatorStore();
    service = newTaskService(Runnable::run);
  }

  @Test
  void getTaskFillsPresetEvaluatorDisplayFieldsWithoutPresetTableRows() {
    when(taskRepository.findTaskBase("task-1")).thenReturn(taskBase("pending", "none"));
    when(datasetRepository.listFields("version-1")).thenReturn(List.of());
    when(taskRepository.listEvaluatorDimensions("task-1")).thenReturn(List.of(new TaskEvaluatorDimension(
        "task-evaluator-1",
        "preset",
        "answer_consistency",
        "",
        "",
        "",
        "preset",
        "pending",
        0,
        0,
        1,
        null,
        1)));
    when(taskRepository.listTagDimensions("task-1")).thenReturn(List.of());
    when(taskRepository.listTaskItems("task-1", 10, 0)).thenReturn(List.of(new TaskItemRecord(
        "task-item-1",
        "task-1",
        "version-1",
        "dataset-item-1",
        1,
        "pending",
        "",
        "skipped",
        "",
        CREATED_DATE,
        LAST_UPDATED_DATE)));
    when(taskRepository.countTaskItems("task-1")).thenReturn(1L);
    when(datasetRepository.loadValues(List.of("dataset-item-1"))).thenReturn(Map.of("dataset-item-1", Map.of()));
    when(taskRepository.listEvaluatorResultsByTaskItemIds(List.of("task-item-1"))).thenReturn(List.of(new TaskEvaluatorResultDto(
        "result-1",
        "task-item-1",
        "task-evaluator-1",
        "",
        "",
        "preset",
        "pending",
        null,
        "",
        "",
        "",
        "",
        "")));
    when(taskRepository.listTagResultsByTaskItemIds(List.of("task-item-1"))).thenReturn(List.of());
    when(taskRepository.listTaskEvaluatorBindings("task-1")).thenReturn(List.of(new TaskEvaluatorBindingRecord(
        "task-evaluator-1",
        "task-1",
        "preset",
        "answer_consistency",
        "",
        "model-1",
        "pending",
        1)));
    when(evaluatorService.getPresetEvaluator("answer_consistency"))
        .thenReturn(presetStore.getPresetEvaluator("answer_consistency"));

    var detail = service.getTask("task-1", 1, 10);

    assertThat(detail.evaluators().getFirst().evaluatorName()).isEqualTo(ANSWER_CONSISTENCY_NAME);
    assertThat(detail.evaluators().getFirst().evaluatorType()).isEqualTo("llm");
    assertThat(detail.items().records().getFirst().evaluatorResults().getFirst().evaluatorName()).isEqualTo(ANSWER_CONSISTENCY_NAME);
    assertThat(detail.items().records().getFirst().evaluatorResults().getFirst().evaluatorType()).isEqualTo("llm");
  }

  @Test
  void createTaskRequiresModelForPresetLlmEvaluator() {
    prepareCreateTaskDataset();
    when(evaluatorService.getPresetEvaluator("answer_consistency"))
        .thenReturn(presetStore.getPresetEvaluator("answer_consistency"));

    assertThatThrownBy(() -> service.createTask(createTaskRequest("")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(PRESET_MODEL_REQUIRED_MESSAGE);
  }

  @Test
  void createTaskPersistsModelForPresetLlmEvaluator() {
    prepareCreateTaskDataset();
    prepareGetTaskAfterCreate();
    when(evaluatorService.getPresetEvaluator("answer_consistency"))
        .thenReturn(presetStore.getPresetEvaluator("answer_consistency"));

    service.createTask(createTaskRequest("model-1"));

    verify(taskRepository).insertTaskEvaluator(
        anyString(),
        anyString(),
        eq("preset"),
        eq("answer_consistency"),
        eq(""),
        eq("model-1"),
        eq("pending"),
        eq(1),
        anyString());
  }

  @Test
  void createAgentTaskPersistsSelectedChildAgentAlias() {
    prepareCreateTaskDataset();
    prepareGetTaskAfterCreate();
    when(evaluatorService.getPresetEvaluator("answer_consistency"))
        .thenReturn(presetStore.getPresetEvaluator("answer_consistency"));

    service.createTask(createAgentTaskRequest(" child-a "));

    verify(taskRepository).insertTask(
        anyString(),
        eq("task"),
        eq("pending"),
        eq(""),
        eq("dataset-1"),
        eq("version-1"),
        eq(1),
        eq("agent"),
        eq("agent-1"),
        eq("bundle-main"),
        eq("child-a"),
        anyString());
  }

  @Test
  void restartTaskClearsPreviousRunOutputsAndResults() {
    TaskService noOpExecutorService = newTaskService(command -> {
    });
    when(taskRepository.findTaskBase("task-1")).thenReturn(taskBase("failed", "agent"));
    when(taskRepository.listTaskEvaluatorBindings("task-1")).thenReturn(List.of());
    when(datasetRepository.listFields("version-1")).thenReturn(List.of());
    when(taskRepository.listEvaluatorDimensions("task-1")).thenReturn(List.of());
    when(taskRepository.listTagDimensions("task-1")).thenReturn(List.of());
    when(taskRepository.listTaskItems("task-1", 10, 0)).thenReturn(List.of());

    noOpExecutorService.startTask("task-1");

    verify(taskRepository).resetTaskItemsForRestart(eq("task-1"), eq("pending"), anyString());
    verify(taskRepository).resetEvaluatorResultsForRestart(eq("task-1"), anyString());
    verify(taskRepository).resetTaskTagsForRestart(eq("task-1"), anyString());
    verify(taskRepository).resetTagResultsForRestart(eq("task-1"), anyString());
  }

  @Test
  void deleteTaskAllowsFailedTask() {
    when(taskRepository.findTaskBase("task-1")).thenReturn(taskBase("failed", "none"));

    service.deleteTask("task-1");

    verify(taskRepository).softDeleteTask(eq("task-1"), anyString());
  }

  private TaskService newTaskService(org.springframework.core.task.TaskExecutor taskExecutor) {
    return new TaskService(
        taskRepository,
        datasetRepository,
        evaluatorService,
        tagRepository,
        integrationService,
        new ObjectMapper(),
        taskExecutor);
  }

  private TaskBase taskBase(String status, String appType) {
    return new TaskBase(
        "task-1",
        "task",
        status,
        "",
        "dataset-1",
        "dataset",
        "version-1",
        1,
        "V1",
        1,
        appType,
        "agent".equals(appType) ? "agent-1" : "",
        "agent".equals(appType) ? "agent-version-1" : "",
        "agent".equals(appType) ? "child-a" : "",
        "",
        "",
        CREATED_DATE,
        LAST_UPDATED_DATE);
  }

  private void prepareCreateTaskDataset() {
    when(datasetRepository.findVersion("version-1")).thenReturn(new DatasetVersionDto(
        "version-1",
        "dataset-1",
        1,
        "V1",
        1,
        false,
        CREATED_DATE,
        LAST_UPDATED_DATE));
    when(datasetRepository.findDatasetSummary("dataset-1")).thenReturn(new DatasetSummary(
        "dataset-1",
        "dataset",
        "",
        1,
        "version-1",
        1,
        CREATED_DATE,
        LAST_UPDATED_DATE));
    when(datasetRepository.listFields("version-1")).thenReturn(List.of(
        new FieldDto("field-query", "version-1", "query", "string", true, "", 1),
        new FieldDto("field-reference", "version-1", "reference_response", "string", true, "", 2),
        new FieldDto("field-response", "version-1", "response", "string", true, "", 3)));
    when(datasetRepository.listAllRows("version-1")).thenReturn(List.of(
        new DatasetRowRecord("row-1", 1, CREATED_DATE, LAST_UPDATED_DATE)));
  }

  private void prepareGetTaskAfterCreate() {
    when(taskRepository.findTaskBase(anyString())).thenReturn(taskBase("pending", "none"));
    when(taskRepository.listTaskItems(anyString(), anyInt(), anyInt())).thenReturn(List.of());
    when(taskRepository.countTaskItems(anyString())).thenReturn(0L);
    when(taskRepository.listEvaluatorDimensions(anyString())).thenReturn(List.of());
    when(taskRepository.listTagDimensions(anyString())).thenReturn(List.of());
  }

  private CreateTaskRequest createTaskRequest(String modelId) {
    return new CreateTaskRequest(
        "task",
        "",
        "dataset-1",
        "version-1",
        "none",
        "",
        "",
        "",
        List.of(),
        List.of(new TaskEvaluatorInput(
            "preset",
            "answer_consistency",
            "",
            modelId,
            List.of(
                new TaskEvaluatorParamMappingInput("answer_consistency:query", "query", "dataset_field", "field-query", ""),
                new TaskEvaluatorParamMappingInput("answer_consistency:reference_response", "reference_response", "dataset_field", "field-reference", ""),
                new TaskEvaluatorParamMappingInput("answer_consistency:response", "response", "dataset_field", "field-response", "")))),
        List.of());
  }

  private CreateTaskRequest createAgentTaskRequest(String appAgentAlias) {
    return new CreateTaskRequest(
        "task",
        "",
        "dataset-1",
        "version-1",
        "agent",
        "agent-1",
        "bundle-main",
        appAgentAlias,
        List.of(new AppFieldMappingInput("query", "query", "string", "field-query")),
        List.of(new TaskEvaluatorInput(
            "preset",
            "answer_consistency",
            "",
            "model-1",
            List.of(
                new TaskEvaluatorParamMappingInput("answer_consistency:query", "query", "dataset_field", "field-query", ""),
                new TaskEvaluatorParamMappingInput("answer_consistency:reference_response", "reference_response", "dataset_field", "field-reference", ""),
                new TaskEvaluatorParamMappingInput("answer_consistency:response", "response", "dataset_field", "field-response", "")))),
        List.of());
  }
}
