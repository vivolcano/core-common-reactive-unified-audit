package ru.sbrf.sbererp.core.common.unified.audit.properties;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * YAML {@code audit.reactive}: лимит буфера тел для {@code DataBufferUtils.join}.
 *
 * @param maxBodySize максимум тела; {@code null} → {@link #DEFAULT_MAX_BODY_SIZE}.
 */
@ConfigurationProperties(prefix = "audit.reactive")
public record AuditReactiveProperties(DataSize maxBodySize) {

  /**
   * Значение лимита тела по умолчанию.
   */
  public static final DataSize DEFAULT_MAX_BODY_SIZE = DataSize.ofMegabytes(1);

  /**
   * Подставляет значение по умолчанию, если лимит не задан.
   *
   * @param maxBodySize лимит тела.
   */
  public AuditReactiveProperties {
    maxBodySize = Objects.isNull(maxBodySize) ? DEFAULT_MAX_BODY_SIZE : maxBodySize;
  }

  /**
   * Возвращает лимит тела в байтах для {@code DataBufferUtils.join}.
   *
   * @return положительное число байт
   */
  public int maxBodyBytes() {
    long bytes = maxBodySize.toBytes();
    return bytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bytes;
  }
}
