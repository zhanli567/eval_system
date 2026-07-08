package com.agentnexus.backend.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.agentnexus.backend.dataset.api.dto.response.DatasetSummary;
import com.agentnexus.backend.dataset.entity.EvalDataset;
import com.agentnexus.backend.dataset.repository.DatasetRowRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DatasetMapper extends BaseMapper<EvalDataset> {
  List<DatasetSummary> listDatasetSummaries(
      @Param("spaceId") String spaceId,
      @Param("like") String like,
      @Param("orderColumn") String orderColumn,
      @Param("orderDirection") String orderDirection,
      @Param("size") int size,
      @Param("offset") int offset
  );

  DatasetSummary findDatasetSummary(@Param("spaceId") String spaceId, @Param("datasetId") String datasetId);

  List<DatasetRowRecord> searchRows(
      @Param("spaceId") String spaceId,
      @Param("versionId") String versionId,
      @Param("fieldId") String fieldId,
      @Param("like") String like,
      @Param("size") int size,
      @Param("offset") int offset
  );

  long countSearchRows(
      @Param("spaceId") String spaceId,
      @Param("versionId") String versionId,
      @Param("fieldId") String fieldId,
      @Param("like") String like
  );
}
