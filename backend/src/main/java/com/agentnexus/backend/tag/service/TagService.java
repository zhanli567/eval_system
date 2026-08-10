package com.agentnexus.backend.tag.service;

import com.agentnexus.backend.common.PageResponse;
import com.agentnexus.backend.tag.api.dto.response.TagConfig;
import com.agentnexus.backend.tag.api.dto.response.TagDetail;
import com.agentnexus.backend.tag.api.dto.request.TagInput;
import com.agentnexus.backend.tag.api.dto.response.TagOptionDto;
import com.agentnexus.backend.tag.api.dto.request.TagOptionInput;
import com.agentnexus.backend.tag.api.dto.response.TagSummary;
import com.agentnexus.backend.tag.constant.TagErrorMessageConstants;
import com.agentnexus.backend.tag.constant.TagOptionGroupConstants;
import com.agentnexus.backend.tag.constant.TagTypeConstants;
import com.agentnexus.backend.tag.repository.TagRepository;
import com.agentnexus.backend.task.repository.TaskRepository;
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
  private final TagRepository tagRepository;
  private final TaskRepository taskRepository;

  public TagService(TagRepository tagRepository, TaskRepository taskRepository) {
    this.tagRepository = tagRepository;
    this.taskRepository = taskRepository;
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
      throw new IllegalArgumentException(TagErrorMessageConstants.DUPLICATE_TAG_NAME_IN_SPACE);
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
      throw new IllegalArgumentException(TagErrorMessageConstants.TAG_TYPE_CANNOT_MODIFY);
    }
    if (tagRepository.countSameNameExcept(normalized.tagName(), tagId) > 0) {
      throw new IllegalArgumentException(TagErrorMessageConstants.DUPLICATE_TAG_NAME);
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
    if (!tagRepository.isTagCreatedByCurrentUser(tagId)) {
      throw new IllegalArgumentException(TagErrorMessageConstants.ONLY_CREATOR_CAN_DELETE_TAG);
    } else {
      List<String> taskNames = taskRepository.listTaskNamesByTagId(tagId);
      if (!taskNames.isEmpty()) {
        throw new IllegalArgumentException(TagErrorMessageConstants.tagUsedByTasks(taskNames));
      } else {
        tagRepository.deleteTag(tagId);
      }
    }
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
      throw new IllegalArgumentException(TagErrorMessageConstants.TAG_ID_REQUIRED);
    }
    TagConfig config = tagRepository.findTagConfig(tagId);
    if (config == null) {
      throw new IllegalArgumentException(TagErrorMessageConstants.TAG_NOT_FOUND);
    }
    return config;
  }

  private NormalizedTag normalizeTagInput(TagInput request, String existingTagType) {
    if (request == null) {
      throw new IllegalArgumentException(TagErrorMessageConstants.TAG_REQUEST_REQUIRED);
    }
    String tagName = normalizeTagName(request.tagName());
    String tagType = normalizeTagType(StringUtils.hasText(request.tagType()) ? request.tagType() : existingTagType);
    String description = normalizeDescription(request.description());

    Integer minValue = null;
    Integer maxValue = null;
    Integer passThreshold = null;
    List<TagOptionInput> options = List.of();
    if (TagTypeConstants.CATEGORY.equals(tagType)) {
      options = normalizeCategoryOptions(request.options());
    } else if (TagTypeConstants.BOOLEAN.equals(tagType)) {
      options = List.of(
          new TagOptionInput(null, TagOptionGroupConstants.TRUE_OPTION_NAME, TagOptionGroupConstants.PASS),
          new TagOptionInput(null, TagOptionGroupConstants.FALSE_OPTION_NAME, TagOptionGroupConstants.FAIL));
    } else if (TagTypeConstants.NUMBER.equals(tagType)) {
      minValue = request.minValue();
      maxValue = request.maxValue();
      passThreshold = request.passThreshold();
      validateNumberConfig(minValue, maxValue, passThreshold);
    }
    return new NormalizedTag(tagName, tagType, description, minValue, maxValue, passThreshold, options);
  }

  private String normalizeTagName(String tagName) {
    if (!StringUtils.hasText(tagName)) {
      throw new IllegalArgumentException(TagErrorMessageConstants.TAG_NAME_REQUIRED);
    }
    String normalized = tagName.trim();
    if (normalized.length() > 50) {
      throw new IllegalArgumentException(TagErrorMessageConstants.TAG_NAME_TOO_LONG);
    }
    return normalized;
  }

  private String normalizeDescription(String description) {
    String normalized = description == null ? "" : description.trim();
    if (normalized.length() > 200) {
      throw new IllegalArgumentException(TagErrorMessageConstants.TAG_DESCRIPTION_TOO_LONG);
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
      throw new IllegalArgumentException(TagErrorMessageConstants.TAG_TYPE_REQUIRED);
    }
    String normalized = tagType.trim();
    if (!TagTypeConstants.SUPPORTED_TYPES.contains(normalized)) {
      throw new IllegalArgumentException(TagErrorMessageConstants.UNSUPPORTED_TAG_TYPE);
    }
    return normalized;
  }

  private List<TagOptionInput> normalizeCategoryOptions(List<TagOptionInput> options) {
    if (options == null || options.isEmpty()) {
      throw new IllegalArgumentException(TagErrorMessageConstants.CATEGORY_OPTIONS_REQUIRED);
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
        throw new IllegalArgumentException(TagErrorMessageConstants.TAG_OPTION_TOO_LONG);
      }
      if (!optionNames.add(optionName)) {
        throw new IllegalArgumentException(TagErrorMessageConstants.TAG_OPTION_DUPLICATED);
      }
      String optionGroup = normalizeOptionGroup(option.optionGroup());
      hasPass = hasPass || TagOptionGroupConstants.PASS.equals(optionGroup);
      hasFail = hasFail || TagOptionGroupConstants.FAIL.equals(optionGroup);
      passCount += TagOptionGroupConstants.PASS.equals(optionGroup) ? 1 : 0;
      failCount += TagOptionGroupConstants.FAIL.equals(optionGroup) ? 1 : 0;
      normalized.add(new TagOptionInput(option.id(), optionName, optionGroup));
    }
    if (!hasPass || !hasFail) {
      throw new IllegalArgumentException(TagErrorMessageConstants.CATEGORY_OPTIONS_REQUIRED);
    }
    if (passCount > 5 || failCount > 5) {
      throw new IllegalArgumentException(TagErrorMessageConstants.CATEGORY_OPTION_LIMIT_EXCEEDED);
    }
    return normalized;
  }

  private String normalizeOptionGroup(String optionGroup) {
    if (!StringUtils.hasText(optionGroup)) {
      throw new IllegalArgumentException(TagErrorMessageConstants.OPTION_GROUP_REQUIRED);
    }
    String normalized = optionGroup.trim();
    if (!TagOptionGroupConstants.SUPPORTED_GROUPS.contains(normalized)) {
      throw new IllegalArgumentException(TagErrorMessageConstants.UNSUPPORTED_OPTION_GROUP);
    }
    return normalized;
  }

  private void validateNumberConfig(Integer minValue, Integer maxValue, Integer passThreshold) {
    if (minValue == null || maxValue == null || passThreshold == null) {
      throw new IllegalArgumentException(TagErrorMessageConstants.NUMBER_TAG_SCORE_REQUIRED);
    }
    if (minValue <= 0 || maxValue <= 0 || passThreshold <= 0) {
      throw new IllegalArgumentException(TagErrorMessageConstants.NUMBER_TAG_SCORE_MUST_BE_POSITIVE);
    }
    if (minValue >= maxValue) {
      throw new IllegalArgumentException(TagErrorMessageConstants.NUMBER_TAG_SCORE_RANGE_INVALID);
    }
    if (passThreshold < minValue || passThreshold > maxValue) {
      throw new IllegalArgumentException(TagErrorMessageConstants.NUMBER_TAG_THRESHOLD_INVALID);
    }
  }

  private void saveOptions(String tagId, String tagType, List<TagOptionInput> options, String now) {
    if (!TagTypeConstants.CATEGORY.equals(tagType) && !TagTypeConstants.BOOLEAN.equals(tagType)) {
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
