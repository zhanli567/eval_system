package com.evalsystem.evaluator.service.impl;

import com.evalsystem.common.PageResponse;
import com.evalsystem.evaluator.dto.EvaluatorConfig;
import com.evalsystem.evaluator.dto.EvaluatorConfigBase;
import com.evalsystem.evaluator.dto.EvaluatorInput;
import com.evalsystem.evaluator.dto.EvaluatorParamDto;
import com.evalsystem.evaluator.dto.EvaluatorParamInput;
import com.evalsystem.evaluator.dto.EvaluatorSummary;
import com.evalsystem.evaluator.dto.EvaluatorVersionDto;
import com.evalsystem.evaluator.dto.PresetCategoryDto;
import com.evalsystem.evaluator.dto.PresetEvaluatorDetail;
import com.evalsystem.evaluator.dto.PresetEvaluatorSummary;
import com.evalsystem.evaluator.mapper.EvaluatorMapper;
import com.evalsystem.evaluator.preset.PresetEvaluatorStore;
import com.evalsystem.evaluator.service.EvaluatorService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EvaluatorServiceImpl implements EvaluatorService {
  private static final String TYPE_LLM = "llm";
  private static final String TYPE_CODE = "code";
  private static final String TARGET_VERSION = "version";
  private static final String PARAM_TYPE_STRING = "string";
  private static final List<String> SUPPORTED_TYPES = List.of(TYPE_LLM, TYPE_CODE);
  private static final List<String> SUPPORTED_PARAM_TYPES = List.of(PARAM_TYPE_STRING, "number", "boolean");
  private static final BigDecimal DEFAULT_SCORE_MIN = BigDecimal.ONE;
  private static final BigDecimal DEFAULT_SCORE_MAX = BigDecimal.valueOf(5);
  private static final BigDecimal DEFAULT_PASS_THRESHOLD = BigDecimal.valueOf(3);
  private static final int MAX_PROMPT_LENGTH = 2000;
  private static final int MAX_EXECUTE_CODE_LENGTH = 10000;
  private static final int MAX_PARAM_DESCRIPTION_LENGTH = 200;
  private static final Pattern PROMPT_PARAM_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][\\w]*)}");

  private final EvaluatorMapper evaluatorMapper;
  private final PresetEvaluatorStore presetEvaluatorStore;

  public EvaluatorServiceImpl(EvaluatorMapper evaluatorMapper, PresetEvaluatorStore presetEvaluatorStore) {
    this.evaluatorMapper = evaluatorMapper;
    this.presetEvaluatorStore = presetEvaluatorStore;
  }

  public PageResponse<EvaluatorSummary> listEvaluators(int page, int size, String evaluatorType, String keyword) {
    String normalizedType = normalizeOptionalEvaluatorType(evaluatorType);
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 100);
    int offset = (safePage - 1) * safeSize;
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    List<EvaluatorSummary> records = evaluatorMapper.listEvaluators(normalizedType, like, safeSize, offset);
    long total = evaluatorMapper.countEvaluators(normalizedType, like);
    return new PageResponse<>(records, total, safePage, safeSize);
  }

  public List<PresetCategoryDto> listPresetCategories() {
    return presetEvaluatorStore.listCategories();
  }

  public PageResponse<PresetEvaluatorSummary> listPresetEvaluators(int page, int size, String categoryId, String keyword) {
    return presetEvaluatorStore.listEvaluators(page, size, categoryId, keyword);
  }

  public PresetEvaluatorDetail getPresetEvaluator(String presetId) {
    return presetEvaluatorStore.getPresetEvaluator(presetId);
  }

  @Transactional
  public EvaluatorConfig createEvaluator(EvaluatorInput request) {
    NormalizedEvaluator normalized = normalizeEvaluatorInput(request, null);
    String evaluatorId = id();
    String versionId = id();
    String now = now();
    evaluatorMapper.insertEvaluator(evaluatorId, normalized.evaluatorName(), normalized.evaluatorType(), normalized.description(), versionId, now);
    evaluatorMapper.insertVersion(
        versionId,
        evaluatorId,
        0,
        normalized.modelId(),
        normalized.prompt(),
        normalized.executeCode(),
        normalized.scoreMin(),
        normalized.scoreMax(),
        normalized.passThreshold(),
        now);
    saveParams(TARGET_VERSION, versionId, normalized.params(), now);
    return getVersion(versionId);
  }

  public List<EvaluatorVersionDto> listVersions(String evaluatorId) {
    return evaluatorMapper.listVersions(evaluatorId);
  }

  public EvaluatorConfig getVersion(String versionId) {
    EvaluatorConfigBase base = evaluatorMapper.findVersionConfig(versionId);
    if (base == null) {
      throw new IllegalArgumentException("评估器版本不存在");
    }
    return attachParams(base);
  }

  @Transactional
  public EvaluatorConfig updateDraft(String versionId, EvaluatorInput request) {
    EvaluatorConfig existing = getVersion(versionId);
    if (!Boolean.TRUE.equals(existing.draft())) {
      throw new IllegalArgumentException("只有草稿版本允许修改");
    }
    NormalizedEvaluator normalized = normalizeEvaluatorInput(request, existing.evaluatorType());
    evaluatorMapper.updateEvaluatorBase(existing.evaluatorId(), normalized.evaluatorName(), normalized.description(), now());
    String now = now();
    evaluatorMapper.updateDraftVersion(
        versionId,
        normalized.modelId(),
        normalized.prompt(),
        normalized.executeCode(),
        normalized.scoreMin(),
        normalized.scoreMax(),
        normalized.passThreshold(),
        now);
    evaluatorMapper.deleteParams(TARGET_VERSION, versionId);
    saveParams(TARGET_VERSION, versionId, normalized.params(), now);
    return getVersion(versionId);
  }

  @Transactional
  public EvaluatorConfig publish(String evaluatorId) {
    String draftVersionId = evaluatorMapper.findDraftVersionId(evaluatorId);
    if (!StringUtils.hasText(draftVersionId)) {
      throw new IllegalArgumentException("草稿版本不存在");
    }
    EvaluatorConfig draft = getVersion(draftVersionId);
    int nextVersionNo = evaluatorMapper.nextVersionNo(evaluatorId);
    String newVersionId = id();
    String now = now();
    evaluatorMapper.insertVersion(
        newVersionId,
        evaluatorId,
        nextVersionNo,
        draft.modelId(),
        draft.prompt(),
        draft.executeCode(),
        draft.scoreMin(),
        draft.scoreMax(),
        draft.passThreshold(),
        now);
    saveParams(TARGET_VERSION, newVersionId, draft.params().stream()
        .map(param -> new EvaluatorParamInput(
            null,
            param.paramName(),
            param.dataType(),
            param.defaultValue(),
            param.required(),
            param.description()))
        .toList(), now);
    evaluatorMapper.updateLatestVersion(evaluatorId, newVersionId, now);
    return getVersion(newVersionId);
  }

  @Transactional
  public void deleteEvaluator(String evaluatorId) {
    String now = now();
    evaluatorMapper.softDeleteEvaluator(evaluatorId, now);
    evaluatorMapper.softDeleteVersionsByEvaluator(evaluatorId, now);
  }

  private EvaluatorConfig attachParams(EvaluatorConfigBase base) {
    return new EvaluatorConfig(
        base.evaluatorId(),
        base.evaluatorName(),
        base.evaluatorType(),
        base.description(),
        base.versionId(),
        base.versionNo(),
        base.versionName(),
        base.draft(),
        base.modelId(),
        base.prompt(),
        base.executeCode(),
        base.scoreMin(),
        base.scoreMax(),
        base.passThreshold(),
        base.createdAt(),
        base.updatedAt(),
        listEvaluatorParams(TARGET_VERSION, base.versionId(), base.evaluatorType(), base.prompt()));
  }

  private List<EvaluatorParamDto> listEvaluatorParams(String targetType, String targetId, String evaluatorType, String prompt) {
    List<EvaluatorParamDto> params = evaluatorMapper.listParams(targetType, targetId);
    if (!TYPE_LLM.equals(evaluatorType) || !params.isEmpty() || !StringUtils.hasText(prompt)) {
      return params;
    }
    List<EvaluatorParamDto> extracted = new ArrayList<>();
    int order = 1;
    for (String paramName : extractPromptParamNames(prompt)) {
      extracted.add(new EvaluatorParamDto(
          null,
          targetType,
          targetId,
          paramName,
          PARAM_TYPE_STRING,
          "",
          true,
          "",
          order++));
    }
    return extracted;
  }

  private NormalizedEvaluator normalizeEvaluatorInput(EvaluatorInput request, String existingType) {
    if (request == null) {
      throw new IllegalArgumentException("评估器参数不能为空");
    }
    String evaluatorName = normalizeName(request.evaluatorName());
    String evaluatorType = StringUtils.hasText(existingType)
        ? existingType
        : normalizeEvaluatorType(request.evaluatorType());
    if (StringUtils.hasText(request.evaluatorType()) && !evaluatorType.equals(request.evaluatorType().trim())) {
      throw new IllegalArgumentException("评估器类型创建后不允许修改");
    }
    String description = normalizeDescription(request.description());
    BigDecimal scoreMin = request.scoreMin() == null ? DEFAULT_SCORE_MIN : request.scoreMin();
    BigDecimal scoreMax = request.scoreMax() == null ? DEFAULT_SCORE_MAX : request.scoreMax();
    BigDecimal passThreshold = request.passThreshold() == null ? DEFAULT_PASS_THRESHOLD : request.passThreshold();
    validateScore(scoreMin, scoreMax, passThreshold);

    String modelId = "";
    String prompt = "";
    String executeCode = "";
    List<EvaluatorParamInput> params = List.of();
    if (TYPE_LLM.equals(evaluatorType)) {
      modelId = request.modelId() == null ? "" : request.modelId().trim();
      prompt = requireText(request.prompt(), "Prompt不能为空");
      validateMaxLength(prompt, MAX_PROMPT_LENGTH, "Prompt不能超过2000个字符");
      params = normalizePromptParams(prompt, request.params());
    } else {
      executeCode = requireText(request.executeCode(), "执行函数不能为空");
      validateMaxLength(executeCode, MAX_EXECUTE_CODE_LENGTH, "执行函数不能超过10000个字符");
      params = normalizeCodeParams(request.params());
    }

    return new NormalizedEvaluator(
        evaluatorName,
        evaluatorType,
        description,
        modelId,
        prompt,
        executeCode,
        scoreMin,
        scoreMax,
        passThreshold,
        params);
  }

  private String normalizeName(String evaluatorName) {
    String normalized = requireText(evaluatorName, "评估器名称不能为空");
    if (normalized.length() > 50) {
      throw new IllegalArgumentException("评估器名称不能超过50个字符");
    }
    return normalized;
  }

  private String normalizeDescription(String description) {
    String normalized = description == null ? "" : description.trim();
    if (normalized.length() > 200) {
      throw new IllegalArgumentException("描述不能超过200个字符");
    }
    return normalized;
  }

  private String normalizeOptionalEvaluatorType(String evaluatorType) {
    if (!StringUtils.hasText(evaluatorType)) {
      return null;
    }
    return normalizeEvaluatorType(evaluatorType);
  }

  private String normalizeEvaluatorType(String evaluatorType) {
    String normalized = requireText(evaluatorType, "评估器类型不能为空").toLowerCase();
    if (!SUPPORTED_TYPES.contains(normalized)) {
      throw new IllegalArgumentException("评估器类型仅支持llm/code");
    }
    return normalized;
  }

  private List<EvaluatorParamInput> normalizeCodeParams(List<EvaluatorParamInput> params) {
    if (params == null) {
      throw new IllegalArgumentException("请至少配置一个变量");
    }
    List<EvaluatorParamInput> normalized = new ArrayList<>();
    Set<String> names = new HashSet<>();
    for (EvaluatorParamInput param : params) {
      if (param == null || !StringUtils.hasText(param.paramName())) {
        continue;
      }
      String paramName = param.paramName().trim();
      if (paramName.length() > 64) {
        throw new IllegalArgumentException("变量名不能超过64个字符");
      }
      if (!names.add(paramName)) {
        throw new IllegalArgumentException("变量名不能重复");
      }
      normalized.add(new EvaluatorParamInput(
          param.id(),
          paramName,
          normalizeParamType(param.dataType()),
          param.defaultValue() == null ? "" : param.defaultValue(),
          normalizeRequired(param.required()),
          normalizeParamDescription(param.description())));
    }
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("请至少配置一个变量");
    }
    return normalized;
  }

  private List<EvaluatorParamInput> normalizePromptParams(String prompt, List<EvaluatorParamInput> params) {
    List<String> paramNames = extractPromptParamNames(prompt);
    if (paramNames.isEmpty()) {
      throw new IllegalArgumentException("Prompt至少需要包含一个${参数名}参数");
    }
    Map<String, EvaluatorParamInput> providedParams = mapParamsByName(params);
    List<EvaluatorParamInput> normalized = new ArrayList<>();
    for (String paramName : paramNames) {
      EvaluatorParamInput provided = providedParams.get(paramName);
      normalized.add(new EvaluatorParamInput(
          provided == null ? null : provided.id(),
          paramName,
          provided == null ? PARAM_TYPE_STRING : normalizeParamType(provided.dataType()),
          provided == null || provided.defaultValue() == null ? "" : provided.defaultValue(),
          provided == null ? true : normalizeRequired(provided.required()),
          provided == null ? "" : normalizeParamDescription(provided.description())));
    }
    return normalized;
  }

  private Map<String, EvaluatorParamInput> mapParamsByName(List<EvaluatorParamInput> params) {
    Map<String, EvaluatorParamInput> mapped = new LinkedHashMap<>();
    if (params == null) {
      return mapped;
    }
    for (EvaluatorParamInput param : params) {
      if (param != null && StringUtils.hasText(param.paramName())) {
        mapped.putIfAbsent(param.paramName().trim(), param);
      }
    }
    return mapped;
  }

  private List<String> extractPromptParamNames(String prompt) {
    List<String> names = new ArrayList<>();
    Matcher matcher = PROMPT_PARAM_PATTERN.matcher(prompt);
    while (matcher.find()) {
      String paramName = matcher.group(1);
      if (!names.contains(paramName)) {
        names.add(paramName);
      }
    }
    return names;
  }

  private String normalizeParamType(String dataType) {
    String normalized = StringUtils.hasText(dataType) ? dataType.trim() : PARAM_TYPE_STRING;
    if (!SUPPORTED_PARAM_TYPES.contains(normalized)) {
      throw new IllegalArgumentException("变量类型仅支持string/number/boolean");
    }
    return normalized;
  }

  private Boolean normalizeRequired(Boolean required) {
    return required == null || required;
  }

  private String normalizeParamDescription(String description) {
    String normalized = description == null ? "" : description.trim();
    if (normalized.length() > MAX_PARAM_DESCRIPTION_LENGTH) {
      throw new IllegalArgumentException("变量描述不能超过200个字符");
    }
    return normalized;
  }

  private void validateScore(BigDecimal scoreMin, BigDecimal scoreMax, BigDecimal passThreshold) {
    if (scoreMin.compareTo(scoreMax) >= 0) {
      throw new IllegalArgumentException("评分范围最大值必须大于最小值");
    }
    if (passThreshold.compareTo(scoreMin) < 0 || passThreshold.compareTo(scoreMax) > 0) {
      throw new IllegalArgumentException("通过阈值必须位于评分范围内");
    }
  }

  private void saveParams(String targetType, String targetId, List<EvaluatorParamInput> params, String now) {
    int order = 1;
    for (EvaluatorParamInput param : params) {
      evaluatorMapper.insertParam(
          id(),
          targetType,
          targetId,
          param.paramName(),
          param.dataType(),
          param.defaultValue(),
          param.required(),
          param.description(),
          order++,
          now);
    }
  }

  private String requireText(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }

  private void validateMaxLength(String value, int maxLength, String message) {
    if (value != null && value.length() > maxLength) {
      throw new IllegalArgumentException(message);
    }
  }

  private String id() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private String now() {
    return String.valueOf(System.currentTimeMillis());
  }

  private record NormalizedEvaluator(
      String evaluatorName,
      String evaluatorType,
      String description,
      String modelId,
      String prompt,
      String executeCode,
      BigDecimal scoreMin,
      BigDecimal scoreMax,
      BigDecimal passThreshold,
      List<EvaluatorParamInput> params
  ) {
  }
}
