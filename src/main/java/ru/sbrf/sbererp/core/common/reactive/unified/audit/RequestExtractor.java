package ru.sbrf.sbererp.core.common.reactive.unified.audit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.Extractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditJsonExtractionUtils;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.Holder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditEventHeaderUtils;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.ReactiveSecurityContextUtils;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditTextConstants;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditHttpConstants;

/**
 * Экстракторы стороны HTTP-запроса: path, query, header, body, Pageable, JWT claim.
 */
public enum RequestExtractor implements Extractor {

  /**
   * Извлекает значение из URI-шаблона (аннотация {@link PathVariable}).
   */
  PATH_VARIABLE(PathVariable.class, RequestExtractor::pathVariableName) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      Map<String, String> pathVariables = exchange.getAttribute(
          HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
      );
      return Objects.isNull(pathVariables) ? null : pathVariables.get(holder.getKey());
    }
  },

  /**
   * Извлекает параметры из строки запроса (аннотация {@link RequestParam}).
   */
  REQUEST_PARAM(RequestParam.class, RequestExtractor::requestParamName) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      List<String> values = exchange.getRequest().getQueryParams().get(holder.getKey());
      if (ObjectUtils.isEmpty(values)) {
        return null;
      }
      return String.join(AuditTextConstants.COMMA, values);
    }
  },

  /**
   * Извлекает HTTP-заголовки (аннотация {@link RequestHeader}).
   */
  REQUEST_HEADER(RequestHeader.class, RequestExtractor::requestHeaderName) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      List<String> values = exchange.getRequest().getHeaders().get(holder.getKey());
      if (ObjectUtils.isEmpty(values)) {
        return null;
      }
      return String.join(AuditTextConstants.COMMA_WITH_SPACE, values);
    }
  },

  /**
   * Извлекает всё тело запроса как строку (аннотация {@link RequestBody}).
   */
  REQUEST_BODY(RequestBody.class, annotation -> null) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      return AuditJsonExtractionUtils.preparationString(AuditJsonExtractionUtils.parseRequestBody(exchange), holder);
    }
  },

  /**
   * Извлекает конкретное поле из JSON-тела запроса.
   */
  REQUEST_BODY_FIELD(null, null) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      String jsonString = AuditJsonExtractionUtils.parseRequestBody(exchange);
      return AuditJsonExtractionUtils.extractFieldValue(jsonString, holder);
    }
  },

  /**
   * Извлекает параметры пагинации (page, size, sort).
   */
  PAGEABLE(null, null) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      return AuditJsonExtractionUtils.getPageableParams(exchange.getRequest());
    }
  },

  /**
   * Извлекает claim по имени из мапы JWT-claim → значение в атрибутах обмена.
   */
  CLAIM(null, null) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      String claimName = Objects.requireNonNullElse(holder.getKey(), holder.getName());
      Map<String, Object> tokenParamsMap = ReactiveSecurityContextUtils.getTokenParamsMap(exchange);
      return AuditEventHeaderUtils.getStringValue(tokenParamsMap, claimName);
    }
  };

  /** Класс аннотации для извлечения имени параметра. */
  private final Class<? extends Annotation> annotationClass;
  /** Функция извлечения имени из аннотации. */
  private final Function<Annotation, String> nameExtractor;

  /**
   * Конструктор экстрактора.
   *
   * @param annotationClass класс аннотации источника {@link Annotation}.
   * @param nameExtractor   функция {@link Annotation} → имя параметра.
   */
  @SuppressWarnings("unchecked")
  <T extends Annotation> RequestExtractor(
      Class<T> annotationClass,
      Function<T, String> nameExtractor) {
    this.annotationClass = annotationClass;
    this.nameExtractor = (Function<Annotation, String>) nameExtractor;
  }

  /**
   * Определяет экстрактор по аннотации параметра и устанавливает ключ.
   *
   * @param parameter параметр метода контроллера {@link Parameter}.
   * @param holder    держатель параметра или условия {@link Holder}.
   */
  public static void setExtractor(Parameter parameter, Holder holder) {
    if (Objects.nonNull(holder.getExtractor())) {
      return;
    }
    if (Objects.equals(parameter.getType().getName(), AuditHttpConstants.PAGEABLE_CLASS_NAME)) {
      holder.setExtractor(PAGEABLE);
    }
    findExtractorByAnnotation(parameter)
        .ifPresent(extractor -> bindExtractor(parameter, holder, extractor));
  }

  /**
   * Устанавливает экстрактор по умолчанию ({@link #REQUEST_BODY_FIELD}).
   *
   * @param holder держатель параметра {@link Holder}.
   */
  public static void setExtractor(Holder holder) {
    holder.setExtractor(REQUEST_BODY_FIELD);
  }

  /**
   * Ищет экстрактор по аннотации параметра метода.
   *
   * @param parameter параметр метода {@link Parameter}.
   * @return найденный {@link RequestExtractor} или пустой {@link Optional}.
   */
  private static Optional<RequestExtractor> findExtractorByAnnotation(Parameter parameter) {
    return java.util.Arrays.stream(values())
        .filter(extractor -> Objects.nonNull(extractor.annotationClass))
        .filter(extractor -> parameter.isAnnotationPresent(extractor.annotationClass))
        .findFirst();
  }

  /**
   * Привязывает найденный экстрактор и ключ к держателю.
   *
   * @param parameter параметр метода {@link Parameter}.
   * @param holder    держатель {@link Holder}.
   * @param extractor найденный {@link RequestExtractor}.
   */
  private static void bindExtractor(Parameter parameter, Holder holder, RequestExtractor extractor) {
    Annotation annotation = parameter.getAnnotation(extractor.annotationClass);
    String key = extractor.nameExtractor.apply(annotation);
    holder.setExtractor(extractor);
    if (Objects.isNull(holder.getKey())) {
      holder.setKey(StringUtils.hasText(key) ? key : parameter.getName());
    }
  }

  /**
   * Возвращает имя path-variable из аннотации {@link PathVariable}.
   *
   * @param pathVariable аннотация {@link PathVariable}.
   * @return имя переменной пути.
   */
  private static String pathVariableName(PathVariable pathVariable) {
    return pathVariable.name().isEmpty() ? pathVariable.value() : pathVariable.name();
  }

  /**
   * Возвращает имя query-параметра из аннотации {@link RequestParam}.
   *
   * @param requestParam аннотация {@link RequestParam}.
   * @return имя параметра.
   */
  private static String requestParamName(RequestParam requestParam) {
    return requestParam.name().isEmpty() ? requestParam.value() : requestParam.name();
  }

  /**
   * Возвращает имя заголовка из аннотации {@link RequestHeader}.
   *
   * @param requestHeader аннотация {@link RequestHeader}.
   * @return имя заголовка.
   */
  private static String requestHeaderName(RequestHeader requestHeader) {
    return requestHeader.name().isEmpty() ? requestHeader.value() : requestHeader.name();
  }
}
