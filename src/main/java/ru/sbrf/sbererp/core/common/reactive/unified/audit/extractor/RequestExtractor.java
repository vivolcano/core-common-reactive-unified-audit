package ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor;

import io.vavr.control.Option;
import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.Holder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditEventHeaderUtils;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditHttpConstants;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditJsonExtractionUtils;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditTextConstants;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.ReactiveSecurityContextUtils;

/**
 * Экстракторы стороны HTTP-запроса: path, query, header, body, Pageable, JWT-claim.
 */
public enum RequestExtractor implements Extractor {

  /**
   * URI-шаблон ({@link PathVariable}).
   */
  PATH_VARIABLE(PathVariable.class, RequestExtractor::pathVariableName) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      final Map<String, String> pathVariables = exchange.getAttribute(
          HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
      );
      return Objects.isNull(pathVariables) ? null : pathVariables.get(holder.getKey());
    }
  },

  /**
   * Query-параметры ({@link RequestParam}).
   */
  REQUEST_PARAM(RequestParam.class, RequestExtractor::requestParamName) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      final List<String> values = exchange.getRequest().getQueryParams().get(holder.getKey());
      return ObjectUtils.isEmpty(values) ? null : String.join(AuditTextConstants.COMMA, values);
    }
  },

  /**
   * HTTP-заголовки запроса ({@link RequestHeader}).
   */
  REQUEST_HEADER(RequestHeader.class, RequestExtractor::requestHeaderName) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      final List<String> values = exchange.getRequest().getHeaders().get(holder.getKey());
      return ObjectUtils.isEmpty(values) ? null : String.join(AuditTextConstants.COMMA_WITH_SPACE, values);
    }
  },

  /**
   * Тело запроса целиком ({@link RequestBody}).
   */
  REQUEST_BODY(RequestBody.class, annotation -> null) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      return AuditJsonExtractionUtils.preparationString(AuditJsonExtractionUtils.parseRequestBody(exchange), holder);
    }
  },

  /**
   * Поле JSON-тела запроса.
   */
  REQUEST_BODY_FIELD(null, null) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      return AuditJsonExtractionUtils.extractFieldValue(AuditJsonExtractionUtils.parseRequestBody(exchange), holder);
    }
  },

  /**
   * Параметры пагинации ({@code page}, {@code size}, {@code sort}).
   */
  PAGEABLE(null, null) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      return AuditJsonExtractionUtils.getPageableParams(exchange.getRequest());
    }
  },

  /**
   * JWT-claim из мапы claim → значение в атрибутах обмена.
   */
  CLAIM(null, null) {
    @Override
    public String extractRequest(ServerWebExchange exchange, Holder holder) {
      final String claimName = Objects.requireNonNullElse(holder.getKey(), holder.getName());
      return AuditEventHeaderUtils.getStringValue(
          ReactiveSecurityContextUtils.getTokenParamsMap(exchange),
          claimName
      );
    }
  };

  private final Class<? extends Annotation> annotationClass;
  private final Function<Annotation, String> nameExtractor;

  /**
   * @param annotationClass класс аннотации источника
   * @param nameExtractor   функция {@link Annotation} → имя параметра
   */
  @SuppressWarnings("unchecked")
  <T extends Annotation> RequestExtractor(
      Class<T> annotationClass,
      Function<T, String> nameExtractor) {
    this.annotationClass = annotationClass;
    this.nameExtractor = (Function<Annotation, String>) nameExtractor;
  }

  /**
   * Выбирает экстрактор по аннотации параметра и устанавливает ключ.
   *
   * @param parameter параметр метода контроллера
   * @param holder    держатель параметра или условия
   */
  public static void setExtractor(Parameter parameter, Holder holder) {
    if (Objects.nonNull(holder.getExtractor())) {
      return;
    }
    if (Objects.equals(parameter.getType().getName(), AuditHttpConstants.PAGEABLE_CLASS_NAME)) {
      holder.setExtractor(PAGEABLE);
    }
    findExtractorByAnnotation(parameter)
        .forEach(extractor -> bindExtractor(parameter, holder, extractor));
  }

  /**
   * Ставит экстрактор по умолчанию ({@link #REQUEST_BODY_FIELD}).
   *
   * @param holder держатель параметра
   */
  public static void setExtractor(Holder holder) {
    holder.setExtractor(REQUEST_BODY_FIELD);
  }

  /**
   * @param parameter параметр метода
   * @return найденный экстрактор
   */
  private static Option<RequestExtractor> findExtractorByAnnotation(Parameter parameter) {
    return Option.ofOptional(
        Arrays.stream(values())
            .filter(extractor -> Objects.nonNull(extractor.annotationClass))
            .filter(extractor -> parameter.isAnnotationPresent(extractor.annotationClass))
            .findFirst()
    );
  }

  private static void bindExtractor(Parameter parameter, Holder holder, RequestExtractor extractor) {
    final Annotation annotation = parameter.getAnnotation(extractor.annotationClass);
    final String key = extractor.nameExtractor.apply(annotation);
    holder.setExtractor(extractor);
    if (Objects.isNull(holder.getKey())) {
      holder.setKey(StringUtils.hasText(key) ? key : parameter.getName());
    }
  }

  private static String pathVariableName(PathVariable pathVariable) {
    return pathVariable.name().isEmpty() ? pathVariable.value() : pathVariable.name();
  }

  private static String requestParamName(RequestParam requestParam) {
    return requestParam.name().isEmpty() ? requestParam.value() : requestParam.name();
  }

  private static String requestHeaderName(RequestHeader requestHeader) {
    return requestHeader.name().isEmpty() ? requestHeader.value() : requestHeader.name();
  }
}
