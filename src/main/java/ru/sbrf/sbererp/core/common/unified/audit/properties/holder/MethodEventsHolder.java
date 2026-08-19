package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import java.util.List;

/**
 * Record, представляющий контейнер для списка конфигураций событий, связанных с методом.
 * <p>
 * Используется для хранения и передачи информации о событиях аудита, которые должны быть
 * зарегистрированы при вызове определённых методов. Каждый элемент списка — это объект
 * {@link EventHolder}, содержащий детали конкретного события (например, тип, сообщение, уровень
 * важности).
 *
 * @param methodEventHolders список объектов {@link EventHolder}, описывающих события аудита
 * @see EventHolder
 */
public record MethodEventsHolder(List<EventHolder> methodEventHolders) {

}
