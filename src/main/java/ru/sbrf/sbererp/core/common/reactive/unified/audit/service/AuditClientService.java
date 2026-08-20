package ru.sbrf.sbererp.core.common.reactive.unified.audit.service;

import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.adapter.EventAdapter;

/**
 * Отправляет одно событие в АС Единый Аудит.
 *
 * <p>Ошибки клиента логируются и не пробрасываются в HTTP-цепочку.
 */
public interface AuditClientService {

  /**
   * Конвертирует адаптер в модель SBT и регистрирует событие.
   *
   * @param event собранное событие
   * @return {@link Mono#empty()} после попытки отправки
   */
  Mono<Void> sendEvent(EventAdapter event);
}
