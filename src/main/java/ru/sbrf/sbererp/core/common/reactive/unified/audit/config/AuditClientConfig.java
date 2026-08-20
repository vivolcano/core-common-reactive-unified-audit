package ru.sbrf.sbererp.core.common.reactive.unified.audit.config;

import com.sbt.audit.core.config.AuditConfig;
import com.sbt.audit.core.config.PropertiesAuditConfigBuilder;
import com.sbt.audit.core.service.AuditService;
import com.sbt.audit.core.service.AuditServiceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.AuditClientProperties;

/**
 * Бины блокирующего клиента SBT: {@link AuditConfig} и {@link AuditService}.
 * <p>
 * {@link AuditService} нельзя вызывать с event loop — только через {@code unifiedAuditElasticScheduler}.
 */
@Configuration(proxyBeanMethods = false)
public final class AuditClientConfig {

  /**
   * Создает и возвращает конфигурацию аудита на основе свойств клиента аудита.
   *
   * @param properties конфигурационные свойства клиента аудита.
   * @return сконфигурированный объект {@link AuditConfig}.
   */
  @Bean
  public AuditConfig auditConfig(AuditClientProperties properties) {
    return new PropertiesAuditConfigBuilder(properties.config()).build();
  }

  /**
   * Создает и возвращает службу аудита на основе заданной конфигурации.
   *
   * @param auditConfig конфигурация аудита.
   * @return инстанцированная служба аудита {@link AuditService}.
   */
  @Bean
  public AuditService auditService(AuditConfig auditConfig) {
    var auditServiceFactory = new AuditServiceFactory(auditConfig);
    return auditServiceFactory.getAuditService();
  }
}
