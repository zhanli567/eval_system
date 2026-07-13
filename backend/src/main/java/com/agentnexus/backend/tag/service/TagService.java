package com.agentnexus.backend.tag.service;

import com.agentnexus.backend.common.PageResponse;
import com.agentnexus.backend.tag.api.dto.response.TagConfig;
import com.agentnexus.backend.tag.api.dto.response.TagDetail;
import com.agentnexus.backend.tag.api.dto.request.TagInput;
import com.agentnexus.backend.tag.api.dto.response.TagOptionDto;
import com.agentnexus.backend.tag.api.dto.request.TagOptionInput;
import com.agentnexus.backend.tag.api.dto.response.TagSummary;
import com.agentnexus.backend.tag.repository.TagRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TagService {
  private static final List<String> SUPPORTED_TAG_TYPES = List.of("category", "boolean", "number", "text");
  private static final List<String> SUPPORTED_OPTION_GROUPS = List.of("pass", "fail");

  private final TagRepository tagRepository;

  public TagService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  public PageResponse<TagSummary> listTags(int page, int size, String tagType, String keyword, String sortBy, String sortOrder) {
    String normalizedType = normalizeOptionalTagType(tagType);
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 100);
    int offset = (safePage - 1) * safeSize;
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    List<TagSummary> records = tagRepository.listTags(normalizedType, like, sortBy, sortOrder, safeSize, offset);
    long total = tagRepository.countTags(normalizedType, like);
    return new PageResponse<>(records, total, safePage, safeSize);
  }

  public TagDetail getTag(String tagId) {
    TagConfig config = findExistingTag(tagId);
    return toDetail(config);
  }

  @Transactional
  public TagDetail createTag(TagInput request) {
    NormalizedTag normalized = normalizeTagInput(request, null);
    String tagId = id();
    String now = now();
    if (tagRepository.countSameName(normalized.tagName()) > 0) {
      throw new IllegalArgumentException("当前空间已存在同名标签");
    } else {
      tagRepository.insertTag(
          tagId,
          normalized.tagName(),
          normalized.tagType(),
          normalized.description(),
          normalized.minValue(),
          normalized.maxValue(),
          normalized.passThreshold(),
          now);
    }
    saveOptions(tagId, normalized.tagType(), normalized.options(), now);
    return getTag(tagId);
  }

  @Transactional
  public TagDetail updateTag(String tagId, TagInput request) {
    TagConfig existing = findExistingTag(tagId);
    NormalizedTag normalized = normalizeTagInput(request, existing.tagType());
    if (!existing.tagType().equals(normalized.tagType())) {
      throw new IllegalArgumentException("标签类型创建后不能修改");
    }
    if (tagRepository.countSameNameExcept(normalized.tagName(), tagId) > 0) {
      throw new IllegalArgumentException("标签名称不能重复");
    }

    String now = now();
    tagRepository.updateTag(
        tagId,
        normalized.tagName(),
        normalized.description(),
        normalized.minValue(),
        normalized.maxValue(),
        normalized.passThreshold(),
        now);
    tagRepository.deleteOptions(tagId);
    saveOptions(tagId, normalized.tagType(), normalized.options(), now);
    return getTag(tagId);
  }

  @Transactional
  public void deleteTag(String tagId) {
    findExistingTag(tagId);
    if (tagRepository.countTaskBindings(tagId) > 0) {
      throw new IllegalArgumentException("标签已被评测任务使用，不能删除");
    }
    tagRepository.deleteTag(tagId);
  }

  private TagDetail toDetail(TagConfig config) {
    return new TagDetail(
        config.id(),
        config.tagName(),
        config.tagType(),
        config.description(),
        config.minValue(),
        config.maxValue(),
        config.passThreshold(),
        config.createdDate(),
        config.lastUpdatedDate(),
        tagRepository.listOptions(config.id()));
  }

  private TagConfig findExistingTag(String tagId) {
    if (!StringUtils.hasText(tagId)) {
      throw new IllegalArgumentException("标签ID不能为空");
    }
    TagConfig config = tagRepository.findTagConfig(tagId);
    if (config == null) {
      throw new IllegalArgumentException("标签不存在");
    }
    return config;
  }

  private NormalizedTag normalizeTagInput(TagInput request, String existingTagType) {
    if (request == null) {
      throw new IllegalArgumentException("标签参数不能为空");
    }
    String tagName = normalizeTagName(request.tagName());
    String tagType = normalizeTagType(StringUtils.hasText(request.tagType()) ? request.tagType() : existingTagType);
    String description = normalizeDescription(request.description());

    Integer minValue = null;
    Integer maxValue = null;
    Integer passThreshold = null;
    List<TagOptionInput> options = List.of();
    if ("category".equals(tagType)) {
      options = normalizeCategoryOptions(request.options());
    } else if ("boolean".equals(tagType)) {
      options = List.of(
          new TagOptionInput(null, "True", "pass"),
          new TagOptionInput(null, "False", "fail"));
    } else if ("number".equals(tagType)) {
      minValue = request.minValue();
      maxValue = request.maxValue();
      passThreshold = request.passThreshold();
      validateNumberConfig(minValue, maxValue, passThreshold);
    }
    return new NormalizedTag(tagName, tagType, description, minValue, maxValue, passThreshold, options);
  }

  private String normalizeTagName(String tagName) {
    if (!StringUtils.hasText(tagName)) {
      throw new IllegalArgumentException("标签名称不能为空");
    }
    String normalized = tagName.trim();
    if (normalized.length() > 50) {
      throw new IllegalArgumentException("标签名称不能超过50个字符");
    }
    return normalized;
  }

  private String normalizeDescription(String description) {
    String normalized = description == null ? "" : description.trim();
    if (normalized.length() > 200) {
      throw new IllegalArgumentException("标签描述不能超过200个字符");
    }
    return normalized;
  }

  private String normalizeOptionalTagType(String tagType) {
    if (!StringUtils.hasText(tagType)) {
      return null;
    }
    return normalizeTagType(tagType);
  }

  private String normalizeTagType(String tagType) {
    if (!StringUtils.hasText(tagType)) {
      throw new IllegalArgumentException("标签类型不能为空");
    }
    String normalized = tagType.trim();
    if (!SUPPORTED_TAG_TYPES.contains(normalized)) {
      throw new IllegalArgumentException("标签类型仅支持category、boolean、number、text");
    }
    return normalized;
  }

  private List<TagOptionInput> normalizeCategoryOptions(List<TagOptionInput> options) {
    if (options == null || options.isEmpty()) {
      throw new IllegalArgumentException("分类标签请至少配置一个Pass选项和一个Fail选项");
    }
    List<TagOptionInput> normalized = new ArrayList<>();
    Set<String> optionNames = new HashSet<>();
    boolean hasPass = false;
    boolean hasFail = false;
    int passCount = 0;
    int failCount = 0;
    for (TagOptionInput option : options) {
      if (option == null || !StringUtils.hasText(option.optionName())) {
        continue;
      }
      String optionName = option.optionName().trim();
      if (optionName.length() > 50) {
        throw new IllegalArgumentException("标签选项不能超过50个字符");
      }
      if (!optionNames.add(optionName)) {
        throw new IllegalArgumentException("标签选项不能重复");
      }
      String optionGroup = normalizeOptionGroup(option.optionGroup());
      hasPass = hasPass || "pass".equals(optionGroup);
      hasFail = hasFail || "fail".equals(optionGroup);
      passCount += "pass".equals(optionGroup) ? 1 : 0;
      failCount += "fail".equals(optionGroup) ? 1 : 0;
      normalized.add(new TagOptionInput(option.id(), optionName, optionGroup));
    }
    if (!hasPass || !hasFail) {
      throw new IllegalArgumentException("分类标签请至少配置一个Pass选项和一个Fail选项");
    }
    if (passCount > 5 || failCount > 5) {
      throw new IllegalArgumentException("Pass和Fail选项每组最多支持5个");
    }
    return normalized;
  }

  private String normalizeOptionGroup(String optionGroup) {
    if (!StringUtils.hasText(optionGroup)) {
      throw new IllegalArgumentException("选项分组不能为空");
    }
    String normalized = optionGroup.trim();
    if (!SUPPORTED_OPTION_GROUPS.contains(normalized)) {
      throw new IllegalArgumentException("选项分组仅支持pass、fail");
    }
    return normalized;
  }

  private void validateNumberConfig(Integer minValue, Integer maxValue, Integer passThreshold) {
    if (minValue == null || maxValue == null || passThreshold == null) {
      throw new IllegalArgumentException("数字标签请维护评分范围和通过阈值");
    }
    if (minValue <= 0 || maxValue <= 0 || passThreshold <= 0) {
      throw new IllegalArgumentException("数字标签评分范围和通过阈值必须为正整数");
    }
    if (minValue >= maxValue) {
      throw new IllegalArgumentException("数字标签评分范围最大值必须大于最小值");
    }
    if (passThreshold < minValue || passThreshold > maxValue) {
      throw new IllegalArgumentException("通过阈值必须介于评分范围最小值和最大值之间");
    }
  }

  private void saveOptions(String tagId, String tagType, List<TagOptionInput> options, String now) {
    if (!"category".equals(tagType) && !"boolean".equals(tagType)) {
      return;
    }
    int order = 1;
    for (TagOptionInput option : options) {
      tagRepository.insertOption(id(), tagId, option.optionName(), option.optionGroup(), order++, now);
    }
  }

  private String id() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private String now() {
    return String.valueOf(System.currentTimeMillis());
  }

  private record NormalizedTag(
      String tagName,
      String tagType,
      String description,
      Integer minValue,
      Integer maxValue,
      Integer passThreshold,
      List<TagOptionInput> options
  ) {
  }
}
