package com.agentnexus.backend.remoteCall.api.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import lombok.Data;

/**
 * Agent generated UI card component definition.
 * Created on 2026-08-04.
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LinkJSUICardComponentDefinition.class, name = "LinkJS")
})
public abstract class UICardComponentDefiniton {
  private String id;
  private String type;
  private String componentKey;
  private Map<String, Object> propsData;
}
