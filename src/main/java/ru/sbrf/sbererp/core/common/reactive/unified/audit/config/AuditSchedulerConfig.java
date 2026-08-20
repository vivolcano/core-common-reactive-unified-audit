package ru.sbrf.sbererp.core.common.reactive.unified.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Регистрирует выделенный {@code boundedElastic}-планировщик для блокирующего клиента аудита.
 * <p>
 * Event loop Netty нельзя блокировать вызовами {@link com.sbt.audit.core.service.AuditService}:
 * все обращения к библиотеке выполняются на этом планировщике.
 */
@Configuration(proxyBeanMethods = false)
public final class AuditSchedulerConfig {

  /**
   * Имя бина планировщика блокирующих вызовов аудита.
   */
  public static final String ELASTIC_SCHEDULER = "unifiedAuditElasticScheduler";

  /** Префикс потоков блокирующего клиента аудита. */
  public static final String THREAD_NAME_PREFIX = "unified-audit";

  /** {@code destroyMethod} бина планировщика. */
  public static final String DESTROY_METHOD = "dispose";

  /**
   * Создаёт именованный {@code boundedElastic}-пул с префиксом потоков {@code unified-audit}.
   *
   * @return планировщик для блокирующего I/O аудита.
   */
  @Bean(name = ELASTIC_SCHEDULER, destroyMethod = DESTROY_METHOD)
  public Scheduler unifiedAuditElasticScheduler() {
    return Schedulers.newBoundedElastic(
        Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
        Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
        THREAD_NAME_PREFIX
    );
  }
}
