package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent response reference item.
 * Created on 2026-08-04.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceItem {
  private String id;
  private String title;
  private String url;
  private String sourceType;
  private String sourceName;
  private String summary;
  private String snippet;
  private Map<String, Object> extra;
}
