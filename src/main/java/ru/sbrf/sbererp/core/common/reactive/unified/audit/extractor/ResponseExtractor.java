package ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditJsonExtractionUtils;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.Holder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditTextConstants;

/**
 * Экстракторы стороны HTTP-ответа: статус, заголовок, JSON-тело.
 */
@Getter
public enum ResponseExtractor implements Extractor {

  /**
   * Экстрактор для извлечения статуса HTTP-ответа.
   */
  RESPONSE_CODE {
    @Override
    public String extractResponse(ServerWebExchange exchange, Holder holder) {
      HttpStatusCode status = exchange.getResponse().getStatusCode();
      int value = Objects.isNull(status) ? HttpStatus.OK.value() : status.value();
      return String.valueOf(value);
    }
  },

  /**
   * Экстрактор для извлечения значений HTTP-заголовков из ответа.
   */
  RESPONSE_HEADER {
    @Override
    public String extractResponse(ServerWebExchange exchange, Holder holder) {
      List<String> headerValues = exchange.getResponse().getHeaders().get(holder.getKey());
      if (ObjectUtils.isEmpty(headerValues)) {
        return null;
      }
      return String.join(AuditTextConstants.COMMA_WITH_SPACE, headerValues);
    }
  },

  /**
   * Экстрактор для извлечения полей или всего тела HTTP-ответа. Сначала пытается извлечь отдельное
   * поле, если не находит — возвращает тело ответа.
   */
  RESPONSE_BODY {
    @Override
    public String extractResponse(ServerWebExchange exchange, Holder holder) {
      String jsonString = AuditJsonExtractionUtils.parseResponseBody(exchange);
      String result = AuditJsonExtractionUtils.extractFieldValue(jsonString, holder);
      if (ObjectUtils.isEmpty(result)) {
        return AuditJsonExtractionUtils.preparationString(jsonString, holder);
      }
      return result;
    }
  }
}
