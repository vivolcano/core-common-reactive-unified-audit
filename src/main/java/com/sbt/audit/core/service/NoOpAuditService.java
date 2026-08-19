package com.sbt.audit.core.service;

import com.sbt.audit.core.model.v2.event.Event;
import com.sbt.audit.core.model.v2.metamodel.Metamodel;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Заглушка блокирующего клиента Единого аудита: не выполняет сеть, возвращает синтетические id.
 */
public class NoOpAuditService implements AuditService {

  /**
   * {@inheritDoc}
   */
  @Override
  public String audit(Event event) {
    Objects.requireNonNull(event, "event");
    return UUID.randomUUID().toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> audit(List<Event> events) {
    if (ObjectUtils.isEmpty(events)) {
      return List.of();
    }
    return events.stream()
        .map(this::audit)
        .toList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String register(Metamodel metamodel) {
    Objects.requireNonNull(metamodel, "metamodel");
    return UUID.randomUUID().toString();
  }
}
