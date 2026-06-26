package com.evalsystem.dataset.api;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.common.PageResponse;
import com.evalsystem.dataset.api.dto.request.BatchRowsRequest;
import com.evalsystem.dataset.api.dto.request.CreateDatasetRequest;
import com.evalsystem.dataset.api.dto.response.DatasetSummary;
import com.evalsystem.dataset.api.dto.response.DatasetVersionDto;
import com.evalsystem.dataset.api.dto.response.FieldDto;
import com.evalsystem.dataset.api.dto.request.FieldInput;
import com.evalsystem.dataset.api.dto.response.ImportRowsResult;
import com.evalsystem.dataset.api.dto.response.RowDto;
import com.evalsystem.dataset.api.dto.request.RowInput;
import com.evalsystem.dataset.api.dto.response.VersionDetail;
import com.evalsystem.dataset.service.DatasetService;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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
      @QueryParam("keyword") String keyword
  ) {
    return ApiResponse.ok(datasetService.listDatasets(page, size, keyword));
  }

  @POST
  @Path("")
  public ApiResponse<DatasetSummary> createDataset(CreateDatasetRequest request) {
    return ApiResponse.ok(datasetService.createDataset(request));
  }

  @DELETE
  @Path("/{datasetId}")
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

  @DELETE
  @Path("/versions/{versionId}")
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

  @PUT
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
  @Path("/versions/{versionId}/items/batch")
  public ApiResponse<List<RowDto>> addRows(@PathParam("versionId") String versionId, BatchRowsRequest request) {
    return ApiResponse.ok(datasetService.addRows(versionId, request));
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

  @PUT
  @Path("/versions/{versionId}/items/{itemId}")
  public ApiResponse<RowDto> updateRow(@PathParam("versionId") String versionId, @PathParam("itemId") String itemId, RowInput request) {
    return ApiResponse.ok(datasetService.updateRow(versionId, itemId, request));
  }

  @DELETE
  @Path("/versions/{versionId}/items/{itemId}")
  public ApiResponse<Void> deleteRow(@PathParam("versionId") String versionId, @PathParam("itemId") String itemId) {
    datasetService.deleteRow(versionId, itemId);
    return ApiResponse.ok(null);
  }
}
