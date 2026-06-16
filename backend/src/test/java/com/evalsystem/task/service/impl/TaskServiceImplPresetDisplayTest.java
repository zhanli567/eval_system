package com.evalsystem.task.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.evalsystem.dataset.mapper.DatasetMapper;
import com.evalsystem.evaluator.preset.PresetEvaluatorStore;
import com.evalsystem.evaluator.service.EvaluatorService;
import com.evalsystem.integration.service.PlatformIntegrationService;
import com.evalsystem.tag.mapper.TagMapper;
import com.evalsystem.task.dto.TaskBase;
import com.evalsystem.task.dto.TaskEvaluatorDimension;
import com.evalsystem.task.dto.TaskEvaluatorResultDto;
import com.evalsystem.task.mapper.TaskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaskServiceImplPresetDisplayTest {
  @Test
  void getTaskFillsPresetEvaluatorDisplayFieldsWithoutPresetTableRows() {
    TaskMapper taskMapper = mock(TaskMapper.class);
    DatasetMapper datasetMapper = mock(DatasetMapper.class);
    EvaluatorService evaluatorService = mock(EvaluatorService.class);
    TagMapper tagMapper = mock(TagMapper.class);
    PlatformIntegrationService integrationService = mock(PlatformIntegrationService.class);
    PresetEvaluatorStore presetStore = new PresetEvaluatorStore();
    TaskServiceImpl service = new TaskServiceImpl(
        taskMapper,
        datasetMapper,
        evaluatorService,
        tagMapper,
        integrationService,
        new ObjectMapper(),
        Runnable::run);

    when(taskMapper.findTaskBase("task-1")).thenReturn(new TaskBase(
        "task-1",
        "任务",
        "pending",
        "",
        "dataset-1",
        "数据集",
        "version-1",
        1,
        "V1",
        1,
        "none",
        "",
        "",
        "",
        "",
        "1",
        "1"));
    when(datasetMapper.listFields("version-1")).thenReturn(List.of());
    when(taskMapper.listEvaluatorDimensions("task-1")).thenReturn(List.of(new TaskEvaluatorDimension(
        "task-evaluator-1",
        "preset",
        "answer_consistency",
        "",
        "",
        "",
        "预置",
        "pending",
        0,
        0,
        1,
        null,
        1)));
    when(taskMapper.listTagDimensions("task-1")).thenReturn(List.of());
    when(taskMapper.listTaskItems("task-1", 10, 0)).thenReturn(List.of(new TaskMapper.TaskItemRecord(
        "task-item-1",
        "task-1",
        "version-1",
        "dataset-item-1",
        1,
        "pending",
        "",
        "skipped",
        "",
        "1",
        "1")));
    when(taskMapper.countTaskItems("task-1")).thenReturn(1L);
    when(datasetMapper.loadValues(List.of("dataset-item-1"))).thenReturn(Map.of("dataset-item-1", Map.of()));
    when(taskMapper.listEvaluatorResultsByTaskItemIds(List.of("task-item-1"))).thenReturn(List.of(new TaskEvaluatorResultDto(
        "result-1",
        "task-item-1",
        "task-evaluator-1",
        "",
        "",
        "预置",
        "pending",
        null,
        "",
        "",
        "",
        "",
        "")));
    when(taskMapper.listTagResultsByTaskItemIds(List.of("task-item-1"))).thenReturn(List.of());
    when(taskMapper.listTaskEvaluatorBindings("task-1")).thenReturn(List.of(new TaskMapper.TaskEvaluatorBindingRecord(
        "task-evaluator-1",
        "task-1",
        "preset",
        "answer_consistency",
        "",
        "pending",
        1)));
    when(evaluatorService.getPresetEvaluator("answer_consistency"))
        .thenReturn(presetStore.getPresetEvaluator("answer_consistency"));

    var detail = service.getTask("task-1", 1, 10);

    assertThat(detail.evaluators().getFirst().evaluatorName()).isEqualTo("回复一致性");
    assertThat(detail.evaluators().getFirst().evaluatorType()).isEqualTo("llm");
    assertThat(detail.items().records().getFirst().evaluatorResults().getFirst().evaluatorName()).isEqualTo("回复一致性");
    assertThat(detail.items().records().getFirst().evaluatorResults().getFirst().evaluatorType()).isEqualTo("llm");
  }
}
