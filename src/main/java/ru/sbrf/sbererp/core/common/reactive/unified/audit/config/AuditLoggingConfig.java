package ru.sbrf.sbererp.core.common.reactive.unified.audit.config;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.filter.AuditWebFilter;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditReactiveProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.resolver.AuditEventResolver;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditNumericConstants;

/**
 * Конфигурация WebFlux-фильтра аудита.
 *
 * <p>Пути {@code audit.reactive.exclude-path-patterns} не обрабатываются.
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public final class AuditLoggingConfig {

  /**
   * Порядок фильтра: ближе к клиенту, чтобы декоратор видел исходные тела запроса и ответа.
   */
  public static final int FILTER_ORDER = HIGHEST_PRECEDENCE + AuditNumericConstants.FILTER_ORDER_OFFSET;

  private final AuditEventResolver auditEventResolver;
  private final AuditReactiveProperties reactiveProperties;

  /**
   * Регистрирует основной WebFlux-фильтр аудита.
   *
   * @return {@link AuditWebFilter} с порядком {@link #FILTER_ORDER}.
   */
  @Bean
  @Order(FILTER_ORDER)
  public AuditWebFilter unifiedAuditWebFilter() {
    return new AuditWebFilter(auditEventResolver, reactiveProperties);
  }
}
