package com.evalsystem.task.api;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.common.PageResponse;
import com.evalsystem.task.api.dto.response.AnnotationDetail;
import com.evalsystem.task.api.dto.request.CreateTaskRequest;
import com.evalsystem.task.api.dto.request.SaveAnnotationRequest;
import com.evalsystem.task.api.dto.response.TaskDetail;
import com.evalsystem.task.api.dto.response.TaskSummary;
import com.evalsystem.task.service.TaskService;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

@Component
@ResponseBody
@Path("/api/tasks")
public class TaskController {
  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GET
  @Path("")
  public ApiResponse<PageResponse<TaskSummary>> listTasks(
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("status") String status,
      @QueryParam("keyword") String keyword,
      @QueryParam("sortBy") @DefaultValue("lastUpdatedDate") String sortBy,
      @QueryParam("sortOrder") @DefaultValue("desc") String sortOrder
  ) {
    return ApiResponse.ok(taskService.listTasks(page, size, status, keyword, sortBy, sortOrder));
  }

  @POST
  @Path("")
  public ApiResponse<TaskDetail> createTask(CreateTaskRequest request) {
    return ApiResponse.ok(taskService.createTask(request));
  }

  @GET
  @Path("/{taskId}")
  public ApiResponse<TaskDetail> getTask(
      @PathParam("taskId") String taskId,
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("size") @DefaultValue("10") int size
  ) {
    return ApiResponse.ok(taskService.getTask(taskId, page, size));
  }

  @POST
  @Path("/{taskId}/start")
  public ApiResponse<TaskDetail> startTask(@PathParam("taskId") String taskId) {
    return ApiResponse.ok(taskService.startTask(taskId));
  }

  @DELETE
  @Path("/{taskId}")
  public ApiResponse<Void> deleteTask(@PathParam("taskId") String taskId) {
    taskService.deleteTask(taskId);
    return ApiResponse.ok(null);
  }

  @GET
  @Path("/{taskId}/items/{taskItemId}/annotation")
  public ApiResponse<AnnotationDetail> getAnnotation(@PathParam("taskId") String taskId, @PathParam("taskItemId") String taskItemId) {
    return ApiResponse.ok(taskService.getAnnotation(taskId, taskItemId));
  }

  @PUT
  @Path("/{taskId}/items/{taskItemId}/annotation")
  public ApiResponse<AnnotationDetail> saveAnnotation(
      @PathParam("taskId") String taskId,
      @PathParam("taskItemId") String taskItemId,
      SaveAnnotationRequest request
  ) {
    return ApiResponse.ok(taskService.saveAnnotation(taskId, taskItemId, request));
  }
}
