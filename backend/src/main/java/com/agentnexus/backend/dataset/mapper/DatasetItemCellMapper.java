package com.agentnexus.backend.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.agentnexus.backend.dataset.entity.EvalDatasetItemCell;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DatasetItemCellMapper extends BaseMapper<EvalDatasetItemCell> {
  /**
   * 批量新增评测集数据单元格。
   *
   * @param cells 评测集数据单元格列表
   * @return 新增单元格数量
   */
  int insertBatch(@Param("cells") List<EvalDatasetItemCell> cells);
}
