package com.evalsystem.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evalsystem.dataset.dto.DatasetSummary;
import com.evalsystem.dataset.pojo.EvalDataset;
import com.evalsystem.dataset.repository.DatasetRowRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DatasetMapper extends BaseMapper<EvalDataset> {
  List<DatasetSummary> listDatasetSummaries(@Param("like") String like, @Param("size") int size, @Param("offset") int offset);

  DatasetSummary findDatasetSummary(@Param("datasetId") String datasetId);

  List<DatasetRowRecord> searchRows(
      @Param("versionId") String versionId,
      @Param("fieldId") String fieldId,
      @Param("like") String like,
      @Param("size") int size,
      @Param("offset") int offset
  );

  long countSearchRows(@Param("versionId") String versionId, @Param("fieldId") String fieldId, @Param("like") String like);
}
