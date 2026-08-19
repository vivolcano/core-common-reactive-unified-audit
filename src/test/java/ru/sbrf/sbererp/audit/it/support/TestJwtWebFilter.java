package ru.sbrf.sbererp.audit.it.support;

import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Кладёт JWT-claims в {@code Authentication.details}, чтобы интеграционные тесты
 * проверяли экстрактор {@code claims} и шапку события.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestJwtWebFilter implements WebFilter {

  static final Map<String, Object> TEST_CLAIMS = Map.of(
      "sub", "audit-user",
      "given_name", "Ivan",
      "patronymic", "Ivanovich",
      "family_name", "Petrov",
      "sid", "node-sid",
      "jti", "jwt-id"
  );

  /**
   * {@inheritDoc}
   */
  @Override
  @NonNull
  public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken("audit-user", "n/a", List.of());
    authentication.setDetails(TEST_CLAIMS);
    return chain.filter(exchange)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
  }
}
