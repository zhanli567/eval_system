package com.evalsystem.task.controller;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.common.PageResponse;
import com.evalsystem.task.dto.AnnotationDetail;
import com.evalsystem.task.dto.CreateTaskRequest;
import com.evalsystem.task.dto.SaveAnnotationRequest;
import com.evalsystem.task.dto.TaskDetail;
import com.evalsystem.task.dto.TaskSummary;
import com.evalsystem.task.service.TaskService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping
  public ApiResponse<PageResponse<TaskSummary>> listTasks(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "updatedAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder
  ) {
    return ApiResponse.ok(taskService.listTasks(page, size, status, keyword, sortBy, sortOrder));
  }

  @PostMapping
  public ApiResponse<TaskDetail> createTask(@RequestBody CreateTaskRequest request) {
    return ApiResponse.ok(taskService.createTask(request));
  }

  @GetMapping("/{taskId}")
  public ApiResponse<TaskDetail> getTask(
      @PathVariable String taskId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    return ApiResponse.ok(taskService.getTask(taskId, page, size));
  }

  @PostMapping("/{taskId}/start")
  public ApiResponse<TaskDetail> startTask(@PathVariable String taskId) {
    return ApiResponse.ok(taskService.startTask(taskId));
  }

  @PostMapping("/{taskId}/terminate")
  public ApiResponse<TaskDetail> terminateTask(@PathVariable String taskId) {
    return ApiResponse.ok(taskService.terminateTask(taskId));
  }

  @DeleteMapping("/{taskId}")
  public ApiResponse<Void> deleteTask(@PathVariable String taskId) {
    taskService.deleteTask(taskId);
    return ApiResponse.ok(null);
  }

  @GetMapping("/{taskId}/items/{taskItemId}/annotation")
  public ApiResponse<AnnotationDetail> getAnnotation(@PathVariable String taskId, @PathVariable String taskItemId) {
    return ApiResponse.ok(taskService.getAnnotation(taskId, taskItemId));
  }

  @PutMapping("/{taskId}/items/{taskItemId}/annotation")
  public ApiResponse<AnnotationDetail> saveAnnotation(
      @PathVariable String taskId,
      @PathVariable String taskItemId,
      @RequestBody SaveAnnotationRequest request
  ) {
    return ApiResponse.ok(taskService.saveAnnotation(taskId, taskItemId, request));
  }
}
