package com.agentnexus.backend.evaluator.service;

import com.agentnexus.backend.common.PageResponse;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorConfig;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorConfigBase;
import com.agentnexus.backend.evaluator.api.dto.request.EvaluatorInput;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorParamDto;
import com.agentnexus.backend.evaluator.api.dto.request.EvaluatorParamInput;
import com.agentnexus.backend.evaluator.api.dto.request.EvaluatorTrialRequest;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorSummary;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorTrialResponse;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorVersionDto;
import com.agentnexus.backend.evaluator.constant.EvaluationResultConstants;
import com.agentnexus.backend.evaluator.constant.EvaluatorParamTypeConstants;
import com.agentnexus.backend.evaluator.constant.EvaluatorSortConstants;
import com.agentnexus.backend.evaluator.constant.EvaluatorTargetConstants;
import com.agentnexus.backend.evaluator.constant.EvaluatorTypeConstants;
import com.agentnexus.backend.evaluator.api.dto.response.PresetCategoryDto;
import com.agentnexus.backend.evaluator.api.dto.response.PresetEvaluatorDetail;
import com.agentnexus.backend.evaluator.api.dto.response.PresetEvaluatorSummary;
import com.agentnexus.backend.evaluator.preset.PresetEvaluatorStore;
import com.agentnexus.backend.evaluator.repository.EvaluatorRepository;
import com.agentnexus.backend.remoteCall.api.dto.response.ModelChatResult;
import com.agentnexus.backend.remoteCall.service.RemoteCallService;
import com.agentnexus.backend.task.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class EvaluatorService {
  private static final BigDecimal DEFAULT_SCORE_MIN = BigDecimal.ONE;
  private static final BigDecimal DEFAULT_SCORE_MAX = BigDecimal.valueOf(5);
  private static final BigDecimal DEFAULT_PASS_THRESHOLD = BigDecimal.valueOf(3);
  private static final int MAX_PROMPT_LENGTH = 2000;
  private static final int MAX_EXECUTE_CODE_LENGTH = 10000;
  private static final int MAX_PARAM_DESCRIPTION_LENGTH = 200;
  private static final Pattern PROMPT_PARAM_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][\\w]*)}");

  private final EvaluatorRepository evaluatorRepository;
  private final PresetEvaluatorStore presetEvaluatorStore;
  private final TaskRepository taskRepository;
  private final RemoteCallService remoteCallService;
  private final ObjectMapper objectMapper;

  public EvaluatorService(
      EvaluatorRepository evaluatorRepository,
      PresetEvaluatorStore presetEvaluatorStore,
      TaskRepository taskRepository,
      RemoteCallService remoteCallService,
      ObjectMapper objectMapper
  ) {
    this.evaluatorRepository = evaluatorRepository;
    this.presetEvaluatorStore = presetEvaluatorStore;
    this.taskRepository = taskRepository;
    this.remoteCallService = remoteCallService;
    this.objectMapper = objectMapper;
  }

  public PageResponse<EvaluatorSummary> listEvaluators(int page, int size, String evaluatorType, String keyword, String sortBy, String sortOrder) {
    String normalizedType = normalizeOptionalEvaluatorType(evaluatorType);
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 100);
    int offset = (safePage - 1) * safeSize;
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    String orderColumn = EvaluatorSortConstants.CREATED_DATE.equals(sortBy)
        ? EvaluatorSortConstants.EVALUATOR_CREATED_DATE_COLUMN
        : EvaluatorSortConstants.EVALUATOR_LAST_UPDATED_DATE_COLUMN;
    String orderDirection = EvaluatorSortConstants.ASC.equalsIgnoreCase(sortOrder)
        ? EvaluatorSortConstants.SQL_ASC
        : EvaluatorSortConstants.SQL_DESC;
    List<EvaluatorSummary> records = evaluatorRepository.listEvaluators(normalizedType, like, orderColumn, orderDirection, safeSize, offset);
    long total = evaluatorRepository.countEvaluators(normalizedType, like);
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

  /**
   * 使用当前页面配置进行LLM评估器试运行。
   *
   * @param request 试运行请求
   * @return 试运行结果
   */
  public EvaluatorTrialResponse runTrial(EvaluatorTrialRequest request) {
    if (request == null || request.evaluator() == null) {
      throw new IllegalArgumentException("评估器试运行参数不能为空");
    }
    TrialEvaluator evaluator = normalizeTrialEvaluator(request.evaluator());
    Map<String, Object> params = prepareTrialParams(evaluator.params(), request.paramValues());
    String renderedPrompt = renderPrompt(evaluator.prompt(), params);
    ModelChatResult response = remoteCallService.chatModel(evaluator.modelId(), evaluator.modelName(), renderedPrompt);
    if (response == null || !StringUtils.hasText(response.outputText())) {
      return new EvaluatorTrialResponse("", EvaluationResultConstants.FAIL, null, "", "模型对话接口未返回评估结果");
    }
    EvaluatorParseResult parsed = parseEvaluationOutput(response.outputText());
    if (StringUtils.hasText(parsed.errorMessage())) {
      return new EvaluatorTrialResponse(response.outputText(), EvaluationResultConstants.FAIL, null, "", parsed.errorMessage());
    }
    String result = parsed.score().compareTo(evaluator.passThreshold()) >= 0
        ? EvaluationResultConstants.PASS
        : EvaluationResultConstants.FAIL;
    String reason = scoreOutOfRange(parsed.score(), evaluator)
        ? appendEvaluationNotice(parsed.reason(), "模型评估结果中的score超出评分范围")
        : parsed.reason();
    return new EvaluatorTrialResponse(response.outputText(), result, parsed.score(), reason, "");
  }

  @Transactional
  public EvaluatorConfig createEvaluator(EvaluatorInput request) {
    NormalizedEvaluator normalized = normalizeEvaluatorInput(request, null);
    String evaluatorId = id();
    String versionId = id();
    String now = now();
    if (evaluatorRepository.existsEvaluatorName(normalized.evaluatorName())) {
      throw new IllegalArgumentException("当前空间已存在同名评估器");
    } else {
      evaluatorRepository.insertEvaluator(evaluatorId, normalized.evaluatorName(), normalized.evaluatorType(), normalized.description(), versionId, now);
    }
    evaluatorRepository.insertVersion(
        versionId,
        evaluatorId,
        0,
        normalized.modelId(),
        normalized.modelName(),
        normalized.prompt(),
        normalized.executeCode(),
        normalized.scoreMin(),
        normalized.scoreMax(),
        normalized.passThreshold(),
        now);
    saveParams(EvaluatorTargetConstants.VERSION, versionId, normalized.params(), now);
    return getVersion(versionId);
  }

  public List<EvaluatorVersionDto> listVersions(String evaluatorId) {
    return evaluatorRepository.listVersions(evaluatorId);
  }

  public EvaluatorConfig getVersion(String versionId) {
    EvaluatorConfigBase base = evaluatorRepository.findVersionConfig(versionId);
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
    evaluatorRepository.updateEvaluatorBase(existing.evaluatorId(), normalized.evaluatorName(), normalized.description(), now());
    String now = now();
    evaluatorRepository.updateDraftVersion(
        versionId,
        normalized.modelId(),
        normalized.modelName(),
        normalized.prompt(),
        normalized.executeCode(),
        normalized.scoreMin(),
        normalized.scoreMax(),
        normalized.passThreshold(),
        now);
    evaluatorRepository.deleteParams(EvaluatorTargetConstants.VERSION, versionId);
    saveParams(EvaluatorTargetConstants.VERSION, versionId, normalized.params(), now);
    return getVersion(versionId);
  }

  @Transactional
  public EvaluatorConfig publish(String evaluatorId) {
    String draftVersionId = evaluatorRepository.findDraftVersionId(evaluatorId);
    if (!StringUtils.hasText(draftVersionId)) {
      throw new IllegalArgumentException("草稿版本不存在");
    }
    EvaluatorConfig draft = getVersion(draftVersionId);
    if (EvaluatorTypeConstants.CODE.equals(draft.evaluatorType())) {
      throw new IllegalArgumentException("暂不支持Code型评估器");
    }
    int nextVersionNo = evaluatorRepository.nextVersionNo(evaluatorId);
    String newVersionId = id();
    String now = now();
    evaluatorRepository.insertVersion(
        newVersionId,
        evaluatorId,
        nextVersionNo,
        draft.modelId(),
        draft.modelName(),
        draft.prompt(),
        draft.executeCode(),
        draft.scoreMin(),
        draft.scoreMax(),
        draft.passThreshold(),
        now);
    saveParams(EvaluatorTargetConstants.VERSION, newVersionId, draft.params().stream()
        .map(param -> new EvaluatorParamInput(
            null,
            param.paramName(),
            param.dataType(),
            param.defaultValue(),
            param.required(),
            param.description()))
        .toList(), now);
    evaluatorRepository.updateLatestVersion(evaluatorId, newVersionId, now);
    return getVersion(newVersionId);
  }

  @Transactional
  public void deleteEvaluator(String evaluatorId) {
    String evaluatorType = evaluatorRepository.findEvaluatorType(evaluatorId);
    if (!StringUtils.hasText(evaluatorType)) {
      throw new IllegalArgumentException("评估器不存在");
    } else if (!evaluatorRepository.isEvaluatorCreatedByCurrentUser(evaluatorId)) {
      throw new IllegalArgumentException("仅创建人可以删除评估器");
    } else {
      List<String> taskNames = taskRepository.listTaskNamesByEvaluatorId(evaluatorId);
      if (!taskNames.isEmpty()) {
        throw new IllegalArgumentException("评估器已被评测任务“" + String.join("、", taskNames) + "”使用，不能删除");
      } else {
        evaluatorRepository.deleteEvaluator(evaluatorId);
      }
    }
  }

  @Transactional
  public void deleteVersion(String versionId) {
    EvaluatorConfig version = getVersion(versionId);
    if (!evaluatorRepository.isVersionCreatedByCurrentUser(versionId)) {
      throw new IllegalArgumentException("仅创建人可以删除评估器版本");
    } else if (Boolean.TRUE.equals(version.draft())) {
      throw new IllegalArgumentException("草稿版本不能删除");
    } else {
      List<String> taskNames = taskRepository.listTaskNamesByEvaluatorVersionId(versionId);
      if (!taskNames.isEmpty()) {
        throw new IllegalArgumentException("评估器版本已被评测任务“" + String.join("、", taskNames) + "”使用，不能删除");
      } else {
        evaluatorRepository.deleteVersion(versionId);
        List<EvaluatorVersionDto> remainingVersions = evaluatorRepository.listVersions(version.evaluatorId());
        remainingVersions.stream()
            .reduce((previous, current) -> current)
            .ifPresent(latest -> evaluatorRepository.updateLatestVersion(version.evaluatorId(), latest.id(), now()));
      }
    }
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
        base.modelName(),
        base.prompt(),
        base.executeCode(),
        base.scoreMin(),
        base.scoreMax(),
        base.passThreshold(),
        base.createdDate(),
        base.lastUpdatedDate(),
        listEvaluatorParams(EvaluatorTargetConstants.VERSION, base.versionId(), base.evaluatorType(), base.prompt()));
  }

  private List<EvaluatorParamDto> listEvaluatorParams(String targetType, String targetId, String evaluatorType, String prompt) {
    List<EvaluatorParamDto> params = evaluatorRepository.listParams(targetType, targetId);
    if (!EvaluatorTypeConstants.LLM.equals(evaluatorType) || !params.isEmpty() || !StringUtils.hasText(prompt)) {
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
          EvaluatorParamTypeConstants.STRING,
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
    if (EvaluatorTypeConstants.CODE.equals(evaluatorType)) {
      throw new IllegalArgumentException("暂不支持Code型评估器");
    }
    String description = normalizeDescription(request.description());
    BigDecimal scoreMin = request.scoreMin() == null ? DEFAULT_SCORE_MIN : request.scoreMin();
    BigDecimal scoreMax = request.scoreMax() == null ? DEFAULT_SCORE_MAX : request.scoreMax();
    BigDecimal passThreshold = request.passThreshold() == null ? DEFAULT_PASS_THRESHOLD : request.passThreshold();
    validateScore(scoreMin, scoreMax, passThreshold);

    String modelId = "";
    String modelName = "";
    String prompt = "";
    String executeCode = "";
    List<EvaluatorParamInput> params = List.of();
    if (EvaluatorTypeConstants.LLM.equals(evaluatorType)) {
      modelId = request.modelId() == null ? "" : request.modelId().trim();
      modelName = requireText(request.modelName(), "请选择模型");
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
        modelName,
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
    if (!EvaluatorTypeConstants.SUPPORTED_TYPES.contains(normalized)) {
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
          provided == null ? EvaluatorParamTypeConstants.STRING : normalizeParamType(provided.dataType()),
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
    String normalized = StringUtils.hasText(dataType) ? dataType.trim() : EvaluatorParamTypeConstants.STRING;
    if (!EvaluatorParamTypeConstants.SUPPORTED_TYPES.contains(normalized)) {
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
      evaluatorRepository.insertParam(
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

  private TrialEvaluator normalizeTrialEvaluator(EvaluatorInput request) {
    String evaluatorType = normalizeEvaluatorType(request.evaluatorType());
    if (EvaluatorTypeConstants.CODE.equals(evaluatorType)) {
      throw new IllegalArgumentException("暂不支持Code型评估器试运行");
    }
    BigDecimal scoreMin = request.scoreMin() == null ? DEFAULT_SCORE_MIN : request.scoreMin();
    BigDecimal scoreMax = request.scoreMax() == null ? DEFAULT_SCORE_MAX : request.scoreMax();
    BigDecimal passThreshold = request.passThreshold() == null ? DEFAULT_PASS_THRESHOLD : request.passThreshold();
    validateScore(scoreMin, scoreMax, passThreshold);
    String modelId = requireText(request.modelId(), "请选择模型");
    String modelName = requireText(request.modelName(), "请选择模型");
    String prompt = requireText(request.prompt(), "Prompt不能为空");
    validateMaxLength(prompt, MAX_PROMPT_LENGTH, "Prompt不能超过2000个字符");
    List<EvaluatorParamInput> params = normalizePromptParams(prompt, request.params());
    return new TrialEvaluator(modelId, modelName, prompt, scoreMin, scoreMax, passThreshold, params);
  }

  private Map<String, Object> prepareTrialParams(List<EvaluatorParamInput> params, Map<String, String> values) {
    Map<String, Object> prepared = new LinkedHashMap<>();
    Map<String, String> safeValues = values == null ? Map.of() : values;
    for (EvaluatorParamInput param : params) {
      String value = safeValues.get(param.paramName());
      if (!StringUtils.hasText(value) && StringUtils.hasText(param.defaultValue())) {
        value = param.defaultValue();
      }
      prepared.put(param.paramName(), value == null ? "" : value);
    }
    return prepared;
  }

  private String renderPrompt(String prompt, Map<String, Object> params) {
    if (!StringUtils.hasText(prompt)) {
      return "";
    }
    Matcher matcher = PROMPT_PARAM_PATTERN.matcher(prompt);
    StringBuffer rendered = new StringBuffer();
    while (matcher.find()) {
      Object value = params.get(matcher.group(1));
      matcher.appendReplacement(rendered, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
    }
    matcher.appendTail(rendered);
    return rendered.toString();
  }

  private EvaluatorParseResult parseEvaluationOutput(String outputText) {
    if (!StringUtils.hasText(outputText)) {
      return new EvaluatorParseResult(null, "", "模型评估结果为空");
    }
    String json = extractJson(outputText);
    if (!StringUtils.hasText(json)) {
      return new EvaluatorParseResult(null, "", "模型评估结果不是JSON格式");
    }
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode scoreNode = root.get(EvaluationResultConstants.SCORE_FIELD);
      if (scoreNode == null || scoreNode.isNull()) {
        return new EvaluatorParseResult(null, "", "模型评估结果缺少score字段");
      }
      BigDecimal score = scoreNode.isNumber()
          ? scoreNode.decimalValue()
          : new BigDecimal(scoreNode.asText().trim());
      String reason = root.hasNonNull(EvaluationResultConstants.REASON_FIELD)
          ? root.get(EvaluationResultConstants.REASON_FIELD).asText()
          : outputText;
      return new EvaluatorParseResult(score, reason, "");
    } catch (Exception error) {
      return new EvaluatorParseResult(null, "", "模型评估结果解析失败：" + error.getMessage());
    }
  }

  private String extractJson(String outputText) {
    String trimmed = outputText == null ? "" : outputText.trim();
    if (trimmed.startsWith("```")) {
      trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
      int fenceIndex = trimmed.lastIndexOf("```");
      if (fenceIndex >= 0) {
        trimmed = trimmed.substring(0, fenceIndex).trim();
      }
    }
    int start = trimmed.indexOf('{');
    int end = trimmed.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return trimmed.substring(start, end + 1);
    }
    return trimmed.startsWith("{") && trimmed.endsWith("}") ? trimmed : "";
  }

  private boolean scoreOutOfRange(BigDecimal score, TrialEvaluator evaluator) {
    return score.compareTo(evaluator.scoreMin()) < 0 || score.compareTo(evaluator.scoreMax()) > 0;
  }

  private String appendEvaluationNotice(String reason, String notice) {
    if (!StringUtils.hasText(notice)) {
      return reason == null ? "" : reason;
    }
    if (!StringUtils.hasText(reason)) {
      return notice;
    }
    return reason + "\n" + notice;
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
      String modelName,
      String prompt,
      String executeCode,
      BigDecimal scoreMin,
      BigDecimal scoreMax,
      BigDecimal passThreshold,
      List<EvaluatorParamInput> params
  ) {
  }

  private record TrialEvaluator(
      String modelId,
      String modelName,
      String prompt,
      BigDecimal scoreMin,
      BigDecimal scoreMax,
      BigDecimal passThreshold,
      List<EvaluatorParamInput> params
  ) {
  }

  private record EvaluatorParseResult(
      BigDecimal score,
      String reason,
      String errorMessage
  ) {
  }
}
