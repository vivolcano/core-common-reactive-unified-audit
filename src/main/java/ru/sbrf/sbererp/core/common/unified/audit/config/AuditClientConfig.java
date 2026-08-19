package ru.sbrf.sbererp.core.common.unified.audit.config;

import com.sbt.audit.core.config.AuditConfig;
import com.sbt.audit.core.config.PropertiesAuditConfigBuilder;
import com.sbt.audit.core.service.AuditService;
import com.sbt.audit.core.service.AuditServiceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditClientProperties;

/**
 * Конфигурационный класс для интеграции клиента аудита.
 * <p>
 * Определяет бины необходимых компонентов для работы механизма аудита:
 * <ul>
 *   <li>конфигурация аудита ({@link AuditConfig});</li>
 *   <li>служба аудита ({@link AuditService}).</li>
 * </ul>
 * Сама служба блокирующая: вызовы выполняются только через {@code boundedElastic}.
 */
@Configuration
public class AuditClientConfig {

  /**
   * Создает и возвращает конфигурацию аудита на основе свойств клиента аудита.
   *
   * @param properties конфигурационные свойства клиента аудита
   * @return сконфигурированный объект {@link AuditConfig}
   */
  @Bean
  public AuditConfig auditConfig(AuditClientProperties properties) {
    return new PropertiesAuditConfigBuilder(properties.config()).build();
  }

  /**
   * Создает и возвращает службу аудита на основе заданной конфигурации.
   *
   * @param auditConfig конфигурация аудита
   * @return инстанцированная служба аудита {@link AuditService}
   */
  @Bean
  public AuditService auditService(AuditConfig auditConfig) {
    var auditServiceFactory = new AuditServiceFactory(auditConfig);
    return auditServiceFactory.getAuditService();
  }
}
