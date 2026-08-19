package ru.sbrf.sbererp.core.common.unified.audit.properties;

import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ClassEventsHolder;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.EventHolder;

/**
 * Модель событий аудита, загружаемая из YAML по префиксу {@code audit.model}.
 *
 * @param classEventsHolders привязки событий к контроллерам
 */
@ConfigurationProperties(prefix = "audit.model")
public record AuditEventsProperties(List<ClassEventsHolder> classEventsHolders) {

  /**
   * Нормализует список держателей событий.
   *
   * @param classEventsHolders список контроллеров с событиями
   */
  public AuditEventsProperties {
    classEventsHolders = ObjectUtils.isEmpty(classEventsHolders)
        ? List.of()
        : List.copyOf(classEventsHolders);
  }

  /**
   * Возвращает плоский список всех событий для регистрации метамодели.
   *
   * @return события всех контроллеров
   */
  public List<EventHolder> getMetamodelEvents() {
    return classEventsHolders.stream()
        .map(ClassEventsHolder::getClassEventHolderList)
        .flatMap(List::stream)
        .toList();
  }

  /**
   * Возвращает привязки событий к контроллерам.
   *
   * @return список {@link ClassEventsHolder}
   */
  public List<ClassEventsHolder> getClassEventsHolders() {
    return classEventsHolders;
  }
}
