package com.evalsystem.integration.api.dto.response;

import java.util.List;

public record PlatformListResult<T>(
    PlatformPageVO pageVO,
    List<T> result
) {
}

record PlatformPageVO(
    Integer totalRows,
    Integer curPage,
    Integer pageSize,
    Integer resultMode,
    Integer startIndex,
    Integer endIndex,
    Integer totalPages
) {
}
