package com.agentnexus.backend.common.resource;

import com.agentnexus.backend.common.context.CurrentUserHolder;
import com.agentnexus.backend.common.security.CurrentUser;
import com.agentnexus.backend.dataset.repository.DatasetRepository;
import com.agentnexus.backend.evaluator.repository.EvaluatorRepository;
import com.agentnexus.backend.tag.repository.TagRepository;
import com.agentnexus.backend.task.repository.TaskRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResourceFieldPatchService {
  private static final String DESCRIPTION_FIELD = "description";
  private static final Set<String> SUPPORTED_FIELDS = Set.of(DESCRIPTION_FIELD);

  private final DatasetRepository datasetRepository;
  private final TagRepository tagRepository;
  private final EvaluatorRepository evaluatorRepository;
  private final TaskRepository taskRepository;

  public ResourceFieldPatchService(
      DatasetRepository datasetRepository,
      TagRepository tagRepository,
      EvaluatorRepository evaluatorRepository,
      TaskRepository taskRepository
  ) {
    this.datasetRepository = datasetRepository;
    this.tagRepository = tagRepository;
    this.evaluatorRepository = evaluatorRepository;
    this.taskRepository = taskRepository;
  }

  @Transactional
  public ResourceFieldPatchResult updateFields(String resourceType, String resourceId, ResourceFieldPatchRequest request) {
    ResourceType type = ResourceType.from(resourceType);
    String id = requireText(resourceId, "资源ID不能为空");
    Map<String, Object> fields = normalizeFields(request);
    ensureCurrentUser();
    ensureResourceExists(type, id);
    ensureCreatedByCurrentUser(type, id);

    String now = now();
    if (fields.containsKey(DESCRIPTION_FIELD)) {
      updateDescription(type, id, fields.get(DESCRIPTION_FIELD).toString(), now);
    }
    return new ResourceFieldPatchResult(type.value, id, fields);
  }

  private Map<String, Object> normalizeFields(ResourceFieldPatchRequest request) {
    Map<String, Object> rawFields = request == null || request.fields() == null ? Map.of() : request.fields();
    if (rawFields.isEmpty()) {
      throw new IllegalArgumentException("请提供需要修改的字段");
    }
    Map<String, Object> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : rawFields.entrySet()) {
      String field = entry.getKey();
      if (!SUPPORTED_FIELDS.contains(field)) {
        throw new IllegalArgumentException("暂不支持修改字段：" + field);
      }
      normalized.put(field, normalizeDescription(entry.getValue()));
    }
    return normalized;
  }

  private String normalizeDescription(Object value) {
    if (value == null) {
      return "";
    }
    if (!(value instanceof String text)) {
      throw new IllegalArgumentException("描述必须是字符串");
    }
    String normalized = text.trim();
    if (normalized.length() > 200) {
      throw new IllegalArgumentException("描述不能超过200个字符");
    }
    return normalized;
  }

  private void ensureCurrentUser() {
    CurrentUser currentUser = CurrentUserHolder.get();
    if (currentUser == null || !StringUtils.hasText(currentUser.userId())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
    }
  }

  private void ensureResourceExists(ResourceType type, String id) {
    boolean exists = switch (type) {
      case DATASET -> datasetRepository.findDatasetSummary(id) != null;
      case TAG -> tagRepository.findTagConfig(id) != null;
      case EVALUATOR -> StringUtils.hasText(evaluatorRepository.findEvaluatorType(id));
      case TASK -> taskRepository.findTaskBase(id) != null;
    };
    if (!exists) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, type.label + "不存在");
    }
  }

  private void ensureCreatedByCurrentUser(ResourceType type, String id) {
    boolean owner = switch (type) {
      case DATASET -> datasetRepository.isDatasetCreatedByCurrentUser(id);
      case TAG -> tagRepository.isTagCreatedByCurrentUser(id);
      case EVALUATOR -> evaluatorRepository.isEvaluatorCreatedByCurrentUser(id);
      case TASK -> taskRepository.isTaskCreatedByCurrentUser(id);
    };
    if (!owner) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅创建人可以修改资源描述");
    }
  }

  private void updateDescription(ResourceType type, String id, String description, String now) {
    switch (type) {
      case DATASET -> datasetRepository.updateDatasetDescription(id, description, now);
      case TAG -> tagRepository.updateTagDescription(id, description, now);
      case EVALUATOR -> evaluatorRepository.updateEvaluatorDescription(id, description, now);
      case TASK -> taskRepository.updateTaskDescription(id, description, now);
    }
  }

  private String requireText(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }

  private String now() {
    return String.valueOf(System.currentTimeMillis());
  }

  private enum ResourceType {
    DATASET("dataset", "评测集"),
    TAG("tag", "标签"),
    EVALUATOR("evaluator", "评估器"),
    TASK("task", "评测任务");

    private final String value;
    private final String label;

    ResourceType(String value, String label) {
      this.value = value;
      this.label = label;
    }

    private static ResourceType from(String value) {
      String normalized = value == null ? "" : value.trim();
      for (ResourceType type : values()) {
        if (type.value.equals(normalized)) {
          return type;
        }
      }
      throw new IllegalArgumentException("资源类型不支持：" + normalized);
    }
  }
}
