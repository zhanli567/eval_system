package com.evalsystem.task.service;

import com.evalsystem.common.PageResponse;
import com.evalsystem.task.dto.AnnotationDetail;
import com.evalsystem.task.dto.CreateTaskRequest;
import com.evalsystem.task.dto.SaveAnnotationRequest;
import com.evalsystem.task.dto.TaskDetail;
import com.evalsystem.task.dto.TaskSummary;

public interface TaskService {
  PageResponse<TaskSummary> listTasks(int page, int size, String status, String keyword, String sortBy, String sortOrder);

  TaskDetail createTask(CreateTaskRequest request);

  TaskDetail getTask(String taskId, int page, int size);

  TaskDetail startTask(String taskId);

  TaskDetail terminateTask(String taskId);

  void deleteTask(String taskId);

  AnnotationDetail getAnnotation(String taskId, String taskItemId);

  AnnotationDetail saveAnnotation(String taskId, String taskItemId, SaveAnnotationRequest request);
}
