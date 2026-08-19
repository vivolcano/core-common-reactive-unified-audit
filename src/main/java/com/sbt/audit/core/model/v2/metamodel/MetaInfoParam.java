package com.sbt.audit.core.model.v2.metamodel;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Заглушка метаданных параметра события.
 */
@Getter
@ToString
@Builder
public class MetaInfoParam {

  private final String name;
  private final String description;
}
