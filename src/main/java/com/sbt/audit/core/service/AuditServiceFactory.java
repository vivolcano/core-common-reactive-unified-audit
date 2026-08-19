package com.sbt.audit.core.service;

import com.sbt.audit.core.config.AuditConfig;
import java.util.Objects;

/**
 * Заглушка фабрики блокирующего {@link AuditService}.
 */
public class AuditServiceFactory {

  /**
   * Создаёт фабрику по конфигурации клиента.
   *
   * @param auditConfig конфигурация; {@code null} недопустим
   */
  public AuditServiceFactory(AuditConfig auditConfig) {
    Objects.requireNonNull(auditConfig, "auditConfig");
  }

  /**
   * Возвращает блокирующую службу аудита.
   *
   * @return заглушка {@link AuditService}
   */
  public AuditService getAuditService() {
    return new NoOpAuditService();
  }
}
