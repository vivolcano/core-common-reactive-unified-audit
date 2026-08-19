package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import static ru.sbrf.sbererp.core.common.unified.audit.utils.AuditPropertiesValidationUtils.validate;

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
   * @throws ru.sbrf.sbererp.core.common.unified.audit.exception.UnifiedAuditException если поле пустое
   */
  public MetaModelTitleHolder {
    validate(version, module, subsystem, sourceSystem);
  }
}
