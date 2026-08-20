package ru.sbrf.sbererp.core.common.reactive.unified.audit.holder;

import ru.sbrf.sbererp.core.common.reactive.unified.audit.binder.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.Extractor;

/**
 * YAML-параметр или условие с отложенной привязкой {@link Extractor}.
 *
 * @see AuditParameterBinder
 */
public interface Holder {

  /**
   * @return имя из YAML
   */
  String getName();

  /**
   * @return экстрактор после биндинга; {@code null} до {@link AuditParameterBinder}
   */
  Extractor getExtractor();

  /**
   * @param extractor стратегия извлечения
   */
  void setExtractor(Extractor extractor);

  /**
   * @return ключ извлечения; {@code null}, пока биндер не задал значение
   */
  String getKey();

  /**
   * @param key имя источника; может совпадать с {@link #getName()}
   */
  void setKey(String key);
}
