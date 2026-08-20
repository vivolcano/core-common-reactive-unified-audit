package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * Имена полей YAML-конфигурации аудита, используемые в сообщениях валидации.
 */
@UtilityClass
public final class AuditConfigurationFieldNames {

  /** YAML-ключ имени события или параметра. */
  public static final String NAME = "name";

  /** YAML-ключ описания события или параметра. */
  public static final String DESCRIPTION = "description";

  /** YAML-ключ критичности события. */
  public static final String MODE = "mode";

  /** YAML-ключ признака успешности события. */
  public static final String SUCCESS = "success";

  /** YAML-ключ мапы источник → параметры события. */
  public static final String PARAMS = "params";

  /** YAML-ключ мапы источник → условия события. */
  public static final String CONDITIONS = "conditions";

  /** YAML-ключ имени поля условия; биндится в {@code name}. */
  public static final String FIELD = "field";

  /** YAML-ключ мапы имя метода контроллера → список событий. */
  public static final String EVENTS = "events";

  /** Префикс свойств клиента аудита. */
  public static final String AUDIT_CLIENT_PREFIX = "audit.client";

  /** Префикс свойств модели событий. */
  public static final String AUDIT_MODEL_PREFIX = "audit.model";

  /** Префикс реактивных свойств фильтра. */
  public static final String AUDIT_REACTIVE_PREFIX = "audit.reactive";

  /** Корневой пакет модуля для {@code @ComponentScan}. */
  public static final String BASE_PACKAGE = "ru.sbrf.sbererp.core.common.reactive.unified.audit";

  /** FQCN auto-config WebFlux, после которой поднимается этот модуль. */
  public static final String WEBFLUX_AUTO_CONFIGURATION =
      "org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration";

  /** YAML-ключ версии метамодели. */
  public static final String VERSION = "version";

  /** YAML-ключ модуля метамодели. */
  public static final String MODULE = "module";

  /** YAML-ключ подсистемы метамодели. */
  public static final String SUBSYSTEM = "subsystem";

  /** YAML-ключ системы-источника метамодели. */
  public static final String SOURCE_SYSTEM = "sourceSystem";

  /** YAML-ключ FQCN контроллера. */
  public static final String CONTROLLER_CLASS = "controllerClass";
}
