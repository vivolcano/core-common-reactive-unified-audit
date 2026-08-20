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
   * Возвращает имя параметра или условия из YAML.
   *
   * @return имя из YAML.
   */
  String getName();

  /**
   * Возвращает привязанный экстрактор.
   *
   * @return экстрактор после биндинга; {@code null} до {@link AuditParameterBinder}.
   */
  Extractor getExtractor();

  /**
   * Сохраняет стратегию извлечения значения.
   *
   * @param extractor стратегия извлечения.
   */
  void setExtractor(Extractor extractor);

  /**
   * Возвращает ключ извлечения (заголовок, query, JSON-поле).
   *
   * @return ключ извлечения; {@code null}, пока биндер не задал значение.
   */
  String getKey();

  /**
   * Задаёт ключ извлечения.
   *
   * @param key имя источника; может совпадать с {@link #getName()}.
   */
  void setKey(String key);
}
