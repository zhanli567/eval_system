package com.agentnexus.backend.dataset.api.dto.request;

import java.util.List;

/**
 * 评测集数据删除请求。
 *
 * @param itemIds 待删除的数据ID列表
 * @since 2026-08-10
 */
public record DeleteRowsRequest(List<String> itemIds) {
}
