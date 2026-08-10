package com.agentnexus.backend.common.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentnexus.backend.common.context.CurrentUserHolder;
import com.agentnexus.backend.common.security.CurrentUser;
import com.agentnexus.backend.dataset.api.dto.response.DatasetSummary;
import com.agentnexus.backend.dataset.repository.DatasetRepository;
import com.agentnexus.backend.evaluator.repository.EvaluatorRepository;
import com.agentnexus.backend.tag.api.dto.response.TagConfig;
import com.agentnexus.backend.tag.repository.TagRepository;
import com.agentnexus.backend.task.repository.TaskRepository;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ResourceFieldPatchServiceTest {
  @Mock
  private DatasetRepository datasetRepository;
  @Mock
  private TagRepository tagRepository;
  @Mock
  private EvaluatorRepository evaluatorRepository;
  @Mock
  private TaskRepository taskRepository;

  private ResourceFieldPatchService service;

  @BeforeEach
  void setUp() {
    service = new ResourceFieldPatchService(datasetRepository, tagRepository, evaluatorRepository, taskRepository);
    CurrentUserHolder.set(new CurrentUser("user-1", "User One", Set.of("space-1")));
  }

  @AfterEach
  void tearDown() {
    CurrentUserHolder.clear();
  }

  @Test
  void updateFieldsTrimsAndPersistsDatasetDescriptionForOwner() {
    when(datasetRepository.findDatasetSummary("dataset-1")).thenReturn(datasetSummary("dataset-1"));
    when(datasetRepository.isDatasetCreatedByCurrentUser("dataset-1")).thenReturn(true);

    ResourceFieldPatchResult result = service.updateFields(
        "dataset",
        "dataset-1",
        new ResourceFieldPatchRequest(Map.of("description", "  新描述  ")));

    assertEquals("dataset", result.resourceType());
    assertEquals("dataset-1", result.resourceId());
    assertEquals("新描述", result.fields().get("description"));
    verify(datasetRepository).updateDatasetDescription(eq("dataset-1"), eq("新描述"), anyString());
  }

  @Test
  void updateFieldsRejectsResourcesCreatedByOtherUsers() {
    when(tagRepository.findTagConfig("tag-1")).thenReturn(tagConfig("tag-1"));
    when(tagRepository.isTagCreatedByCurrentUser("tag-1")).thenReturn(false);

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class,
        () -> service.updateFields("tag", "tag-1", new ResourceFieldPatchRequest(Map.of("description", "新描述"))));

    assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    verify(tagRepository, never()).updateTagDescription(anyString(), anyString(), anyString());
  }

  @Test
  void updateFieldsRejectsUnsupportedFields() {
    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> service.updateFields("evaluator", "evaluator-1", new ResourceFieldPatchRequest(Map.of("name", "新名称"))));

    assertEquals("暂不支持修改字段：name", error.getMessage());
  }

  @Test
  void updateFieldsRejectsLongDescription() {
    String longDescription = "a".repeat(201);

    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> service.updateFields("task", "task-1", new ResourceFieldPatchRequest(Map.of("description", longDescription))));

    assertEquals("描述不能超过200个字符", error.getMessage());
  }

  private DatasetSummary datasetSummary(String id) {
    return new DatasetSummary(id, "name", "", 0, null, 0, "User One", null, "User One", null);
  }

  private TagConfig tagConfig(String id) {
    return new TagConfig(id, "tag", "text", "", null, null, null, null, null);
  }
}
