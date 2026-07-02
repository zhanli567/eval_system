package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.List;

public record ListResult<T>(
    PageVO pageVO,
    List<T> result
) {
}

record PageVO(
    Integer totalRows,
    Integer curPage,
    Integer pageSize,
    Integer resultMode,
    Integer startIndex,
    Integer endIndex,
    Integer totalPages
) {
}
