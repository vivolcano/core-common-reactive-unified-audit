package ru.sbrf.sbererp.core.common.reactive.unified.audit.properties;

import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.MetaModelTitleHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditConfigurationFieldNames;

/**
 * YAML {@code audit.client}: свойства билдера SBT-клиента и заголовок метамодели.
 *
 * @param config    ключ свойства → значение из {@code audit.client.config}
 * @param metaModel обязательный {@link MetaModelTitleHolder}
 */
@ConfigurationProperties(prefix = AuditConfigurationFieldNames.AUDIT_CLIENT_PREFIX)
public record AuditClientProperties(Map<String, String> config, MetaModelTitleHolder metaModel) {

  /**
   * Нормализует свойства клиента и проверяет наличие {@link MetaModelTitleHolder}.
   */
  public AuditClientProperties {
    config = ObjectUtils.isEmpty(config) ? Map.of() : Map.copyOf(config);
    Objects.requireNonNull(metaModel, "metaModel");
  }
}
