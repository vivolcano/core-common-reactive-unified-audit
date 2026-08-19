package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import static ru.sbrf.sbererp.core.common.unified.audit.properties.util.ValidateUtil.validate;

/**
 * Record, содержащй данные заголовка метамодели.
 */
public record MetaModelTitleHolder(String version, String module, String subsystem, String sourceSystem) {
  /**
   * Конструктор записи, выполняет валидацию переданных параметров.
   * В случае некорректных данных класс выбрасывается исключение.
   *
   * @param version      версия модели
   * @param module       имя модуля
   * @param subsystem    имя подсистемы
   * @param sourceSystem источник системы
   */
  public MetaModelTitleHolder {
    validate(version, module, subsystem, sourceSystem);
  }
}
