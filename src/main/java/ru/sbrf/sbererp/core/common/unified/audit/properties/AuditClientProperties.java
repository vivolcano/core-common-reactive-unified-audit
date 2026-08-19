package ru.sbrf.sbererp.core.common.unified.audit.properties;

import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.MetaModelTitleHolder;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditConfigurationFieldNames;

/**
 * YAML {@code audit.client}: map для билдера SBT-клиента и заголовок метамодели.
 *
 * @param config    свойства {@code audit.client.config}; {@code null} → пустая карта.
 * @param metaModel обязательный {@code audit.client.meta-model}.
 */
@ConfigurationProperties(prefix = AuditConfigurationFieldNames.AUDIT_CLIENT_PREFIX)
public record AuditClientProperties(Map<String, String> config, MetaModelTitleHolder metaModel) {

  /**
   * Нормализует карту свойств и проверяет наличие заголовка метамодели.
   *
   * @param config    свойства клиента; пустая карта, если значение не задано.
   * @param metaModel заголовок метамодели.
   */
  public AuditClientProperties {
    config = ObjectUtils.isEmpty(config) ? Map.of() : Map.copyOf(config);
    Objects.requireNonNull(metaModel, "metaModel");
  }
}
