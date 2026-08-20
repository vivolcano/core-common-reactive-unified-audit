package ru.sbrf.sbererp.core.common.reactive.unified.audit.config;

import com.sbt.audit.core.config.AuditConfig;
import com.sbt.audit.core.config.PropertiesAuditConfigBuilder;
import com.sbt.audit.core.service.AuditService;
import com.sbt.audit.core.service.AuditServiceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditClientProperties;

/**
 * Бины блокирующего клиента SBT: {@link AuditConfig} и {@link AuditService}.
 *
 * <p>{@link AuditService} нельзя вызывать с event loop — только через {@code unifiedAuditElasticScheduler}.
 */
@Configuration(proxyBeanMethods = false)
public final class AuditClientConfig {

  /**
   * Создаёт конфигурацию аудита из {@link AuditClientProperties#config()}.
   *
   * @param properties конфигурационные свойства клиента.
   * @return {@link AuditConfig}.
   */
  @Bean
  public AuditConfig auditConfig(AuditClientProperties properties) {
    return new PropertiesAuditConfigBuilder(properties.config()).build();
  }

  /**
   * Создаёт блокирующий {@link AuditService} по заданной конфигурации.
   *
   * @param auditConfig конфигурация аудита.
   * @return {@link AuditService}.
   */
  @Bean
  public AuditService auditService(AuditConfig auditConfig) {
    final AuditServiceFactory auditServiceFactory = new AuditServiceFactory(auditConfig);
    return auditServiceFactory.getAuditService();
  }
}
