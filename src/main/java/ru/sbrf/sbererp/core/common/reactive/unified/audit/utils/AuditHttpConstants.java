package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * HTTP-имена, используемые фильтром, экстракторами и шапкой события.
 */
@UtilityClass
public final class AuditHttpConstants {

  /** Cookie сессии servlet-контейнера. */
  public static final String JSESSIONID = "JSESSIONID";

  /** Префикс session id, взятого из cookie. */
  public static final String COOKIE_WITH_UNDERSCORE = "cookie_";

  /** Заголовок корреляции. */
  public static final String X_CORRELATION_ID = "X-Correlation-ID";

  /** Префикс session id из {@link #X_CORRELATION_ID}. */
  public static final String CORRELATION_WITH_UNDERSCORE = "correlation_";

  /** Заголовок идентификатора сессии. */
  public static final String X_SESSION_ID = "X-Session-ID";

  /** Заголовок идентификатора запроса. */
  public static final String REQUEST_ID = "request-id";

  /** GET: {@link ru.sbrf.sbererp.core.common.reactive.unified.audit.filter.AuditWebFilter} не читает тело. */
  public static final String GET_METHOD = "GET";

  /** FQCN Spring Data {@link org.springframework.data.domain.Pageable} для выбора экстрактора. */
  public static final String PAGEABLE_CLASS_NAME = "org.springframework.data.domain.Pageable";

  /** Query-параметр номера страницы. */
  public static final String PAGE = "page";

  /** Query-параметр размера страницы. */
  public static final String SIZE = "size";

  /** Query-параметр сортировки. */
  public static final String SORT = "sort";

  /** Формат {@link String#format(String, Object...)} для логирования Pageable. */
  public static final String PAGEABLE_STRING_PATTERN = "{page=%s, size=%s, sort=[%s]}";

  /** Ключ YAML/биндера: параметры запроса. */
  public static final String REQUEST = "REQUEST";

  /** Ключ YAML/биндера: параметры ответа. */
  public static final String RESPONSE = "RESPONSE";

  /** Ключ YAML/биндера: JWT claims. */
  public static final String CLAIMS = "CLAIMS";

  /** Ant-шаблон actuator. */
  public static final String ACTUATOR_ANT_PATTERN = "/actuator/**";

  /** Корневая страница swagger-ui. */
  public static final String SWAGGER_UI_HTML = "/swagger-ui.html";

  /** Ant-шаблон swagger-ui. */
  public static final String SWAGGER_UI_ANT_PATTERN = "/swagger-ui/**";

  /** OpenAPI JSON. */
  public static final String V3_API_DOCS = "/v3/api-docs";

  /** Ant-шаблон OpenAPI. */
  public static final String V3_API_DOCS_ANT_PATTERN = "/v3/api-docs/**";

  /** Ant-шаблон webjars. */
  public static final String WEBJARS_ANT_PATTERN = "/webjars/**";

  /** Favicon. */
  public static final String FAVICON_ICO = "/favicon.ico";
}
