package ru.sbrf.sbererp.core.common.reactive.unified.audit.holder;

import static ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditPropertiesValidationUtils.validate;

/**
 * Заголовок метамодели из {@code audit.client.meta-model}. Все поля обязательны.
 *
 * @param version      версия метамодели.
 * @param module       модуль.
 * @param subsystem    подсистема.
 * @param sourceSystem система-источник.
 */
public record MetaModelTitleHolder(String version, String module, String subsystem, String sourceSystem) {

  /**
   * Проверяет обязательные поля заголовка метамодели.
   */
  public MetaModelTitleHolder {
    validate(version, module, subsystem, sourceSystem);
  }
}
