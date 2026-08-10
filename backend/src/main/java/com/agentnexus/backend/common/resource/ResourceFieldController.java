package com.agentnexus.backend.common.resource;

import com.agentnexus.backend.common.ApiResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

@Component
@ResponseBody
@Path("/resources")
public class ResourceFieldController {
  private final ResourceFieldPatchService resourceFieldPatchService;

  public ResourceFieldController(ResourceFieldPatchService resourceFieldPatchService) {
    this.resourceFieldPatchService = resourceFieldPatchService;
  }

  @POST
  @Path("/{resourceType}/{resourceId}/fields")
  public ApiResponse<ResourceFieldPatchResult> updateFields(
      @PathParam("resourceType") String resourceType,
      @PathParam("resourceId") String resourceId,
      ResourceFieldPatchRequest request
  ) {
    return ApiResponse.ok(resourceFieldPatchService.updateFields(resourceType, resourceId, request));
  }
}
