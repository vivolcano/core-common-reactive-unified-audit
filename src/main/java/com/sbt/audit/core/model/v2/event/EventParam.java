package com.sbt.audit.core.model.v2.event;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Заглушка параметра события аудита.
 */
@Getter
@ToString
@Builder
public class EventParam {

  private final String name;
  private final String value;
}
