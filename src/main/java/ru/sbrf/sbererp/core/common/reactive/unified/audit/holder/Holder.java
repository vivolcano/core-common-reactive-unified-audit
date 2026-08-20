package ru.sbrf.sbererp.core.common.reactive.unified.audit.holder;

import ru.sbrf.sbererp.core.common.reactive.unified.audit.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.Extractor;
import org.springframework.web.server.ServerWebExchange;

/**
 * YAML-параметр или условие с отложенной привязкой {@link Extractor}.
 * <p>
 * {@link #setExtractor(Extractor)} / {@link #setKey(String)} вызываются из
 * {@link AuditParameterBinder} после резолва метода контроллера.
 */
public interface Holder {

  /**
   * Возвращает имя параметра или условия из YAML.
   *
   * @return имя из YAML ({@code name} / {@code field}), не {@code null} после валидации.
   */
  String getName();

  /**
   * Возвращает привязанный экстрактор.
   *
   * @return экстрактор после биндинга; {@code null} до {@link AuditParameterBinder}.
   */
  Extractor getExtractor();

  /**
   * Сохраняет стратегию извлечения значения из {@link ServerWebExchange}.
   *
   * @param extractor стратегия извлечения из {@link ServerWebExchange}.
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
