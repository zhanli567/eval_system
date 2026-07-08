package com.agentnexus.backend.tag.api;

import com.agentnexus.backend.common.ApiResponse;
import com.agentnexus.backend.common.PageResponse;
import com.agentnexus.backend.tag.api.dto.response.TagDetail;
import com.agentnexus.backend.tag.api.dto.request.TagInput;
import com.agentnexus.backend.tag.api.dto.response.TagSummary;
import com.agentnexus.backend.tag.service.TagService;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

@Component
@ResponseBody
@Path("/tags")
public class TagController {
  private final TagService tagService;

  public TagController(TagService tagService) {
    this.tagService = tagService;
  }

  @GET
  @Path("")
  public ApiResponse<PageResponse<TagSummary>> listTags(
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("tagType") String tagType,
      @QueryParam("keyword") String keyword
  ) {
    return ApiResponse.ok(tagService.listTags(page, size, tagType, keyword));
  }

  @GET
  @Path("/{tagId}")
  public ApiResponse<TagDetail> getTag(@PathParam("tagId") String tagId) {
    return ApiResponse.ok(tagService.getTag(tagId));
  }

  @POST
  @Path("")
  public ApiResponse<TagDetail> createTag(TagInput request) {
    return ApiResponse.ok(tagService.createTag(request));
  }

  @POST
  @Path("/{tagId}")
  public ApiResponse<TagDetail> updateTag(@PathParam("tagId") String tagId, TagInput request) {
    return ApiResponse.ok(tagService.updateTag(tagId, request));
  }

  @POST
  @Path("/{tagId}/delete")
  public ApiResponse<Void> deleteTag(@PathParam("tagId") String tagId) {
    tagService.deleteTag(tagId);
    return ApiResponse.ok(null);
  }
}
