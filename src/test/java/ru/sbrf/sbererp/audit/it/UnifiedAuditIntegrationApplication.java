package ru.sbrf.sbererp.audit.it;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Тестовое WebFlux-приложение для интеграционных проверок фильтра и YAML-модели аудита.
 */
@SpringBootApplication
public class UnifiedAuditIntegrationApplication {

  /**
   * @param args аргументы командной строки
   */
  public static void main(String[] args) {
    SpringApplication.run(UnifiedAuditIntegrationApplication.class, args);
  }
}
