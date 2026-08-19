package ru.sbrf.sbererp.core.common.unified.audit.extractor;

import ru.sbrf.sbererp.core.common.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.Holder;
import ru.sbrf.sbererp.core.common.unified.audit.util.LogMessage;
import org.springframework.web.server.ServerWebExchange;

/**
 * Интерфейс извлечения значений параметров событий аудита из реактивного HTTP-обмена.
 * <p>
 * Реализации получают конкретные значения из {@link ServerWebExchange}. Методы по умолчанию
 * бросают исключение, если экстрактор не поддерживает операцию.
 */
public interface Extractor {

  /**
   * Извлекает значение параметра из HTTP-запроса обмена.
   *
   * @param exchange текущий обмен
   * @param holder   описание параметра
   * @return строковое представление значения
   * @throws UnifiedAuditException если операция не поддерживается
   */
  default String extractRequest(ServerWebExchange exchange, Holder holder) {
    throw new UnifiedAuditException(LogMessage.EXTRACTION_NOT_SUPPORTED_FOR, holder.getName());
  }

  /**
   * Извлекает значение параметра из HTTP-ответа обмена.
   *
   * @param exchange текущий обмен
   * @param holder   описание параметра
   * @return строковое представление значения
   * @throws UnifiedAuditException если операция не поддерживается
   */
  default String extractResponse(ServerWebExchange exchange, Holder holder) {
    throw new UnifiedAuditException(LogMessage.EXTRACTION_NOT_SUPPORTED_FOR, holder.getName());
  }
}
