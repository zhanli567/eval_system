package com.agentnexus.backend.evaluator.preset;

public record PresetParamDefinition(
    String paramName,
    String dataType,
    String defaultValue,
    Boolean required,
    String description
) {
  public static PresetParamDefinition param(
      String paramName,
      String dataType,
      String defaultValue,
      Boolean required,
      String description
  ) {
    return new PresetParamDefinition(paramName, dataType, defaultValue, required, description);
  }
}
