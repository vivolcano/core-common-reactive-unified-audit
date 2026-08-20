package ru.sbrf.sbererp.core.common.reactive.unified.audit.holder;

import java.util.List;

/**
 * События одного метода контроллера.
 *
 * @param methodEventHolders список YAML-событий метода
 */
public record MethodEventsHolder(List<EventHolder> methodEventHolders) {
}
