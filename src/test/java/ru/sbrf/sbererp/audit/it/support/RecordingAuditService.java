package ru.sbrf.sbererp.audit.it.support;

import com.sbt.audit.core.model.v2.event.Event;
import com.sbt.audit.core.model.v2.metamodel.Metamodel;
import com.sbt.audit.core.service.AuditService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Тестовый клиент аудита: запоминает события и зарегистрированные метамодели.
 */
@Component
@Primary
public class RecordingAuditService implements AuditService {

  private final List<Event> events = new CopyOnWriteArrayList<>();
  private final List<Metamodel> metamodels = new CopyOnWriteArrayList<>();
  private final AtomicInteger eventSequence = new AtomicInteger();

  /**
   * Очищает только события запросов. Метамодель старта приложения сохраняется.
   */
  public void clearEvents() {
    events.clear();
  }

  /**
   * @return снимок отправленных событий
   */
  public List<Event> events() {
    return List.copyOf(events);
  }

  /**
   * @return снимок зарегистрированных метамоделей
   */
  public List<Metamodel> metamodels() {
    return List.copyOf(metamodels);
  }

  /**
   * @param name имя события из YAML
   * @return первое событие с этим именем
   */
  public Optional<Event> findByName(String name) {
    return events.stream()
        .filter(event -> Objects.equals(name, event.getName()))
        .findFirst();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String audit(Event event) {
    Objects.requireNonNull(event, "event");
    events.add(event);
    return "event-" + eventSequence.incrementAndGet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> audit(List<Event> auditEvents) {
    if (ObjectUtils.isEmpty(auditEvents)) {
      return List.of();
    }
    return auditEvents.stream()
        .map(this::audit)
        .toList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String register(Metamodel metamodel) {
    Objects.requireNonNull(metamodel, "metamodel");
    metamodels.add(metamodel);
    return "metamodel-" + metamodels.size();
  }
}
