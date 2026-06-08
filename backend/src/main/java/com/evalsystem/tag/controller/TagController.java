package com.evalsystem.tag.controller;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.common.PageResponse;
import com.evalsystem.tag.dto.TagDetail;
import com.evalsystem.tag.dto.TagInput;
import com.evalsystem.tag.dto.TagSummary;
import com.evalsystem.tag.service.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {
  private final TagService tagService;

  public TagController(TagService tagService) {
    this.tagService = tagService;
  }

  @GetMapping
  public ApiResponse<PageResponse<TagSummary>> listTags(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String tagType,
      @RequestParam(required = false) String keyword
  ) {
    return ApiResponse.ok(tagService.listTags(page, size, tagType, keyword));
  }

  @GetMapping("/{tagId}")
  public ApiResponse<TagDetail> getTag(@PathVariable String tagId) {
    return ApiResponse.ok(tagService.getTag(tagId));
  }

  @PostMapping
  public ApiResponse<TagDetail> createTag(@RequestBody TagInput request) {
    return ApiResponse.ok(tagService.createTag(request));
  }

  @PutMapping("/{tagId}")
  public ApiResponse<TagDetail> updateTag(@PathVariable String tagId, @RequestBody TagInput request) {
    return ApiResponse.ok(tagService.updateTag(tagId, request));
  }
}
