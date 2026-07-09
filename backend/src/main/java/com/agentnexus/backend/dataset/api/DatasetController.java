package com.agentnexus.backend.dataset.api;

import com.agentnexus.backend.common.ApiResponse;
import com.agentnexus.backend.common.PageResponse;
import com.agentnexus.backend.dataset.api.dto.request.CreateDatasetRequest;
import com.agentnexus.backend.dataset.api.dto.response.DatasetSummary;
import com.agentnexus.backend.dataset.api.dto.response.DatasetVersionDto;
import com.agentnexus.backend.dataset.api.dto.response.FieldDto;
import com.agentnexus.backend.dataset.api.dto.request.FieldInput;
import com.agentnexus.backend.dataset.api.dto.response.ImportRowsResult;
import com.agentnexus.backend.dataset.api.dto.response.RowDto;
import com.agentnexus.backend.dataset.api.dto.request.RowInput;
import com.agentnexus.backend.dataset.api.dto.response.VersionDetail;
import com.agentnexus.backend.dataset.service.DatasetService;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Component
@ResponseBody
@Path("/datasets")
public class DatasetController {
  private final DatasetService datasetService;

  public DatasetController(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  @GET
  @Path("")
  public ApiResponse<PageResponse<DatasetSummary>> listDatasets(
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("keyword") String keyword,
      @QueryParam("sortBy") @DefaultValue("lastUpdatedDate") String sortBy,
      @QueryParam("sortOrder") @DefaultValue("desc") String sortOrder
  ) {
    return ApiResponse.ok(datasetService.listDatasets(page, size, keyword, sortBy, sortOrder));
  }

  @POST
  @Path("")
  public ApiResponse<DatasetSummary> createDataset(CreateDatasetRequest request) {
    return ApiResponse.ok(datasetService.createDataset(request));
  }

  @POST
  @Path("/{datasetId}/delete")
  public ApiResponse<Void> deleteDataset(@PathParam("datasetId") String datasetId) {
    datasetService.deleteDataset(datasetId);
    return ApiResponse.ok(null);
  }

  @GET
  @Path("/{datasetId}/versions")
  public ApiResponse<List<DatasetVersionDto>> listVersions(@PathParam("datasetId") String datasetId) {
    return ApiResponse.ok(datasetService.listVersions(datasetId));
  }

  @POST
  @Path("/{datasetId}/publish")
  public ApiResponse<DatasetVersionDto> publish(@PathParam("datasetId") String datasetId) {
    return ApiResponse.ok(datasetService.publish(datasetId));
  }

  @POST
  @Path("/{datasetId}/versions/{versionId}/cover-draft")
  public ApiResponse<DatasetVersionDto> coverDraft(@PathParam("datasetId") String datasetId, @PathParam("versionId") String versionId) {
    return ApiResponse.ok(datasetService.coverDraft(datasetId, versionId));
  }

  @POST
  @Path("/versions/{versionId}/delete")
  public ApiResponse<Void> deleteVersion(@PathParam("versionId") String versionId) {
    datasetService.deleteVersion(versionId);
    return ApiResponse.ok(null);
  }

  @GET
  @Path("/versions/{versionId}")
  public ApiResponse<VersionDetail> getVersionDetail(
      @PathParam("versionId") String versionId,
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("fieldId") String fieldId,
      @QueryParam("keyword") String keyword
  ) {
    return ApiResponse.ok(datasetService.getVersionDetail(versionId, page, size, fieldId, keyword));
  }

  @POST
  @Path("/versions/{versionId}/fields")
  public ApiResponse<List<FieldDto>> replaceFields(@PathParam("versionId") String versionId, List<FieldInput> fields) {
    return ApiResponse.ok(datasetService.replaceFields(versionId, fields));
  }

  @POST
  @Path("/versions/{versionId}/items")
  public ApiResponse<RowDto> addRow(@PathParam("versionId") String versionId, RowInput request) {
    return ApiResponse.ok(datasetService.addRow(versionId, request));
  }

  @POST
  @Path("/versions/{versionId}/items/import")
  public ApiResponse<ImportRowsResult> importRows(@PathParam("versionId") String versionId, @FormParam("file") MultipartFile file) {
    return ApiResponse.ok(datasetService.importRows(versionId, file));
  }

  @POST
  @Path("/versions/{versionId}/items/import-cover")
  public ApiResponse<ImportRowsResult> coverRowsByExcel(@PathParam("versionId") String versionId, @FormParam("file") MultipartFile file) {
    return ApiResponse.ok(datasetService.coverRowsByExcel(versionId, file));
  }

  @POST
  @Path("/versions/{versionId}/items/{itemId}")
  public ApiResponse<RowDto> updateRow(@PathParam("versionId") String versionId, @PathParam("itemId") String itemId, RowInput request) {
    return ApiResponse.ok(datasetService.updateRow(versionId, itemId, request));
  }

  @POST
  @Path("/versions/{versionId}/items/{itemId}/delete")
  public ApiResponse<Void> deleteRow(@PathParam("versionId") String versionId, @PathParam("itemId") String itemId) {
    datasetService.deleteRow(versionId, itemId);
    return ApiResponse.ok(null);
  }
}
