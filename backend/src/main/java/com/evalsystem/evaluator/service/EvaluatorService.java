package com.evalsystem.evaluator.service;

import com.evalsystem.common.PageResponse;
import com.evalsystem.evaluator.dto.EvaluatorConfig;
import com.evalsystem.evaluator.dto.EvaluatorInput;
import com.evalsystem.evaluator.dto.EvaluatorSummary;
import com.evalsystem.evaluator.dto.EvaluatorVersionDto;
import com.evalsystem.evaluator.dto.PresetCategoryDto;
import com.evalsystem.evaluator.dto.PresetEvaluatorDetail;
import com.evalsystem.evaluator.dto.PresetEvaluatorSummary;
import java.util.List;

public interface EvaluatorService {
  PageResponse<EvaluatorSummary> listEvaluators(int page, int size, String evaluatorType, String keyword);

  List<PresetCategoryDto> listPresetCategories();

  PageResponse<PresetEvaluatorSummary> listPresetEvaluators(int page, int size, String categoryId, String keyword);

  PresetEvaluatorDetail getPresetEvaluator(String presetId);

  EvaluatorConfig createEvaluator(EvaluatorInput request);

  List<EvaluatorVersionDto> listVersions(String evaluatorId);

  EvaluatorConfig getVersion(String versionId);

  EvaluatorConfig updateDraft(String versionId, EvaluatorInput request);

  EvaluatorConfig publish(String evaluatorId);

  void deleteEvaluator(String evaluatorId);
}
