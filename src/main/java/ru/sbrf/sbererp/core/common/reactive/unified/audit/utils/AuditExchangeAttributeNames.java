package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * Имена атрибутов {@link org.springframework.web.server.ServerWebExchange}, в которых фильтр аудита
 * хранит кэш тел и JWT-claims.
 */
@UtilityClass
public final class AuditExchangeAttributeNames {

  /** Кэшированное тело HTTP-запроса. */
  public static final String CACHED_REQUEST_BODY = "unified.audit.cachedRequestBody";

  /** Кэшированное тело HTTP-ответа. */
  public static final String CACHED_RESPONSE_BODY = "unified.audit.cachedResponseBody";

  /**
   * Мапа JWT-claim → значение из {@link org.springframework.security.core.Authentication#getDetails()}
   * реактивного {@link org.springframework.security.core.context.SecurityContext}.
   */
  public static final String TOKEN_PARAMS = "unified.audit.tokenParams";

  /** Пустое тело, если запись отсутствует. */
  public static final byte[] EMPTY_BODY = new byte[AuditNumericConstants.ZERO];
}
