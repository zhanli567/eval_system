package com.evalsystem.dataset.api.dto.response;

import com.evalsystem.common.PageResponse;
import java.util.List;

public record VersionDetail(
    DatasetVersionDto version,
    List<FieldDto> fields,
    PageResponse<RowDto> rows
) {
}
