package ru.sbrf.sbererp.core.common.reactive.unified.audit.holder;

import static ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditPropertiesValidationUtils.validate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * События аудита, привязанные к контроллеру.
 *
 * <p>Класс, а не record: YAML принимает {@code Map<String, List<EventHolder>}},
 * а хранит {@code Map<String, MethodEventsHolder>}.
 */
public final class ClassEventsHolder {

  private final Class<?> controllerClass;
  private final Map<String, MethodEventsHolder> eventsMap;

  /**
   * Конструктор привязки YAML: ключ {@code events} преобразуется в {@link MethodEventsHolder}.
   *
   * @param controllerClass класс контроллера.
   * @param events          имя метода контроллера → список {@link EventHolder}.
   */
  @ConstructorBinding
  public ClassEventsHolder(Class<?> controllerClass, Map<String, List<EventHolder>> events) {
    validate(controllerClass, events);
    this.controllerClass = controllerClass;
    this.eventsMap = events.entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(
            Map.Entry::getKey,
            entry -> new MethodEventsHolder(entry.getValue())
        ));
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
   * Возвращает имя метода контроллера → {@link MethodEventsHolder}.
   *
   * @return имя метода контроллера → {@link MethodEventsHolder}.
   */
  public Map<String, MethodEventsHolder> eventsMap() {
    return eventsMap;
  }

  /**
   * Возвращает плоский список событий контроллера для регистрации метамодели.
   *
   * @return плоский список событий контроллера для регистрации метамодели.
   */
  public List<EventHolder> classEventHolderList() {
    return eventsMap.values().stream()
        .map(MethodEventsHolder::methodEventHolders)
        .flatMap(List::stream)
        .toList();
  }
}
