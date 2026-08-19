package ru.sbrf.sbererp.core.common.unified.audit.config;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import ru.sbrf.sbererp.core.common.unified.audit.filter.AuditWebFilter;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditReactiveProperties;
import ru.sbrf.sbererp.core.common.unified.audit.resolver.AuditEventResolver;

/**
 * Конфигурация WebFlux-фильтра аудита.
 * <p>
 * Один {@link AuditWebFilter} совмещает роли servlet-фильтров кеширования тел и аудита: буферизует
 * тело запроса/ответа и после завершения цепочки отправляет событие.
 */
@RequiredArgsConstructor
@Configuration
public class AuditLoggingConfig {

  /**
   * Порядок фильтра: ближе к клиенту, чтобы тело запроса было доступно контроллеру повторно.
   */
  public static final int FILTER_ORDER = HIGHEST_PRECEDENCE + 50;

  private final AuditEventResolver auditEventResolver;
  private final AuditReactiveProperties reactiveProperties;

  /**
   * Регистрирует основной WebFlux-фильтр аудита.
   *
   * @return фильтр аудита
   */
  @Bean
  @Order(FILTER_ORDER)
  public AuditWebFilter unifiedAuditWebFilter() {
    return new AuditWebFilter(auditEventResolver, reactiveProperties);
  }
}
