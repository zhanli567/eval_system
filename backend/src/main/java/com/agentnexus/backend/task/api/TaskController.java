package com.agentnexus.backend.task.api;

import com.agentnexus.backend.common.ApiResponse;
import com.agentnexus.backend.common.PageResponse;
import com.agentnexus.backend.task.api.dto.response.AnnotationDetail;
import com.agentnexus.backend.task.api.dto.request.CreateTaskRequest;
import com.agentnexus.backend.task.api.dto.request.SaveAnnotationRequest;
import com.agentnexus.backend.task.api.dto.response.TaskDetail;
import com.agentnexus.backend.task.api.dto.response.TaskMetricItemDistribution;
import com.agentnexus.backend.task.api.dto.response.TaskMetricOverview;
import com.agentnexus.backend.task.api.dto.response.TaskMetricScoreSummary;
import com.agentnexus.backend.task.api.dto.response.TaskSummary;
import com.agentnexus.backend.task.service.TaskService;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

@Component
@ResponseBody
@Path("/tasks")
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

  /**
   * 查询评测任务指标统计概览。
   *
   * @param taskId 评测任务ID
   * @return 指标统计概览
   */
  @GET
  @Path("/{taskId}/metrics/overview")
  public ApiResponse<TaskMetricOverview> getMetricOverview(@PathParam("taskId") String taskId) {
    return ApiResponse.ok(taskService.getMetricOverview(taskId));
  }

  /**
   * 查询评测任务得分汇总。
   *
   * @param taskId 评测任务ID
   * @return 得分汇总
   */
  @GET
  @Path("/{taskId}/metrics/score-summary")
  public ApiResponse<TaskMetricScoreSummary> getMetricScoreSummary(@PathParam("taskId") String taskId) {
    return ApiResponse.ok(taskService.getMetricScoreSummary(taskId));
  }

  /**
   * 查询评测任务数据项分布。
   *
   * @param taskId 评测任务ID
   * @return 数据项分布
   */
  @GET
  @Path("/{taskId}/metrics/item-distribution")
  public ApiResponse<TaskMetricItemDistribution> getMetricItemDistribution(@PathParam("taskId") String taskId) {
    return ApiResponse.ok(taskService.getMetricItemDistribution(taskId));
  }

  @GET
  @Path("/{taskId}/copy-config")
  public ApiResponse<CreateTaskRequest> getTaskCopyConfig(@PathParam("taskId") String taskId) {
    return ApiResponse.ok(taskService.getTaskCopyConfig(taskId));
  }

  @POST
  @Path("/{taskId}/start")
  public ApiResponse<TaskDetail> startTask(@PathParam("taskId") String taskId, @HeaderParam("Cookie") String cookie) {
    return ApiResponse.ok(taskService.startTask(taskId, cookie));
  }

  /**
   * 停止正在执行的评测任务。
   *
   * @param taskId 评测任务ID
   * @return 停止后的评测任务详情
   */
  @POST
  @Path("/{taskId}/stop")
  public ApiResponse<TaskDetail> stopTask(@PathParam("taskId") String taskId) {
    return ApiResponse.ok(taskService.stopTask(taskId));
  }

  /**
   * 为评测任务添加标签。
   *
   * @param taskId 评测任务ID
   * @param tagId 标签ID
   * @return 添加标签后的评测任务详情
   */
  @POST
  @Path("/{taskId}/tags/{tagId}")
  public ApiResponse<TaskDetail> addTaskTag(
      @PathParam("taskId") String taskId,
      @PathParam("tagId") String tagId
  ) {
    return ApiResponse.ok(taskService.addTaskTag(taskId, tagId));
  }

  /**
   * 删除评测任务已绑定的标签。
   *
   * @param taskId 评测任务ID
   * @param taskTagId 任务标签ID
   * @return 删除标签后的评测任务详情
   */
  @POST
  @Path("/{taskId}/tags/{taskTagId}/delete")
  public ApiResponse<TaskDetail> deleteTaskTag(
      @PathParam("taskId") String taskId,
      @PathParam("taskTagId") String taskTagId
  ) {
    return ApiResponse.ok(taskService.deleteTaskTag(taskId, taskTagId));
  }

  @POST
  @Path("/{taskId}/delete")
  public ApiResponse<Void> deleteTask(@PathParam("taskId") String taskId) {
    taskService.deleteTask(taskId);
    return ApiResponse.ok(null);
  }

  @GET
  @Path("/{taskId}/items/{taskItemId}/annotation")
  public ApiResponse<AnnotationDetail> getAnnotation(@PathParam("taskId") String taskId, @PathParam("taskItemId") String taskItemId) {
    return ApiResponse.ok(taskService.getAnnotation(taskId, taskItemId));
  }

  @POST
  @Path("/{taskId}/items/{taskItemId}/annotation")
  public ApiResponse<AnnotationDetail> saveAnnotation(
      @PathParam("taskId") String taskId,
      @PathParam("taskItemId") String taskItemId,
      SaveAnnotationRequest request
  ) {
    return ApiResponse.ok(taskService.saveAnnotation(taskId, taskItemId, request));
  }
}
