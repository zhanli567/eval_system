package com.evalsystem.dataset.controller;

import com.evalsystem.common.ApiResponse;
import com.evalsystem.common.PageResponse;
import com.evalsystem.dataset.dto.BatchRowsRequest;
import com.evalsystem.dataset.dto.CreateDatasetRequest;
import com.evalsystem.dataset.dto.DatasetSummary;
import com.evalsystem.dataset.dto.DatasetVersionDto;
import com.evalsystem.dataset.dto.FieldDto;
import com.evalsystem.dataset.dto.FieldInput;
import com.evalsystem.dataset.dto.ImportRowsResult;
import com.evalsystem.dataset.dto.RowDto;
import com.evalsystem.dataset.dto.RowInput;
import com.evalsystem.dataset.dto.VersionDetail;
import com.evalsystem.dataset.service.DatasetService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {
  private final DatasetService datasetService;

  public DatasetController(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  @GetMapping
  public ApiResponse<PageResponse<DatasetSummary>> listDatasets(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword
  ) {
    return ApiResponse.ok(datasetService.listDatasets(page, size, keyword));
  }

  @PostMapping
  public ApiResponse<DatasetSummary> createDataset(@RequestBody CreateDatasetRequest request) {
    return ApiResponse.ok(datasetService.createDataset(request));
  }

  @DeleteMapping("/{datasetId}")
  public ApiResponse<Void> deleteDataset(@PathVariable String datasetId) {
    datasetService.deleteDataset(datasetId);
    return ApiResponse.ok(null);
  }

  @GetMapping("/{datasetId}/versions")
  public ApiResponse<List<DatasetVersionDto>> listVersions(@PathVariable String datasetId) {
    return ApiResponse.ok(datasetService.listVersions(datasetId));
  }

  @PostMapping("/{datasetId}/publish")
  public ApiResponse<DatasetVersionDto> publish(@PathVariable String datasetId) {
    return ApiResponse.ok(datasetService.publish(datasetId));
  }

  @PostMapping("/{datasetId}/versions/{versionId}/cover-draft")
  public ApiResponse<DatasetVersionDto> coverDraft(@PathVariable String datasetId, @PathVariable String versionId) {
    return ApiResponse.ok(datasetService.coverDraft(datasetId, versionId));
  }

  @DeleteMapping("/versions/{versionId}")
  public ApiResponse<Void> deleteVersion(@PathVariable String versionId) {
    datasetService.deleteVersion(versionId);
    return ApiResponse.ok(null);
  }

  @GetMapping("/versions/{versionId}")
  public ApiResponse<VersionDetail> getVersionDetail(
      @PathVariable String versionId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String fieldId,
      @RequestParam(required = false) String keyword
  ) {
    return ApiResponse.ok(datasetService.getVersionDetail(versionId, page, size, fieldId, keyword));
  }

  @PutMapping("/versions/{versionId}/fields")
  public ApiResponse<List<FieldDto>> replaceFields(@PathVariable String versionId, @RequestBody List<FieldInput> fields) {
    return ApiResponse.ok(datasetService.replaceFields(versionId, fields));
  }

  @PostMapping("/versions/{versionId}/items")
  public ApiResponse<RowDto> addRow(@PathVariable String versionId, @RequestBody RowInput request) {
    return ApiResponse.ok(datasetService.addRow(versionId, request));
  }

  @PostMapping("/versions/{versionId}/items/batch")
  public ApiResponse<List<RowDto>> addRows(@PathVariable String versionId, @RequestBody BatchRowsRequest request) {
    return ApiResponse.ok(datasetService.addRows(versionId, request));
  }

  @PostMapping("/versions/{versionId}/items/import")
  public ApiResponse<ImportRowsResult> importRows(@PathVariable String versionId, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(datasetService.importRows(versionId, file));
  }

  @PostMapping("/versions/{versionId}/items/import-cover")
  public ApiResponse<ImportRowsResult> coverRowsByExcel(@PathVariable String versionId, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(datasetService.coverRowsByExcel(versionId, file));
  }

  @PutMapping("/versions/{versionId}/items/{itemId}")
  public ApiResponse<RowDto> updateRow(@PathVariable String versionId, @PathVariable String itemId, @RequestBody RowInput request) {
    return ApiResponse.ok(datasetService.updateRow(versionId, itemId, request));
  }

  @DeleteMapping("/versions/{versionId}/items/{itemId}")
  public ApiResponse<Void> deleteRow(@PathVariable String versionId, @PathVariable String itemId) {
    datasetService.deleteRow(versionId, itemId);
    return ApiResponse.ok(null);
  }
}
