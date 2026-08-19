package ru.sbrf.sbererp.core.common.unified.audit.service;

import java.util.List;
import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.unified.audit.adapter.EventAdapter;

/**
 * Интерфейс сервиса, предоставляющего методы для отправки событий в АС Единый Аудит.
 * <p>
 * Предназначен для интеграции с АС Единый Аудит. Содержит два метода: один для отправки одного
 * события, второй — для отправки нескольких событий.
 */
public interface AuditClientService {

  /**
   * Отправляет отдельное событие аудита в систему.
   *
   * @param event событие, которое необходимо отправить
   * @return сигнал завершения отправки
   */
  Mono<Void> sendEvent(EventAdapter event);

  /**
   * Отправляет список событий аудита в систему.
   *
   * @param event список событий, которые необходимо отправить
   * @return сигнал завершения отправки
   */
  Mono<Void> sendEvent(List<EventAdapter> event);
}
