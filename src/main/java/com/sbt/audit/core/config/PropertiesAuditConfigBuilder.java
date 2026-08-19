package com.sbt.audit.core.config;

import java.util.Map;
import java.util.Objects;

/**
 * Заглушка билдера конфигурации клиента Единого аудита из map-свойств.
 */
public class PropertiesAuditConfigBuilder {

  /**
   * Создаёт билдер на основе карты свойств клиента аудита.
   *
   * @param properties свойства клиента; {@code null} недопустим
   */
  public PropertiesAuditConfigBuilder(Map<String, String> properties) {
    Objects.requireNonNull(properties, "properties");
  }

  /**
   * Собирает конфигурацию клиента аудита.
   *
   * @return конфигурация-заглушка
   */
  public AuditConfig build() {
    return new AuditConfig();
  }
}
