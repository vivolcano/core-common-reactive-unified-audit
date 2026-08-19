package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import ru.sbrf.sbererp.core.common.unified.audit.binder.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.unified.audit.extractor.Extractor;
import org.springframework.web.server.ServerWebExchange;

/**
 * YAML-параметр или условие с отложенной привязкой {@link Extractor}.
 * <p>
 * {@link #setExtractor(Extractor)} / {@link #setKey(String)} вызываются из
 * {@link AuditParameterBinder} после резолва метода контроллера.
 */
public interface Holder {

  /**
   * @return имя из YAML ({@code name} / {@code field}), не {@code null} после валидации
   */
  String getName();

  /**
   * @return экстрактор после биндинга; {@code null} до {@link AuditParameterBinder}
   */
  Extractor getExtractor();

  /**
   * @param extractor стратегия извлечения из {@link ServerWebExchange}.
   */
  void setExtractor(Extractor extractor);

  /**
   * @return ключ извлечения (заголовок, query, JSON-поле); {@code null} пока биндер не задал
   */
  String getKey();

  /**
   * @param key имя источника; может совпадать с {@link #getName()}.
   */
  void setKey(String key);
}
