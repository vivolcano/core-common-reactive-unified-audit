package ru.sbrf.sbererp.core.common.unified.audit.extractor;

import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.Holder;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditExceptionMessages;

/**
 * Стратегия чтения значения из {@link ServerWebExchange} для {@link Holder}.
 * <p>
 * Реализации переопределяют только ту сторону (request/response), которую обслуживают.
 * Дефолт бросает {@link UnifiedAuditException}.
 */
public interface Extractor {

  /**
   * @param exchange текущий обмен с кэшем тел в атрибутах.
   * @param holder   параметр или условие; {@link Holder#getKey()} — имя поля/заголовка.
   * @return строка для карты параметров события; {@code null} если значения нет
   * @throws UnifiedAuditException если экстрактор не работает с запросом
   */
  default String extractRequest(ServerWebExchange exchange, Holder holder) {
    throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTION_NOT_SUPPORTED_FOR, holder.getName());
  }

  /**
   * @param exchange текущий обмен после записи ответа.
   * @param holder   параметр или условие.
   * @return строка для карты параметров события; {@code null} если значения нет
   * @throws UnifiedAuditException если экстрактор не работает с ответом
   */
  default String extractResponse(ServerWebExchange exchange, Holder holder) {
    throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTION_NOT_SUPPORTED_FOR, holder.getName());
  }
}
