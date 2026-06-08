package com.evalsystem.tag.service;

import com.evalsystem.common.PageResponse;
import com.evalsystem.tag.dto.TagDetail;
import com.evalsystem.tag.dto.TagInput;
import com.evalsystem.tag.dto.TagSummary;

public interface TagService {
  PageResponse<TagSummary> listTags(int page, int size, String tagType, String keyword);

  TagDetail getTag(String tagId);

  TagDetail createTag(TagInput request);

  TagDetail updateTag(String tagId, TagInput request);
}
