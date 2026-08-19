package ru.sbrf.sbererp.core.common.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * Имена атрибутов {@code ServerWebExchange}, в которых фильтр аудита хранит кэш тел и claims.
 */
@UtilityClass
public final class AuditExchangeAttributeNames {

  /** Кэшированное тело HTTP-запроса. */
  public static final String CACHED_REQUEST_BODY = "unified.audit.cachedRequestBody";

  /** Кэшированное тело HTTP-ответа. */
  public static final String CACHED_RESPONSE_BODY = "unified.audit.cachedResponseBody";

  /** Карта claims/details из реактивного SecurityContext. */
  public static final String TOKEN_PARAMS = "unified.audit.tokenParams";

  /** Пустое тело, если запись отсутствует. */
  public static final byte[] EMPTY_BODY = new byte[0];
}
