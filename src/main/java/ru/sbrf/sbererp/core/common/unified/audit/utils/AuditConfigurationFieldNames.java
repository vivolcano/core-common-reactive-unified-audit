package ru.sbrf.sbererp.core.common.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * Имена полей YAML-конфигурации аудита, используемые в сообщениях валидации.
 */
@UtilityClass
public final class AuditConfigurationFieldNames {

  public static final String NAME = "name";
  public static final String DESCRIPTION = "description";
  public static final String MODE = "mode";
  public static final String SUCCESS = "success";
  public static final String PARAMS = "params";
  public static final String EVENTS = "events";
  public static final String VERSION = "version";
  public static final String MODULE = "module";
  public static final String SUBSYSTEM = "subsystem";
  public static final String SOURCE_SYSTEM = "sourceSystem";
  public static final String CONTROLLER_CLASS = "controllerClass";
}
