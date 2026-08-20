package ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.Holder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditJsonExtractionUtils;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditTextConstants;

/**
 * Экстракторы стороны HTTP-ответа: статус, заголовок, JSON-тело.
 */
@Getter
public enum ResponseExtractor implements Extractor {

  /**
   * HTTP-статус ответа.
   */
  RESPONSE_CODE {
    /**
     * {@inheritDoc}
     */
    @Override
    public String extractResponse(ServerWebExchange exchange, Holder holder) {
      final HttpStatusCode status = exchange.getResponse().getStatusCode();
      final int value = Objects.isNull(status) ? HttpStatus.OK.value() : status.value();
      return String.valueOf(value);
    }
  },

  /**
   * HTTP-заголовки ответа.
   */
  RESPONSE_HEADER {
    /**
     * {@inheritDoc}
     */
    @Override
    public String extractResponse(ServerWebExchange exchange, Holder holder) {
      final List<String> headerValues = exchange.getResponse().getHeaders().get(holder.getKey());
      return ObjectUtils.isEmpty(headerValues)
          ? null
          : String.join(AuditTextConstants.COMMA_WITH_SPACE, headerValues);
    }
  },

  /**
   * Поле JSON-тела ответа либо тело целиком, если поле не найдено.
   */
  RESPONSE_BODY {
    /**
     * {@inheritDoc}
     */
    @Override
    public String extractResponse(ServerWebExchange exchange, Holder holder) {
      final String jsonString = AuditJsonExtractionUtils.parseResponseBody(exchange);
      final String result = AuditJsonExtractionUtils.extractFieldValue(jsonString, holder);
      return ObjectUtils.isEmpty(result)
          ? AuditJsonExtractionUtils.preparationString(jsonString, holder)
          : result;
    }
  }
}
