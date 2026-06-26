package com.agentnexus.backend.dataset.api.dto.response;

import com.agentnexus.backend.common.PageResponse;
import java.util.List;

public record VersionDetail(
    DatasetVersionDto version,
    List<FieldDto> fields,
    PageResponse<RowDto> rows
) {
}
