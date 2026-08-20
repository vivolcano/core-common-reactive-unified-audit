package ru.sbrf.sbererp.core.common.reactive.unified.audit;

import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.MetaModelTitleHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditConfigurationFieldNames;

/**
 * YAML {@code audit.client}: свойства билдера SBT-клиента и заголовок метамодели.
 *
 * @param config    мапа ключ свойства → значение из {@code audit.client.config}; {@code null} → пустая мапа.
 * @param metaModel обязательный {@link MetaModelTitleHolder} из {@code audit.client.meta-model}.
 */
@ConfigurationProperties(prefix = AuditConfigurationFieldNames.AUDIT_CLIENT_PREFIX)
public record AuditClientProperties(Map<String, String> config, MetaModelTitleHolder metaModel) {

  /**
   * Нормализует мапу свойств клиента и проверяет наличие {@link MetaModelTitleHolder}.
   *
   * @param config    мапа ключ свойства → значение; пустая мапа, если значение не задано.
   * @param metaModel заголовок метамодели.
   */
  public AuditClientProperties {
    config = ObjectUtils.isEmpty(config) ? Map.of() : Map.copyOf(config);
    Objects.requireNonNull(metaModel, "metaModel");
  }
}
