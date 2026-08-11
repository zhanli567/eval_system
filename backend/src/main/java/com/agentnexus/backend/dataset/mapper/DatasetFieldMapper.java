package com.agentnexus.backend.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.agentnexus.backend.dataset.entity.EvalDatasetField;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DatasetFieldMapper extends BaseMapper<EvalDatasetField> {
  /**
   * 批量新增评测集字段。
   *
   * @param fields 评测集字段列表
   * @return 新增字段数量
   */
  int insertBatch(@Param("fields") List<EvalDatasetField> fields);
}
