package com.agentnexus.backend.remoteCall.api.dto.response;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * LinkJS generated UI card component definition.
 * Created on 2026-08-04.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("LinkJS")
public class LinkJSUICardComponentDefinition extends UICardComponentDefinition {
  private String componentName;
  private String componentPortalUrl;
}
