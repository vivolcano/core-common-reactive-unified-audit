package ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor;

import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.Holder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditExceptionMessages;

/**
 * Стратегия чтения значения из {@link ServerWebExchange} для {@link Holder}.
 *
 * <p>Реализации переопределяют только ту сторону (request/response), которую обслуживают.
 */
public interface Extractor {

  /**
   * @param exchange текущий обмен
   * @param holder   параметр или условие
   * @return строка для события либо {@code null}
   * @throws UnifiedAuditException если экстрактор не работает с запросом
   */
  default String extractRequest(ServerWebExchange exchange, Holder holder) {
    throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTION_NOT_SUPPORTED_FOR, holder.getName());
  }

  /**
   * @param exchange текущий обмен
   * @param holder   параметр или условие
   * @return строка для события либо {@code null}
   * @throws UnifiedAuditException если экстрактор не работает с ответом
   */
  default String extractResponse(ServerWebExchange exchange, Holder holder) {
    throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTION_NOT_SUPPORTED_FOR, holder.getName());
  }
}
