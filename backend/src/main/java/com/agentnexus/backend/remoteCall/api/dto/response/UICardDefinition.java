package com.agentnexus.backend.remoteCall.api.dto.response;

import java.util.List;
import lombok.Data;

/**
 * Agent generated UI card definition.
 * Created on 2026-08-04.
 */
@Data
public class UICardDefinition {
  private String id;
  private String type;
  private String version;
  private String displayName;
  private UICardLocation location;
  private List<UICardComponentDefinition> body;
}
