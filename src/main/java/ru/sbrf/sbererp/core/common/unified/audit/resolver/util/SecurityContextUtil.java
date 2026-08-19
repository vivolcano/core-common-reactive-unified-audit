package ru.sbrf.sbererp.core.common.unified.audit.resolver.util;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.unified.audit.web.AuditExchangeAttributes;

/**
 * Утилиты извлечения claims из реактивного SecurityContext.
 */
@UtilityClass
public class SecurityContextUtil {

  /**
   * Читает карту параметров токена, ранее положенную фильтром/резолвером в атрибуты обмена.
   *
   * @param exchange текущий обмен
   * @return карта параметров или пустая карта
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> getTokenParamsMap(ServerWebExchange exchange) {
    Object stored = exchange.getAttribute(AuditExchangeAttributes.TOKEN_PARAMS);
    return stored instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
  }

  /**
   * Асинхронно извлекает карту details из {@link ReactiveSecurityContextHolder}.
   *
   * @return карта параметров токена или пустая карта
   */
  public static Mono<Map<String, Object>> loadTokenParamsMap() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContextUtil::extractDetails)
        .defaultIfEmpty(Map.of());
  }

  /**
   * Достаёт {@code details} аутентификации, если это карта.
   *
   * @param context контекст безопасности
   * @return карта details или пустая карта
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
   * Сохраняет карту claims в атрибуты обмена для синхронных экстракторов.
   *
   * @param exchange текущий обмен
   * @param params   карта claims
   * @return тот же обмен
   */
  public static ServerWebExchange storeTokenParams(
      ServerWebExchange exchange,
      Map<String, Object> params) {
    Map<String, Object> safeParams = Objects.isNull(params) ? Map.of() : params;
    exchange.getAttributes().put(AuditExchangeAttributes.TOKEN_PARAMS, safeParams);
    return exchange;
  }
}
