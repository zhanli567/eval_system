package com.agentnexus.backend.tag.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.agentnexus.backend.common.context.RepositoryContext;
import com.agentnexus.backend.tag.api.dto.response.TagConfig;
import com.agentnexus.backend.tag.api.dto.response.TagOptionDto;
import com.agentnexus.backend.tag.api.dto.response.TagSummary;
import com.agentnexus.backend.tag.mapper.TagMapper;
import com.agentnexus.backend.tag.mapper.TagOptionMapper;
import com.agentnexus.backend.tag.entity.EvalTag;
import com.agentnexus.backend.tag.entity.EvalTagOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class TagRepository {
  private final TagMapper tagMapper;
  private final TagOptionMapper optionMapper;

  public TagRepository(TagMapper tagMapper, TagOptionMapper optionMapper) {
    this.tagMapper = tagMapper;
    this.optionMapper = optionMapper;
  }

  public List<TagSummary> listTags(String tagType, String like, int size, int offset) {
    return tagMapper.selectList(tagQuery(tagType, like)
            .orderByDesc(EvalTag::getCreatedDate)
            .last("LIMIT " + size + " OFFSET " + offset))
        .stream()
        .map(this::toSummary)
        .toList();
  }

  public long countTags(String tagType, String like) {
    return tagMapper.selectCount(tagQuery(tagType, like));
  }

  public TagConfig findTagConfig(String tagId) {
    EvalTag tag = tagMapper.selectOne(new LambdaQueryWrapper<EvalTag>()
        .eq(EvalTag::getSpaceId, RepositoryContext.spaceId())
        .eq(EvalTag::getId, tagId)
        .last("LIMIT 1"));
    return tag == null ? null : toConfig(tag);
  }

  public List<TagOptionDto> listOptions(String tagId) {
    return optionMapper.selectList(new LambdaQueryWrapper<EvalTagOption>()
            .eq(EvalTagOption::getSpaceId, RepositoryContext.spaceId())
            .eq(EvalTagOption::getTagId, tagId)
            .orderByAsc(EvalTagOption::getDisplayOrder))
        .stream()
        .map(this::toOption)
        .toList();
  }

  public String findTagType(String tagId) {
    EvalTag tag = tagMapper.selectOne(new LambdaQueryWrapper<EvalTag>()
        .select(EvalTag::getTagType)
        .eq(EvalTag::getSpaceId, RepositoryContext.spaceId())
        .eq(EvalTag::getId, tagId)
        .last("LIMIT 1"));
    return tag == null ? null : tag.getTagType();
  }

  public int countSameNameExcept(String tagName, String tagId) {
    return Math.toIntExact(tagMapper.selectCount(new LambdaQueryWrapper<EvalTag>()
        .eq(EvalTag::getSpaceId, RepositoryContext.spaceId())
        .eq(EvalTag::getTagName, tagName)
        .ne(EvalTag::getId, tagId)));
  }

  public int countSameName(String tagName) {
    return Math.toIntExact(tagMapper.selectCount(new LambdaQueryWrapper<EvalTag>()
        .eq(EvalTag::getSpaceId, RepositoryContext.spaceId())
        .eq(EvalTag::getTagName, tagName)));
  }

  public void insertTag(
      String tagId,
      String tagName,
      String tagType,
      String description,
      Integer minValue,
      Integer maxValue,
      Integer passThreshold,
      String now
  ) {
    EvalTag tag = new EvalTag();
    tag.setId(tagId);
    tag.setTagName(tagName);
    tag.setTagType(tagType);
    tag.setDescription(description);
    tag.setMinValue(minValue);
    tag.setMaxValue(maxValue);
    tag.setPassThreshold(passThreshold);
    tag.setLastUpdatedDate(toLastUpdatedDate(now));
    RepositoryContext.fillCreated(tag);
    tagMapper.insert(tag);
  }

  public void updateTag(
      String tagId,
      String tagName,
      String description,
      Integer minValue,
      Integer maxValue,
      Integer passThreshold,
      String now
  ) {
    tagMapper.update(null, new LambdaUpdateWrapper<EvalTag>()
        .eq(EvalTag::getSpaceId, RepositoryContext.spaceId())
        .eq(EvalTag::getId, tagId)
        .set(EvalTag::getTagName, tagName)
        .set(EvalTag::getDescription, description)
        .set(EvalTag::getMinValue, minValue)
        .set(EvalTag::getMaxValue, maxValue)
        .set(EvalTag::getPassThreshold, passThreshold)
        .set(EvalTag::getLastUpdatedBy, RepositoryContext.userId())
        .set(EvalTag::getLastUpdatedByName, RepositoryContext.displayName())
        .set(EvalTag::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void deleteOptions(String tagId) {
    optionMapper.delete(new LambdaQueryWrapper<EvalTagOption>()
        .eq(EvalTagOption::getSpaceId, RepositoryContext.spaceId())
        .eq(EvalTagOption::getTagId, tagId));
  }

  public void insertOption(String optionId, String tagId, String optionName, String optionGroup, int displayOrder, String now) {
    EvalTagOption option = new EvalTagOption();
    option.setId(optionId);
    option.setTagId(tagId);
    option.setOptionName(optionName);
    option.setOptionGroup(optionGroup);
    option.setDisplayOrder(displayOrder);
    option.setLastUpdatedDate(toLastUpdatedDate(now));
    RepositoryContext.fillCreated(option);
    optionMapper.insert(option);
  }

  private LambdaQueryWrapper<EvalTag> tagQuery(String tagType, String like) {
    return new LambdaQueryWrapper<EvalTag>()
        .eq(EvalTag::getSpaceId, RepositoryContext.spaceId())
        .eq(StringUtils.hasText(tagType), EvalTag::getTagType, tagType)
        .like(hasLikeText(like), EvalTag::getTagName, likeText(like));
  }

  private TagSummary toSummary(EvalTag tag) {
    return new TagSummary(
        tag.getId(),
        tag.getTagName(),
        tag.getTagType(),
        tag.getDescription(),
        tag.getCreatedDate(),
        tag.getLastUpdatedDate());
  }

  private TagConfig toConfig(EvalTag tag) {
    return new TagConfig(
        tag.getId(),
        tag.getTagName(),
        tag.getTagType(),
        tag.getDescription(),
        tag.getMinValue(),
        tag.getMaxValue(),
        tag.getPassThreshold(),
        tag.getCreatedDate(),
        tag.getLastUpdatedDate());
  }

  private TagOptionDto toOption(EvalTagOption option) {
    return new TagOptionDto(
        option.getId(),
        option.getTagId(),
        option.getOptionName(),
        option.getOptionGroup(),
        option.getDisplayOrder(),
        option.getCreatedDate(),
        option.getLastUpdatedDate());
  }

  private LocalDateTime toLastUpdatedDate(String now) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(now)), ZoneId.systemDefault());
  }

  private boolean hasLikeText(String like) {
    return StringUtils.hasText(like) && !"%%".equals(like);
  }

  private String likeText(String like) {
    return hasLikeText(like) && like.length() > 1 ? like.substring(1, like.length() - 1) : "";
  }
}
