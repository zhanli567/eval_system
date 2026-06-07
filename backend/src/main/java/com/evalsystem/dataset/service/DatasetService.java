package com.evalsystem.dataset.service;

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
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DatasetService {
  PageResponse<DatasetSummary> listDatasets(int page, int size, String keyword);

  DatasetSummary createDataset(CreateDatasetRequest request);

  DatasetSummary getDatasetSummary(String datasetId);

  void deleteDataset(String datasetId);

  List<DatasetVersionDto> listVersions(String datasetId);

  VersionDetail getVersionDetail(String versionId, int page, int size, String fieldId, String keyword);

  List<FieldDto> listFields(String versionId);

  PageResponse<RowDto> listRows(String versionId, int page, int size, String fieldId, String keyword);

  List<FieldDto> replaceFields(String versionId, List<FieldInput> fields);

  RowDto addRow(String versionId, RowInput request);

  List<RowDto> addRows(String versionId, BatchRowsRequest request);

  ImportRowsResult importRows(String versionId, MultipartFile file);

  ImportRowsResult coverRowsByExcel(String versionId, MultipartFile file);

  RowDto updateRow(String versionId, String itemId, RowInput request);

  void deleteRow(String versionId, String itemId);

  DatasetVersionDto publish(String datasetId);

  void deleteVersion(String versionId);

  DatasetVersionDto coverDraft(String datasetId, String sourceVersionId);
}
