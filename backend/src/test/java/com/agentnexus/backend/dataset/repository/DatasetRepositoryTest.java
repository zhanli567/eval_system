package com.agentnexus.backend.dataset.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.agentnexus.backend.dataset.entity.EvalDatasetItem;
import com.agentnexus.backend.dataset.entity.EvalDatasetItemCell;
import com.agentnexus.backend.dataset.mapper.DatasetFieldMapper;
import com.agentnexus.backend.dataset.mapper.DatasetItemCellMapper;
import com.agentnexus.backend.dataset.mapper.DatasetItemMapper;
import com.agentnexus.backend.dataset.mapper.DatasetMapper;
import com.agentnexus.backend.dataset.mapper.DatasetVersionMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DatasetRepositoryTest {
  private static final int MAX_BATCH_PARAMETERS = 60000;
  private static final int ITEM_PARAMETER_COUNT = 9;
  private static final int CELL_PARAMETER_COUNT = 11;

  @Test
  void insertItemsSplitsLargeListByPreparedStatementParameterBudget() {
    DatasetItemMapper itemMapper = mock(DatasetItemMapper.class);
    DatasetRepository repository = repositoryWith(itemMapper, mock(DatasetItemCellMapper.class));

    repository.insertItems("versionId", itemInserts(MAX_BATCH_PARAMETERS / ITEM_PARAMETER_COUNT + 1), "1723814400000");

    ArgumentCaptor<List<EvalDatasetItem>> captor = ArgumentCaptor.captor();
    verify(itemMapper, org.mockito.Mockito.times(2)).insertBatch(captor.capture());
    assertBatchParameters(captor.getAllValues(), ITEM_PARAMETER_COUNT);
  }

  @Test
  void insertCellsSplitsLargeListByPreparedStatementParameterBudget() {
    DatasetItemCellMapper cellMapper = mock(DatasetItemCellMapper.class);
    DatasetRepository repository = repositoryWith(mock(DatasetItemMapper.class), cellMapper);

    repository.insertCells("versionId", cellInserts(MAX_BATCH_PARAMETERS / CELL_PARAMETER_COUNT + 1), "1723814400000");

    ArgumentCaptor<List<EvalDatasetItemCell>> captor = ArgumentCaptor.captor();
    verify(cellMapper, org.mockito.Mockito.times(2)).insertBatch(captor.capture());
    assertBatchParameters(captor.getAllValues(), CELL_PARAMETER_COUNT);
  }

  private DatasetRepository repositoryWith(DatasetItemMapper itemMapper, DatasetItemCellMapper cellMapper) {
    return new DatasetRepository(
        mock(DatasetMapper.class),
        mock(DatasetVersionMapper.class),
        mock(DatasetFieldMapper.class),
        itemMapper,
        cellMapper);
  }

  private List<DatasetRepository.DatasetItemInsert> itemInserts(int count) {
    List<DatasetRepository.DatasetItemInsert> items = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      items.add(new DatasetRepository.DatasetItemInsert("item" + index, index + 1));
    }
    return items;
  }

  private List<DatasetRepository.DatasetCellInsert> cellInserts(int count) {
    List<DatasetRepository.DatasetCellInsert> cells = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      cells.add(new DatasetRepository.DatasetCellInsert("cell" + index, "item" + index, "field", "value"));
    }
    return cells;
  }

  private <T> void assertBatchParameters(List<List<T>> batches, int parametersPerRecord) {
    assertThat(batches).hasSize(2);
    assertThat(batches)
        .allSatisfy(batch -> assertThat(batch.size() * parametersPerRecord).isLessThanOrEqualTo(MAX_BATCH_PARAMETERS));
    assertThat(batches.stream().mapToInt(List::size).sum()).isEqualTo(MAX_BATCH_PARAMETERS / parametersPerRecord + 1);
  }
}
