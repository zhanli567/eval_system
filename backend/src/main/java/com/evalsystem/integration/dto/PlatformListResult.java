package com.evalsystem.integration.dto;

import java.util.List;

public record PlatformListResult<T>(
    List<T> list,
    Integer total,
    Integer pageNum,
    Integer pageSize,
    Integer pages
) {
}
