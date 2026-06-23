package com.evalsystem.tag.api;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.common.PageResponse;
import com.evalsystem.tag.api.dto.response.TagDetail;
import com.evalsystem.tag.api.dto.request.TagInput;
import com.evalsystem.tag.api.dto.response.TagSummary;
import com.evalsystem.tag.service.TagService;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

@Component
@ResponseBody
@Path("/api/tags")
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

  @PUT
  @Path("/{tagId}")
  public ApiResponse<TagDetail> updateTag(@PathParam("tagId") String tagId, TagInput request) {
    return ApiResponse.ok(tagService.updateTag(tagId, request));
  }
}
