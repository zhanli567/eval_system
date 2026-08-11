package com.agentnexus.backend.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.agentnexus.backend.dataset.entity.EvalDatasetItem;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DatasetItemMapper extends BaseMapper<EvalDatasetItem> {
  /**
   * 批量新增评测集数据行。
   *
   * @param items 评测集数据行列表
   * @return 新增数据行数量
   */
  int insertBatch(@Param("items") List<EvalDatasetItem> items);
}
