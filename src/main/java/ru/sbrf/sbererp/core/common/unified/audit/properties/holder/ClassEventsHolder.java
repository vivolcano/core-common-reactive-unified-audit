package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import lombok.Getter;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.sbrf.sbererp.core.common.unified.audit.properties.util.ValidateUtil.validate;

/**
 * Класс, представляющий коллекцию событий аудита, связанных с конкретным контроллером.
 * Используется для хранения и организации событий по методам контроллера.
 */
@Getter
public class ClassEventsHolder {

  /**
   * Класс контроллера, к которому привязаны события аудита.
   */
  private final Class<?> controllerClass;

  /**
   * Карта событий, где ключ — имя метода контроллера, значение — объект {@link MethodEventsHolder}.
   */
  private final Map<String, MethodEventsHolder> eventsMap;

  /**
   * Конструктор класса.
   * <p>
   * Создаёт карту событий на основе yaml файла. Для каждого метода создаётся
   * соответствующий объект {@link MethodEventsHolder}.
   *
   * @param controllerClass класс контроллера
   * @param events          модель из application.yaml
   */
  @ConstructorBinding
  public ClassEventsHolder(Class<?> controllerClass, Map<String, List<EventHolder>> events) {
    validate(controllerClass, events);
    this.controllerClass = controllerClass;
    this.eventsMap = new HashMap<>();
    for (Map.Entry<String, List<EventHolder>> entry : events.entrySet()) {
      this.eventsMap.put(entry.getKey(), new MethodEventsHolder(entry.getValue()));
    }
  }

  /**
   * Возвращает плоский список всех событий аудита, связанных с методами контроллера для регистрации метамодели.
   *
   * @return список объектов {@link EventHolder}
   */
  public List<EventHolder> getClassEventHolderList() {
    List<EventHolder> auditEventProviders = new ArrayList<>();
    for (MethodEventsHolder methodEventsHolder : eventsMap.values()) {
      auditEventProviders.addAll(methodEventsHolder.methodEventHolders());
    }
    return auditEventProviders;
  }
}
