package ru.sbrf.sbererp.core.common.reactive.unified.audit.service;

import com.sbt.audit.core.model.v2.event.Event;
import com.sbt.audit.core.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.adapter.EventAdapter;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.config.AuditSchedulerConfig;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.converter.EventConverter;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditPrettyJsonUtils;

/**
 * Отправка события в АС Единый Аудит через блокирующий {@link AuditService}.
 *
 * <p>Вызов выполняется на {@link AuditSchedulerConfig#ELASTIC_SCHEDULER}. Исключения логируются и глотаются.
 */
@Slf4j
@Service
public final class AuditClientServiceImpl implements AuditClientService {

  private final AuditService auditService;
  private final EventConverter eventConverter;
  private final Scheduler auditScheduler;

  /**
   * @param auditService   блокирующий клиент SBT
   * @param eventConverter конвертер {@link EventAdapter} → {@link Event}
   * @param auditScheduler {@code boundedElastic} для блокирующего I/O
   */
  public AuditClientServiceImpl(
      AuditService auditService,
      EventConverter eventConverter,
      @Qualifier(AuditSchedulerConfig.ELASTIC_SCHEDULER) Scheduler auditScheduler) {
    this.auditService = auditService;
    this.eventConverter = eventConverter;
    this.auditScheduler = auditScheduler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> sendEvent(EventAdapter eventAdapter) {
    return Mono.fromCallable(() -> sendBlocking(eventAdapter))
        .subscribeOn(auditScheduler)
        .doOnError(error -> log.error(
            AuditLogMessages.AUDIT_EVENT_SEND_FAILED, eventAdapter.eventName(), error))
        .onErrorResume(this::swallowError)
        .then();
  }

  /**
   * Синхронная отправка. Вызывать только с {@code auditScheduler}.
   *
   * @param eventAdapter внутреннее событие
   * @return идентификатор, возвращённый {@link AuditService#audit(Event)}
   */
  private String sendBlocking(EventAdapter eventAdapter) {
    final Event event = eventConverter.convert(eventAdapter);
    log.info(AuditLogMessages.SENDING_AUDIT_EVENT, event.getName());
    if (log.isDebugEnabled()) {
      log.debug(
          AuditLogMessages.AUDIT_EVENT_PAYLOAD,
          AuditPrettyJsonUtils.getFormatString(event, AuditLogMessages.FAILED_TO_SERIALIZE_EVENT)
      );
    }
    final String eventId = auditService.audit(event);
    log.info(AuditLogMessages.SENT_AUDIT_EVENT, event.getName(), eventId);
    return eventId;
  }

  private <T> Mono<T> swallowError(Throwable throwable) {
    return Mono.empty();
  }
}
