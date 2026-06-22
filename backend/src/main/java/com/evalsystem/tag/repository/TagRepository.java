package com.evalsystem.tag.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.evalsystem.tag.dto.TagConfig;
import com.evalsystem.tag.dto.TagOptionDto;
import com.evalsystem.tag.dto.TagSummary;
import com.evalsystem.tag.mapper.TagMapper;
import com.evalsystem.tag.mapper.TagOptionMapper;
import com.evalsystem.tag.pojo.EvalTag;
import com.evalsystem.tag.pojo.EvalTagOption;
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
            .orderByDesc(EvalTag::getCreatedAt)
            .last("LIMIT " + size + " OFFSET " + offset))
        .stream()
        .map(this::toSummary)
        .toList();
  }

  public long countTags(String tagType, String like) {
    return tagMapper.selectCount(tagQuery(tagType, like));
  }

  public TagConfig findTagConfig(String tagId) {
    EvalTag tag = tagMapper.selectById(tagId);
    return tag == null ? null : toConfig(tag);
  }

  public List<TagOptionDto> listOptions(String tagId) {
    return optionMapper.selectList(new LambdaQueryWrapper<EvalTagOption>()
            .eq(EvalTagOption::getTagId, tagId)
            .orderByAsc(EvalTagOption::getDisplayOrder))
        .stream()
        .map(this::toOption)
        .toList();
  }

  public String findTagType(String tagId) {
    EvalTag tag = tagMapper.selectOne(new LambdaQueryWrapper<EvalTag>()
        .select(EvalTag::getTagType)
        .eq(EvalTag::getId, tagId)
        .last("LIMIT 1"));
    return tag == null ? null : tag.getTagType();
  }

  public int countSameNameExcept(String tagName, String tagId) {
    return Math.toIntExact(tagMapper.selectCount(new LambdaQueryWrapper<EvalTag>()
        .eq(EvalTag::getTagName, tagName)
        .ne(EvalTag::getId, tagId)));
  }

  public int countSameName(String tagName) {
    return Math.toIntExact(tagMapper.selectCount(new LambdaQueryWrapper<EvalTag>()
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
    tag.setCreatedAt(now);
    tag.setUpdatedAt(now);
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
        .eq(EvalTag::getId, tagId)
        .set(EvalTag::getTagName, tagName)
        .set(EvalTag::getDescription, description)
        .set(EvalTag::getMinValue, minValue)
        .set(EvalTag::getMaxValue, maxValue)
        .set(EvalTag::getPassThreshold, passThreshold)
        .set(EvalTag::getUpdatedAt, now));
  }

  public void deleteOptions(String tagId) {
    optionMapper.delete(new LambdaQueryWrapper<EvalTagOption>()
        .eq(EvalTagOption::getTagId, tagId));
  }

  public void insertOption(String optionId, String tagId, String optionName, String optionGroup, int displayOrder, String now) {
    EvalTagOption option = new EvalTagOption();
    option.setId(optionId);
    option.setTagId(tagId);
    option.setOptionName(optionName);
    option.setOptionGroup(optionGroup);
    option.setDisplayOrder(displayOrder);
    option.setCreatedAt(now);
    option.setUpdatedAt(now);
    optionMapper.insert(option);
  }

  private LambdaQueryWrapper<EvalTag> tagQuery(String tagType, String like) {
    return new LambdaQueryWrapper<EvalTag>()
        .eq(StringUtils.hasText(tagType), EvalTag::getTagType, tagType)
        .like(hasLikeText(like), EvalTag::getTagName, likeText(like));
  }

  private TagSummary toSummary(EvalTag tag) {
    return new TagSummary(
        tag.getId(),
        tag.getTagName(),
        tag.getTagType(),
        tag.getDescription(),
        tag.getCreatedAt(),
        tag.getUpdatedAt());
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
        tag.getCreatedAt(),
        tag.getUpdatedAt());
  }

  private TagOptionDto toOption(EvalTagOption option) {
    return new TagOptionDto(
        option.getId(),
        option.getTagId(),
        option.getOptionName(),
        option.getOptionGroup(),
        option.getDisplayOrder(),
        option.getCreatedAt(),
        option.getUpdatedAt());
  }

  private boolean hasLikeText(String like) {
    return StringUtils.hasText(like) && !"%%".equals(like);
  }

  private String likeText(String like) {
    return hasLikeText(like) && like.length() > 1 ? like.substring(1, like.length() - 1) : "";
  }
}
