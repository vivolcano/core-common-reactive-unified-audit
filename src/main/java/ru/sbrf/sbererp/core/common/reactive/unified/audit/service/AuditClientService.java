package ru.sbrf.sbererp.core.common.reactive.unified.audit.service;

import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.adapter.EventAdapter;

/**
 * Отправляет одно событие в АС Единый Аудит.
 * <p>
 * Реализация вызывает блокирующий {@link com.sbt.audit.core.service.AuditService} только на
 * {@link reactor.core.scheduler.Schedulers#boundedElastic()}.
 * Ошибки клиента логируются и не пробрасываются в HTTP-цепочку.
 */
public interface AuditClientService {

  /**
   * Конвертирует адаптер в модель SBT и регистрирует событие.
   *
   * @param event собранное событие {@link EventAdapter}; {@code null} недопустим на стороне реализации.
   * @return {@link Mono#empty()} после попытки отправки.
   */
  Mono<Void> sendEvent(EventAdapter event);
}
