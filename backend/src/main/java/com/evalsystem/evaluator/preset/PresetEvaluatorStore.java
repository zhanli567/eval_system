package com.evalsystem.evaluator.preset;

import com.evalsystem.common.PageResponse;
import com.evalsystem.evaluator.dto.EvaluatorParamDto;
import com.evalsystem.evaluator.dto.PresetCategoryDto;
import com.evalsystem.evaluator.dto.PresetEvaluatorDetail;
import com.evalsystem.evaluator.dto.PresetEvaluatorSummary;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PresetEvaluatorStore {
  private static final String TYPE_LLM = "llm";
  private static final String TYPE_CODE = "code";
  private static final String TARGET_PRESET = "preset";
  private static final String PARAM_TYPE_STRING = "string";
  private static final List<String> SUPPORTED_PARAM_TYPES = List.of(PARAM_TYPE_STRING, "number", "boolean");
  private static final Pattern PROMPT_PARAM_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][\\w]*)}");
  private static final Pattern PARAM_NAME_PATTERN = Pattern.compile("[a-zA-Z_][\\w]*");

  private final List<PresetCategoryDefinition> categories;
  private final List<PresetEvaluatorDefinition> evaluators;
  private final Map<String, PresetCategoryDefinition> categoryById;
  private final Map<String, PresetEvaluatorDefinition> evaluatorById;

  public PresetEvaluatorStore() {
    this(PresetEvaluatorCatalog.categories(), PresetEvaluatorCatalog.evaluators());
  }

  PresetEvaluatorStore(
      List<PresetCategoryDefinition> categories,
      List<PresetEvaluatorDefinition> evaluators
  ) {
    this.categoryById = indexCategories(categories);
    this.evaluatorById = indexEvaluators(evaluators);
    this.categories = categories.stream()
        .sorted(Comparator.comparingInt(category -> safeOrder(category.displayOrder())))
        .toList();
    this.evaluators = evaluators.stream()
        .sorted(this::compareEvaluator)
        .toList();
  }

  public List<PresetCategoryDto> listCategories() {
    return categories.stream()
        .map(category -> new PresetCategoryDto(category.id(), category.categoryName(), category.displayOrder()))
        .toList();
  }

  public PageResponse<PresetEvaluatorSummary> listEvaluators(int page, int size, String categoryId, String keyword) {
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 100);
    int offset = (safePage - 1) * safeSize;
    String safeCategoryId = StringUtils.hasText(categoryId) ? categoryId.trim() : "";
    String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    List<PresetEvaluatorDefinition> filtered = evaluators.stream()
        .filter(evaluator -> !StringUtils.hasText(safeCategoryId) || evaluator.categoryId().equals(safeCategoryId))
        .filter(evaluator -> !StringUtils.hasText(safeKeyword)
            || evaluator.evaluatorName().toLowerCase(Locale.ROOT).contains(safeKeyword))
        .toList();
    List<PresetEvaluatorSummary> records = filtered.stream()
        .skip(offset)
        .limit(safeSize)
        .map(this::toSummary)
        .toList();
    return new PageResponse<>(records, filtered.size(), safePage, safeSize);
  }

  public PresetEvaluatorDetail getPresetEvaluator(String presetId) {
    String safePresetId = requireText(presetId, "预置评估器不存在");
    PresetEvaluatorDefinition evaluator = evaluatorById.get(safePresetId);
    if (evaluator == null) {
      throw new IllegalArgumentException("预置评估器不存在");
    }
    return new PresetEvaluatorDetail(
        evaluator.id(),
        evaluator.categoryId(),
        categoryName(evaluator.categoryId()),
        evaluator.evaluatorName(),
        evaluator.evaluatorType(),
        evaluator.description(),
        evaluator.modelId(),
        evaluator.prompt(),
        evaluator.executeCode(),
        evaluator.scoreMin(),
        evaluator.scoreMax(),
        evaluator.passThreshold(),
        evaluator.createdAt(),
        evaluator.updatedAt(),
        toParamDtos(evaluator));
  }

  private PresetEvaluatorSummary toSummary(PresetEvaluatorDefinition evaluator) {
    return new PresetEvaluatorSummary(
        evaluator.id(),
        evaluator.categoryId(),
        categoryName(evaluator.categoryId()),
        evaluator.evaluatorName(),
        evaluator.evaluatorType(),
        evaluator.description());
  }

  private List<EvaluatorParamDto> toParamDtos(PresetEvaluatorDefinition evaluator) {
    List<PresetParamDefinition> params = resolveParams(evaluator);
    List<EvaluatorParamDto> result = new ArrayList<>();
    int order = 1;
    for (PresetParamDefinition param : params) {
      String paramName = requireText(param.paramName(), "预置评估器参数名不能为空");
      result.add(new EvaluatorParamDto(
          evaluator.id() + ":" + paramName,
          TARGET_PRESET,
          evaluator.id(),
          paramName,
          normalizeParamType(param.dataType()),
          param.defaultValue() == null ? "" : param.defaultValue(),
          param.required() == null || param.required(),
          param.description() == null ? "" : param.description(),
          order++));
    }
    return result;
  }

  private List<PresetParamDefinition> resolveParams(PresetEvaluatorDefinition evaluator) {
    if (!TYPE_LLM.equals(evaluator.evaluatorType())) {
      return evaluator.params();
    }
    Map<String, PresetParamDefinition> configuredParams = new LinkedHashMap<>();
    for (PresetParamDefinition param : evaluator.params()) {
      configuredParams.put(param.paramName(), param);
    }
    List<PresetParamDefinition> resolved = new ArrayList<>();
    for (String paramName : extractPromptParamNames(evaluator.prompt())) {
      PresetParamDefinition configured = configuredParams.get(paramName);
      resolved.add(configured == null
          ? new PresetParamDefinition(paramName, PARAM_TYPE_STRING, "", true, "")
          : configured);
    }
    return resolved;
  }

  private Map<String, PresetCategoryDefinition> indexCategories(List<PresetCategoryDefinition> categories) {
    Map<String, PresetCategoryDefinition> indexed = new LinkedHashMap<>();
    for (PresetCategoryDefinition category : categories) {
      String id = requireText(category.id(), "预置评估器分类ID不能为空");
      requireText(category.categoryName(), "预置评估器分类名称不能为空");
      if (indexed.putIfAbsent(id, category) != null) {
        throw new IllegalStateException("预置评估器分类ID重复：" + id);
      }
    }
    return indexed;
  }

  private Map<String, PresetEvaluatorDefinition> indexEvaluators(List<PresetEvaluatorDefinition> evaluators) {
    Map<String, PresetEvaluatorDefinition> indexed = new LinkedHashMap<>();
    for (PresetEvaluatorDefinition evaluator : evaluators) {
      validateEvaluator(evaluator);
      if (indexed.putIfAbsent(evaluator.id(), evaluator) != null) {
        throw new IllegalStateException("预置评估器ID重复：" + evaluator.id());
      }
    }
    return indexed;
  }

  private void validateEvaluator(PresetEvaluatorDefinition evaluator) {
    requireText(evaluator.id(), "预置评估器ID不能为空");
    requireText(evaluator.evaluatorName(), "预置评估器名称不能为空");
    if (!categoryById.containsKey(evaluator.categoryId())) {
      throw new IllegalStateException("预置评估器分类不存在：" + evaluator.categoryId());
    }
    if (!List.of(TYPE_LLM, TYPE_CODE).contains(evaluator.evaluatorType())) {
      throw new IllegalStateException("预置评估器类型仅支持llm/code：" + evaluator.id());
    }
    validateScore(evaluator);
    validateParams(evaluator);
  }

  private void validateScore(PresetEvaluatorDefinition evaluator) {
    BigDecimal min = evaluator.scoreMin();
    BigDecimal max = evaluator.scoreMax();
    BigDecimal pass = evaluator.passThreshold();
    if (min == null || max == null || pass == null || min.compareTo(max) >= 0) {
      throw new IllegalStateException("预置评估器评分范围不合法：" + evaluator.id());
    }
    if (pass.compareTo(min) < 0 || pass.compareTo(max) > 0) {
      throw new IllegalStateException("预置评估器通过阈值不在评分范围内：" + evaluator.id());
    }
  }

  private void validateParams(PresetEvaluatorDefinition evaluator) {
    if (TYPE_LLM.equals(evaluator.evaluatorType())) {
      requireText(evaluator.prompt(), "LLM预置评估器Prompt不能为空：" + evaluator.id());
      List<String> promptParams = extractPromptParamNames(evaluator.prompt());
      if (promptParams.isEmpty()) {
        throw new IllegalStateException("LLM预置评估器Prompt至少需要一个${参数名}：" + evaluator.id());
      }
      Set<String> promptParamSet = new HashSet<>(promptParams);
      for (PresetParamDefinition param : evaluator.params()) {
        if (!promptParamSet.contains(param.paramName())) {
          throw new IllegalStateException("LLM预置评估器参数未在Prompt中引用：" + param.paramName());
        }
      }
    } else {
      requireText(evaluator.executeCode(), "Code预置评估器执行函数不能为空：" + evaluator.id());
      if (evaluator.params().isEmpty()) {
        throw new IllegalStateException("Code预置评估器至少需要一个参数：" + evaluator.id());
      }
    }
    Set<String> paramNames = new HashSet<>();
    for (PresetParamDefinition param : resolveParams(evaluator)) {
      String paramName = requireText(param.paramName(), "预置评估器参数名不能为空");
      if (!PARAM_NAME_PATTERN.matcher(paramName).matches()) {
        throw new IllegalStateException("预置评估器参数名不合法：" + paramName);
      }
      if (!paramNames.add(paramName)) {
        throw new IllegalStateException("预置评估器参数名重复：" + paramName);
      }
      normalizeParamType(param.dataType());
    }
  }

  private List<String> extractPromptParamNames(String prompt) {
    List<String> names = new ArrayList<>();
    Matcher matcher = PROMPT_PARAM_PATTERN.matcher(prompt == null ? "" : prompt);
    while (matcher.find()) {
      String paramName = matcher.group(1);
      if (!names.contains(paramName)) {
        names.add(paramName);
      }
    }
    return names;
  }

  private String normalizeParamType(String dataType) {
    String normalized = StringUtils.hasText(dataType) ? dataType.trim().toLowerCase(Locale.ROOT) : PARAM_TYPE_STRING;
    if (!SUPPORTED_PARAM_TYPES.contains(normalized)) {
      throw new IllegalStateException("预置评估器参数类型仅支持string/number/boolean：" + dataType);
    }
    return normalized;
  }

  private String categoryName(String categoryId) {
    PresetCategoryDefinition category = categoryById.get(categoryId);
    return category == null ? "" : category.categoryName();
  }

  private int compareEvaluator(PresetEvaluatorDefinition left, PresetEvaluatorDefinition right) {
    int categoryCompare = Integer.compare(
        safeOrder(categoryById.get(left.categoryId()).displayOrder()),
        safeOrder(categoryById.get(right.categoryId()).displayOrder()));
    if (categoryCompare != 0) {
      return categoryCompare;
    }
    return Integer.compare(safeOrder(left.displayOrder()), safeOrder(right.displayOrder()));
  }

  private int safeOrder(Integer value) {
    return value == null ? Integer.MAX_VALUE : value;
  }

  private String requireText(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }
}
