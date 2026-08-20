package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT-claims из {@link ReactiveSecurityContextHolder} и атрибут обмена
 * {@link AuditExchangeAttributeNames#TOKEN_PARAMS}.
 */
@UtilityClass
public final class ReactiveSecurityContextUtils {

  /**
   * Читает мапу JWT-claim → значение, ранее положенную фильтром/резолвером в атрибуты обмена.
   *
   * @param exchange текущий {@link ServerWebExchange}.
   * @return мапа claim → значение или пустая мапа.
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> getTokenParamsMap(ServerWebExchange exchange) {
    Object stored = exchange.getAttribute(AuditExchangeAttributeNames.TOKEN_PARAMS);
    return stored instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
  }

  /**
   * Асинхронно извлекает мапу {@code details} из {@link ReactiveSecurityContextHolder}.
   *
   * @return мапа JWT-claim → значение или пустая мапа.
   */
  public static Mono<Map<String, Object>> loadTokenParamsMap() {
    return ReactiveSecurityContextHolder.getContext()
        .map(ReactiveSecurityContextUtils::extractDetails)
        .defaultIfEmpty(Map.of());
  }

  /**
   * Достаёт {@code details} аутентификации, если это мапа JWT-claim → значение.
   *
   * @param context контекст безопасности {@link SecurityContext}.
   * @return мапа claim → значение или пустая мапа.
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> extractDetails(SecurityContext context) {
    return Optional.ofNullable(context.getAuthentication())
        .map(Authentication::getDetails)
        .filter(Map.class::isInstance)
        .map(details -> (Map<String, Object>) details)
        .orElseGet(Map::of);
  }

  /**
   * Сохраняет мапу JWT-claim → значение в атрибуты обмена для синхронных экстракторов.
   *
   * @param exchange текущий {@link ServerWebExchange}.
   * @param params   мапа JWT-claim → значение.
   * @return тот же обмен.
   */
  public static ServerWebExchange storeTokenParams(
      ServerWebExchange exchange,
      Map<String, Object> params) {
    Map<String, Object> safeParams = Objects.isNull(params) ? Map.of() : params;
    exchange.getAttributes().put(AuditExchangeAttributeNames.TOKEN_PARAMS, safeParams);
    return exchange;
  }
}
