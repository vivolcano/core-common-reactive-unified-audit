package com.sbt.audit.core.service;

import com.sbt.audit.core.model.v2.event.Event;
import com.sbt.audit.core.model.v2.metamodel.Metamodel;
import java.util.List;

/**
 * Блокирующий клиент Единого аудита.
 * <p>
 * Реальная реализация выполняет сетевой I/O. Вызовы из WebFlux-кода обязательно оборачиваются в
 * {@code boundedElastic}, чтобы не блокировать event loop.
 */
public interface AuditService {

  /**
   * Отправляет одно событие аудита.
   *
   * @param event событие
   * @return идентификатор зарегистрированного события
   */
  String audit(Event event);

  /**
   * Отправляет набор событий аудита.
   *
   * @param events список событий
   * @return идентификаторы зарегистрированных событий
   */
  List<String> audit(List<Event> events);

  /**
   * Регистрирует метамодель событий.
   *
   * @param metamodel метамодель
   * @return хеш зарегистрированной метамодели
   */
  String register(Metamodel metamodel);
}
