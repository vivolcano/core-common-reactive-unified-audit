package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import io.vavr.control.Option;
import java.util.Map;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT-claims из {@link ReactiveSecurityContextHolder} и атрибут {@link AuditExchangeAttributeNames#TOKEN_PARAMS}.
 */
@UtilityClass
public final class ReactiveSecurityContextUtils {

  /**
   * Читает JWT-claims, ранее положенные в атрибуты обмена.
   *
   * @param exchange текущий обмен.
   * @return JWT-claim → значение либо пустая мапа.
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> getTokenParamsMap(ServerWebExchange exchange) {
    final Object stored = exchange.getAttribute(AuditExchangeAttributeNames.TOKEN_PARAMS);
    return stored instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
  }

  /**
   * Асинхронно извлекает JWT-claims из {@link ReactiveSecurityContextHolder}.
   *
   * @return JWT-claim → значение из {@link ReactiveSecurityContextHolder} либо пустая мапа.
   */
  public static Mono<Map<String, Object>> loadTokenParamsMap() {
    return ReactiveSecurityContextHolder.getContext()
        .map(ReactiveSecurityContextUtils::extractDetails)
        .defaultIfEmpty(Map.of());
  }

  /**
   * Достаёт {@code details} аутентификации, если это JWT-claim → значение.
   *
   * @param context контекст безопасности.
   * @return JWT-claim → значение либо пустая мапа.
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> extractDetails(SecurityContext context) {
    return Option.of(context.getAuthentication())
        .flatMap(authentication -> Option.of(authentication.getDetails()))
        .filter(Map.class::isInstance)
        .map(details -> (Map<String, Object>) details)
        .getOrElse(Map::of);
  }

  /**
   * Сохраняет JWT-claims в атрибуты обмена для синхронных экстракторов.
   *
   * @param exchange текущий обмен.
   * @param params   JWT-claim → значение.
   * @return тот же обмен.
   */
  public static ServerWebExchange storeTokenParams(
      ServerWebExchange exchange,
      Map<String, Object> params) {
    final Map<String, Object> safeParams = Objects.isNull(params) ? Map.of() : params;
    exchange.getAttributes().put(AuditExchangeAttributeNames.TOKEN_PARAMS, safeParams);
    return exchange;
  }
}
