package com.sbt.audit.core.model.v2.metamodel;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Заглушка метамодели событий аудита.
 */
@Getter
@ToString
@Builder
public class Metamodel {

  private final String metamodelVersion;
  private final String module;
  private final EventMetaInfos eventMetaInfos;
}
