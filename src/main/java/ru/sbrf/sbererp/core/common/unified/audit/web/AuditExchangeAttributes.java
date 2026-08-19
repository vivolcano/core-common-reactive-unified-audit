package ru.sbrf.sbererp.core.common.unified.audit.web;

/**
 * Ключи атрибутов {@code ServerWebExchange}, в которых фильтр аудита кладёт кэш тел и claims.
 */
public final class AuditExchangeAttributes {

  /**
   * Кэшированное тело HTTP-запроса.
   */
  public static final String CACHED_REQUEST_BODY = "unified.audit.cachedRequestBody";

  /**
   * Кэшированное тело HTTP-ответа.
   */
  public static final String CACHED_RESPONSE_BODY = "unified.audit.cachedResponseBody";

  /**
   * Карта claims/details из {@code ReactiveSecurityContextHolder}.
   */
  public static final String TOKEN_PARAMS = "unified.audit.tokenParams";

  /**
   * Пустое тело.
   */
  public static final byte[] EMPTY_BODY = new byte[0];

  private AuditExchangeAttributes() {
  }
}
