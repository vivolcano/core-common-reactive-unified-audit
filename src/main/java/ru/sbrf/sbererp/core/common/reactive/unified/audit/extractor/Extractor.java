package ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor;

import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.Holder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditExceptionMessages;

/**
 * Стратегия чтения значения из {@link ServerWebExchange} для {@link Holder}.
 * <p>
 * Реализации переопределяют только ту сторону (request/response), которую обслуживают.
 * Дефолт бросает {@link UnifiedAuditException}.
 */
public interface Extractor {

  /**
   * Извлекает значение из HTTP-запроса.
   *
   * @param exchange текущий {@link ServerWebExchange} с кэшем тел в атрибутах.
   * @param holder   параметр или условие; {@link Holder#getKey()} — имя поля/заголовка.
   * @return строка для мапы {@link ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ParamHolder} → значение события.
   *         {@code null}, если значения нет.
   * @throws UnifiedAuditException если экстрактор не работает с запросом.
   */
  default String extractRequest(ServerWebExchange exchange, Holder holder) {
    throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTION_NOT_SUPPORTED_FOR, holder.getName());
  }

  /**
   * Извлекает значение из HTTP-ответа.
   *
   * @param exchange текущий {@link ServerWebExchange} после записи ответа.
   * @param holder   параметр или условие.
   * @return строка для мапы {@link ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ParamHolder} → значение события.
   *         {@code null}, если значения нет.
   * @throws UnifiedAuditException если экстрактор не работает с ответом.
   */
  default String extractResponse(ServerWebExchange exchange, Holder holder) {
    throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTION_NOT_SUPPORTED_FOR, holder.getName());
  }
}
