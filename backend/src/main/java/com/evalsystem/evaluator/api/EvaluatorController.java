package com.evalsystem.evaluator.api;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.common.PageResponse;
import com.evalsystem.evaluator.api.dto.response.EvaluatorConfig;
import com.evalsystem.evaluator.api.dto.request.EvaluatorInput;
import com.evalsystem.evaluator.api.dto.response.EvaluatorSummary;
import com.evalsystem.evaluator.api.dto.response.EvaluatorVersionDto;
import com.evalsystem.evaluator.api.dto.response.PresetCategoryDto;
import com.evalsystem.evaluator.api.dto.response.PresetEvaluatorDetail;
import com.evalsystem.evaluator.api.dto.response.PresetEvaluatorSummary;
import com.evalsystem.evaluator.service.EvaluatorService;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

@Component
@ResponseBody
@Path("/evaluators")
public class EvaluatorController {
  private final EvaluatorService evaluatorService;

  public EvaluatorController(EvaluatorService evaluatorService) {
    this.evaluatorService = evaluatorService;
  }

  @GET
  @Path("")
  public ApiResponse<PageResponse<EvaluatorSummary>> listEvaluators(
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("evaluatorType") String evaluatorType,
      @QueryParam("keyword") String keyword
  ) {
    return ApiResponse.ok(evaluatorService.listEvaluators(page, size, evaluatorType, keyword));
  }

  @GET
  @Path("/presets/categories")
  public ApiResponse<List<PresetCategoryDto>> listPresetCategories() {
    return ApiResponse.ok(evaluatorService.listPresetCategories());
  }

  @GET
  @Path("/presets")
  public ApiResponse<PageResponse<PresetEvaluatorSummary>> listPresetEvaluators(
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("size") @DefaultValue("12") int size,
      @QueryParam("categoryId") String categoryId,
      @QueryParam("keyword") String keyword
  ) {
    return ApiResponse.ok(evaluatorService.listPresetEvaluators(page, size, categoryId, keyword));
  }

  @GET
  @Path("/presets/{presetId}")
  public ApiResponse<PresetEvaluatorDetail> getPresetEvaluator(@PathParam("presetId") String presetId) {
    return ApiResponse.ok(evaluatorService.getPresetEvaluator(presetId));
  }

  @POST
  @Path("")
  public ApiResponse<EvaluatorConfig> createEvaluator(EvaluatorInput request) {
    return ApiResponse.ok(evaluatorService.createEvaluator(request));
  }

  @DELETE
  @Path("/{evaluatorId}")
  public ApiResponse<Void> deleteEvaluator(@PathParam("evaluatorId") String evaluatorId) {
    evaluatorService.deleteEvaluator(evaluatorId);
    return ApiResponse.ok(null);
  }

  @GET
  @Path("/{evaluatorId}/versions")
  public ApiResponse<List<EvaluatorVersionDto>> listVersions(@PathParam("evaluatorId") String evaluatorId) {
    return ApiResponse.ok(evaluatorService.listVersions(evaluatorId));
  }

  @POST
  @Path("/{evaluatorId}/publish")
  public ApiResponse<EvaluatorConfig> publish(@PathParam("evaluatorId") String evaluatorId) {
    return ApiResponse.ok(evaluatorService.publish(evaluatorId));
  }

  @GET
  @Path("/versions/{versionId}")
  public ApiResponse<EvaluatorConfig> getVersion(@PathParam("versionId") String versionId) {
    return ApiResponse.ok(evaluatorService.getVersion(versionId));
  }

  @PUT
  @Path("/versions/{versionId}")
  public ApiResponse<EvaluatorConfig> updateDraft(@PathParam("versionId") String versionId, EvaluatorInput request) {
    return ApiResponse.ok(evaluatorService.updateDraft(versionId, request));
  }
}
