package com.sbt.audit.core.model.v2.metamodel;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import ru.sbrf.ufs.platform.audit.model.CriticalityEnum;

/**
 * Заглушка метаданных одного события аудита.
 */
@Getter
@ToString
@Builder
public class EventMetaInfo {

  private final String name;
  private final String description;
  private final CriticalityEnum mode;
  private final Boolean success;
  private final String subsystem;
  private final MetaInfoParams metaInfoParams;
}
