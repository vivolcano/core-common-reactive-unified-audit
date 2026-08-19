package ru.sbrf.sbererp.core.common.unified.audit.service;

import com.sbt.audit.core.model.v2.event.Event;
import com.sbt.audit.core.service.AuditService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import ru.sbrf.sbererp.core.common.unified.audit.adapter.EventAdapter;
import ru.sbrf.sbererp.core.common.unified.audit.config.AuditSchedulerConfig;
import ru.sbrf.sbererp.core.common.unified.audit.converter.EventConverter;
import ru.sbrf.sbererp.core.common.unified.audit.util.Constants;
import ru.sbrf.sbererp.core.common.unified.audit.util.LogMessage;
import ru.sbrf.sbererp.core.common.unified.audit.util.ObjectToFormatStringUtil;
import ru.sbrf.sbererp.core.common.unified.audit.util.enums.ApplicationGeneralError;

/**
 * Реализация {@link AuditClientService} для отправки событий в Единый аудит.
 * <p>
 * Преобразует {@link EventAdapter} в {@link Event} и вызывает блокирующий {@link AuditService}
 * строго на {@code boundedElastic}, чтобы не блокировать event loop Netty.
 */
@Slf4j
@Service
public class AuditClientServiceImpl implements AuditClientService {

  private final AuditService auditService;
  private final EventConverter eventConverter;
  private final Scheduler auditScheduler;

  /**
   * Создаёт сервис с выделенным elastic-планировщиком для блокирующего клиента.
   *
   * @param auditService    блокирующий клиент Единого аудита
   * @param eventConverter  конвертер адаптера в модель SBT
   * @param auditScheduler  {@code boundedElastic} для вызовов клиента
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
   * Отправляет одно событие аудита в систему.
   *
   * @param eventAdapter событие, которое необходимо отправить
   * @return сигнал завершения; ошибки клиента логируются и не пробрасываются
   */
  @Override
  public Mono<Void> sendEvent(EventAdapter eventAdapter) {
    return Mono.fromCallable(() -> sendSingleBlocking(eventAdapter))
        .subscribeOn(auditScheduler)
        .doOnNext(this::logSingleSuccess)
        .doOnError(this::logErrorEvent)
        .onErrorResume(this::swallowError)
        .then();
  }

  /**
   * Отправляет список событий аудита в систему.
   *
   * @param eventAdapter список событий
   * @return сигнал завершения; ошибки клиента логируются и не пробрасываются
   */
  @Override
  public Mono<Void> sendEvent(List<EventAdapter> eventAdapter) {
    return Mono.fromCallable(() -> sendBatchBlocking(eventAdapter))
        .subscribeOn(auditScheduler)
        .doOnNext(this::logBatchSuccess)
        .doOnError(this::logErrorEvent)
        .onErrorResume(this::swallowError)
        .then();
  }

  /**
   * Конвертирует и синхронно отправляет одно событие. Вызывается только на elastic-потоке.
   *
   * @param eventAdapter внутреннее событие
   * @return идентификатор, возвращённый клиентом
   */
  private String sendSingleBlocking(EventAdapter eventAdapter) {
    Event event = eventConverter.convert(eventAdapter);
    log.info(LogMessage.AUDIT_EVENT_SEND_START, event.getName());
    log.debug(ObjectToFormatStringUtil.getFormatString(event, Constants.ERROR_CONVERT_EVENT_TO_STRING));
    return auditService.audit(event);
  }

  /**
   * Конвертирует и синхронно отправляет набор событий. Вызывается только на elastic-потоке.
   *
   * @param eventAdapter список внутренних событий
   * @return идентификаторы, возвращённые клиентом
   */
  private List<String> sendBatchBlocking(List<EventAdapter> eventAdapter) {
    List<Event> events = eventAdapter.stream()
        .map(eventConverter::convert)
        .toList();
    log.info(
        LogMessage.AUDIT_EVENTS_SEND_START,
        events.stream().map(Event::getName).collect(Collectors.joining(Constants.COMMA))
    );
    return auditService.audit(events);
  }

  /**
   * Логирует успешную отправку одного события.
   *
   * @param eventId идентификатор события
   */
  private void logSingleSuccess(String eventId) {
    log.info(LogMessage.AUDIT_EVENT_SEND_SUCCESS, eventId);
  }

  /**
   * Логирует успешную массовую отправку.
   *
   * @param eventIds идентификаторы событий
   */
  private void logBatchSuccess(List<String> eventIds) {
    log.info(LogMessage.AUDIT_EVENTS_SEND_SUCCESS, String.join(Constants.COMMA, eventIds));
  }

  /**
   * Логирует ошибку отправки события аудита.
   *
   * @param throwable исключение, которое произошло при отправке события
   */
  private void logErrorEvent(Throwable throwable) {
    log.error(LogMessage.AUDIT_EVENT_SEND_FAIL, throwable.getMessage(), throwable);
    log.error(ApplicationGeneralError.FAILED_SEND_AUDIT_EVENT.getMessage());
  }

  /**
   * Поглощает ошибку клиента, чтобы сбой аудита не ломал HTTP-ответ.
   *
   * @param throwable ошибка отправки
   * @param <T>       тип сигнала
   * @return пустой сигнал
   */
  private <T> Mono<T> swallowError(Throwable throwable) {
    return Mono.empty();
  }
}
