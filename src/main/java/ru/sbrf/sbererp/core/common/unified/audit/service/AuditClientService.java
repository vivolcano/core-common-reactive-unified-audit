package ru.sbrf.sbererp.core.common.unified.audit.service;

import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.unified.audit.adapter.EventAdapter;

/**
 * Отправляет одно событие в АС Единый Аудит.
 * <p>
 * Реализация вызывает блокирующий {@code AuditService} только на {@code boundedElastic}.
 * Ошибки клиента логируются и не пробрасываются в HTTP-цепочку.
 */
public interface AuditClientService {

  /**
   * Конвертирует адаптер в модель SBT и регистрирует событие.
   *
   * @param event собранное событие; {@code null} недопустим на стороне реализации.
   * @return {@link Mono#empty()} после попытки отправки
   */
  Mono<Void> sendEvent(EventAdapter event);
}
