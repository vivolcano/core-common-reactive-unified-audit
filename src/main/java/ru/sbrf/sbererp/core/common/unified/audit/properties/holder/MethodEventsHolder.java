package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import java.util.List;

/**
 * События одного метода контроллера.
 *
 * @param methodEventHolders список YAML-событий метода; может быть пустым.
 */
public record MethodEventsHolder(List<EventHolder> methodEventHolders) {
}
