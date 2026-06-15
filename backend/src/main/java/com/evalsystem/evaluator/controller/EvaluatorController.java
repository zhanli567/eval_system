package com.evalsystem.evaluator.controller;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.common.PageResponse;
import com.evalsystem.evaluator.dto.EvaluatorConfig;
import com.evalsystem.evaluator.dto.EvaluatorInput;
import com.evalsystem.evaluator.dto.EvaluatorSummary;
import com.evalsystem.evaluator.dto.EvaluatorVersionDto;
import com.evalsystem.evaluator.dto.PresetCategoryDto;
import com.evalsystem.evaluator.dto.PresetEvaluatorDetail;
import com.evalsystem.evaluator.dto.PresetEvaluatorSummary;
import com.evalsystem.evaluator.service.EvaluatorService;
import java.util.List;
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
@RequestMapping("/api/evaluators")
public class EvaluatorController {
  private final EvaluatorService evaluatorService;

  public EvaluatorController(EvaluatorService evaluatorService) {
    this.evaluatorService = evaluatorService;
  }

  @GetMapping
  public ApiResponse<PageResponse<EvaluatorSummary>> listEvaluators(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String evaluatorType,
      @RequestParam(required = false) String keyword
  ) {
    return ApiResponse.ok(evaluatorService.listEvaluators(page, size, evaluatorType, keyword));
  }

  @GetMapping("/presets/categories")
  public ApiResponse<List<PresetCategoryDto>> listPresetCategories() {
    return ApiResponse.ok(evaluatorService.listPresetCategories());
  }

  @GetMapping("/presets")
  public ApiResponse<PageResponse<PresetEvaluatorSummary>> listPresetEvaluators(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "12") int size,
      @RequestParam(required = false) String categoryId,
      @RequestParam(required = false) String keyword
  ) {
    return ApiResponse.ok(evaluatorService.listPresetEvaluators(page, size, categoryId, keyword));
  }

  @GetMapping("/presets/{presetId}")
  public ApiResponse<PresetEvaluatorDetail> getPresetEvaluator(@PathVariable String presetId) {
    return ApiResponse.ok(evaluatorService.getPresetEvaluator(presetId));
  }

  @PostMapping
  public ApiResponse<EvaluatorConfig> createEvaluator(@RequestBody EvaluatorInput request) {
    return ApiResponse.ok(evaluatorService.createEvaluator(request));
  }

  @DeleteMapping("/{evaluatorId}")
  public ApiResponse<Void> deleteEvaluator(@PathVariable String evaluatorId) {
    evaluatorService.deleteEvaluator(evaluatorId);
    return ApiResponse.ok(null);
  }

  @GetMapping("/{evaluatorId}/versions")
  public ApiResponse<List<EvaluatorVersionDto>> listVersions(@PathVariable String evaluatorId) {
    return ApiResponse.ok(evaluatorService.listVersions(evaluatorId));
  }

  @PostMapping("/{evaluatorId}/publish")
  public ApiResponse<EvaluatorConfig> publish(@PathVariable String evaluatorId) {
    return ApiResponse.ok(evaluatorService.publish(evaluatorId));
  }

  @GetMapping("/versions/{versionId}")
  public ApiResponse<EvaluatorConfig> getVersion(@PathVariable String versionId) {
    return ApiResponse.ok(evaluatorService.getVersion(versionId));
  }

  @PutMapping("/versions/{versionId}")
  public ApiResponse<EvaluatorConfig> updateDraft(@PathVariable String versionId, @RequestBody EvaluatorInput request) {
    return ApiResponse.ok(evaluatorService.updateDraft(versionId, request));
  }
}
