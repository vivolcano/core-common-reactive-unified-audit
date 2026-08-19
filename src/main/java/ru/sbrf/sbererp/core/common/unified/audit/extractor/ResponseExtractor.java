package ru.sbrf.sbererp.core.common.unified.audit.extractor;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.unified.audit.extractor.util.ExtractorUtil;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.Holder;
import ru.sbrf.sbererp.core.common.unified.audit.util.Constants;

/**
 * Enum, содержащий экстракторы, реализующие механизмы извлечения параметров из HTTP-ответов. Каждый
 * элемент перечисления предназначен для извлечения определенных типов данных из ответа сервера.
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
      int value = Objects.isNull(status) ? 200 : status.value();
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
      return String.join(Constants.COMMA_WITH_SPACE, headerValues);
    }
  },

  /**
   * Экстрактор для извлечения полей или всего тела HTTP-ответа. Сначала пытается извлечь отдельное
   * поле, если не находит — возвращает тело ответа.
   */
  RESPONSE_BODY {
    @Override
    public String extractResponse(ServerWebExchange exchange, Holder holder) {
      String jsonString = ExtractorUtil.parseResponseBody(exchange);
      String result = ExtractorUtil.extractFieldValue(jsonString, holder);
      if (ObjectUtils.isEmpty(result)) {
        return ExtractorUtil.preparationString(jsonString, holder);
      }
      return result;
    }
  }
}
