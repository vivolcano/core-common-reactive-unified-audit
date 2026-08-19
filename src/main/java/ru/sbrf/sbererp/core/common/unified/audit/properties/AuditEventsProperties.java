package ru.sbrf.sbererp.core.common.unified.audit.properties;

import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ClassEventsHolder;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.EventHolder;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditConfigurationFieldNames;

/**
 * YAML {@code audit.model}: привязка событий к FQCN контроллеров и именам методов.
 *
 * @param classEventsHolders список контроллеров; {@code null}/пусто нормализуется в {@link List#of()}.
 */
@ConfigurationProperties(prefix = AuditConfigurationFieldNames.AUDIT_MODEL_PREFIX)
public record AuditEventsProperties(List<ClassEventsHolder> classEventsHolders) {

  /**
   * Копирует список, чтобы бин свойств не делил мутабельную коллекцию с биндером.
   */
  public AuditEventsProperties {
    classEventsHolders = ObjectUtils.isEmpty(classEventsHolders)
        ? List.of()
        : List.copyOf(classEventsHolders);
  }

  /**
   * Плоский список всех {@link EventHolder} для {@code AuditService#register}.
   *
   * @return события всех контроллеров в порядке YAML
   */
  public List<EventHolder> metamodelEvents() {
    return classEventsHolders.stream()
        .map(ClassEventsHolder::classEventHolderList)
        .flatMap(List::stream)
        .toList();
  }
}
