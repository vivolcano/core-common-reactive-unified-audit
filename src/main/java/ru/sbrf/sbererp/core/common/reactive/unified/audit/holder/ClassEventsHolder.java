package ru.sbrf.sbererp.core.common.reactive.unified.audit.holder;

import static ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditPropertiesValidationUtils.validate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Модель событий аудита, привязанных к контроллеру.
 * <p>
 * Класс, а не record: конструктор YAML принимает {@code Map<String, List<EventHolder>>},
 * а хранит {@code Map<String, MethodEventsHolder>} — из-за erasure два конструктора record
 * не могут сосуществовать.
 */
public final class ClassEventsHolder {

  private final Class<?> controllerClass;
  private final Map<String, MethodEventsHolder> eventsMap;

  /**
   * Конструктор привязки YAML: ключ {@code events} преобразуется в {@link MethodEventsHolder}.
   *
   * @param controllerClass класс контроллера.
   * @param events          мапа имя метода контроллера → список {@link EventHolder} из YAML.
   */
  @ConstructorBinding
  public ClassEventsHolder(Class<?> controllerClass, Map<String, List<EventHolder>> events) {
    validate(controllerClass, events);
    this.controllerClass = controllerClass;
    Map<String, MethodEventsHolder> boundEvents = new HashMap<>();
    for (Map.Entry<String, List<EventHolder>> entry : events.entrySet()) {
      boundEvents.put(entry.getKey(), new MethodEventsHolder(entry.getValue()));
    }
    this.eventsMap = Map.copyOf(boundEvents);
  }

  /**
   * Возвращает класс контроллера.
   *
   * @return класс контроллера.
   */
  public Class<?> controllerClass() {
    return controllerClass;
  }

  /**
   * Возвращает мапу имя метода контроллера → {@link MethodEventsHolder}.
   *
   * @return неизменяемая мапа событий по именам методов.
   */
  public Map<String, MethodEventsHolder> eventsMap() {
    return eventsMap;
  }

  /**
   * Возвращает плоский список всех событий аудита контроллера для регистрации метамодели.
   *
   * @return список объектов {@link EventHolder}.
   */
  public List<EventHolder> classEventHolderList() {
    List<EventHolder> auditEventProviders = new ArrayList<>();
    for (MethodEventsHolder methodEventsHolder : eventsMap.values()) {
      auditEventProviders.addAll(methodEventsHolder.methodEventHolders());
    }
    return auditEventProviders;
  }
}
