package com.evalsystem.dataset.controller;

import com.evalsystem.common.PageResponse;
import com.evalsystem.dataset.dto.BatchRowsRequest;
import com.evalsystem.dataset.dto.CreateDatasetRequest;
import com.evalsystem.dataset.dto.DatasetSummary;
import com.evalsystem.dataset.dto.DatasetVersionDto;
import com.evalsystem.dataset.dto.FieldDto;
import com.evalsystem.dataset.dto.FieldInput;
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

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {
  private final DatasetService datasetService;

  public DatasetController(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  @GetMapping
  public PageResponse<DatasetSummary> listDatasets(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword
  ) {
    return datasetService.listDatasets(page, size, keyword);
  }

  @PostMapping
  public DatasetSummary createDataset(@RequestBody CreateDatasetRequest request) {
    return datasetService.createDataset(request);
  }

  @DeleteMapping("/{datasetId}")
  public void deleteDataset(@PathVariable String datasetId) {
    datasetService.deleteDataset(datasetId);
  }

  @GetMapping("/{datasetId}/versions")
  public List<DatasetVersionDto> listVersions(@PathVariable String datasetId) {
    return datasetService.listVersions(datasetId);
  }

  @PostMapping("/{datasetId}/publish")
  public DatasetVersionDto publish(@PathVariable String datasetId) {
    return datasetService.publish(datasetId);
  }

  @PostMapping("/{datasetId}/versions/{versionId}/cover-draft")
  public DatasetVersionDto coverDraft(@PathVariable String datasetId, @PathVariable String versionId) {
    return datasetService.coverDraft(datasetId, versionId);
  }

  @DeleteMapping("/versions/{versionId}")
  public void deleteVersion(@PathVariable String versionId) {
    datasetService.deleteVersion(versionId);
  }

  @GetMapping("/versions/{versionId}")
  public VersionDetail getVersionDetail(
      @PathVariable String versionId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String fieldId,
      @RequestParam(required = false) String keyword
  ) {
    return datasetService.getVersionDetail(versionId, page, size, fieldId, keyword);
  }

  @PutMapping("/versions/{versionId}/fields")
  public List<FieldDto> replaceFields(@PathVariable String versionId, @RequestBody List<FieldInput> fields) {
    return datasetService.replaceFields(versionId, fields);
  }

  @PostMapping("/versions/{versionId}/items")
  public RowDto addRow(@PathVariable String versionId, @RequestBody RowInput request) {
    return datasetService.addRow(versionId, request);
  }

  @PostMapping("/versions/{versionId}/items/batch")
  public List<RowDto> addRows(@PathVariable String versionId, @RequestBody BatchRowsRequest request) {
    return datasetService.addRows(versionId, request);
  }

  @PutMapping("/versions/{versionId}/items/{itemId}")
  public RowDto updateRow(@PathVariable String versionId, @PathVariable String itemId, @RequestBody RowInput request) {
    return datasetService.updateRow(versionId, itemId, request);
  }

  @DeleteMapping("/versions/{versionId}/items/{itemId}")
  public void deleteRow(@PathVariable String versionId, @PathVariable String itemId) {
    datasetService.deleteRow(versionId, itemId);
  }
}
