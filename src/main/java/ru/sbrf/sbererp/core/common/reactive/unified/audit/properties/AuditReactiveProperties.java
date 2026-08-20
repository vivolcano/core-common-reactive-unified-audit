package ru.sbrf.sbererp.core.common.reactive.unified.audit.properties;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.server.PathContainer;
import org.springframework.util.unit.DataSize;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditConfigurationFieldNames;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditHttpConstants;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditNumericConstants;

/**
 * YAML {@code audit.reactive}: лимит кэша тел и пути, которые фильтр аудита пропускает.
 *
 * @param maxBodySize         максимум тела для аудита; {@code null} → {@link #DEFAULT_MAX_BODY_SIZE}.
 * @param excludePathPatterns ant-style пути без аудита; {@code null} → {@link #DEFAULT_EXCLUDE_PATH_PATTERNS}.
 */
@ConfigurationProperties(prefix = AuditConfigurationFieldNames.AUDIT_REACTIVE_PREFIX)
public record AuditReactiveProperties(DataSize maxBodySize, List<String> excludePathPatterns) {

  /**
   * Значение лимита тела по умолчанию.
   */
  public static final DataSize DEFAULT_MAX_BODY_SIZE = DataSize.ofMegabytes(
      AuditNumericConstants.DEFAULT_MAX_BODY_SIZE_MEGABYTES
  );

  /**
   * Служебные пути, которые не аудируются и не копируют тела.
   */
  public static final List<String> DEFAULT_EXCLUDE_PATH_PATTERNS = List.of(
      AuditHttpConstants.ACTUATOR_ANT_PATTERN,
      AuditHttpConstants.SWAGGER_UI_HTML,
      AuditHttpConstants.SWAGGER_UI_ANT_PATTERN,
      AuditHttpConstants.V3_API_DOCS,
      AuditHttpConstants.V3_API_DOCS_ANT_PATTERN,
      AuditHttpConstants.WEBJARS_ANT_PATTERN,
      AuditHttpConstants.FAVICON_ICO
  );

  /**
   * Подставляет значения по умолчанию: лимит 1 МБ, стандартный exclude.
   * Пустой список {@code exclude-path-patterns} отключает исключения.
   *
   * @param maxBodySize         лимит тела.
   * @param excludePathPatterns шаблоны путей.
   */
  public AuditReactiveProperties {
    maxBodySize = Objects.isNull(maxBodySize) ? DEFAULT_MAX_BODY_SIZE : maxBodySize;
    excludePathPatterns = Objects.isNull(excludePathPatterns)
        ? DEFAULT_EXCLUDE_PATH_PATTERNS
        : List.copyOf(excludePathPatterns);
  }

  /**
   * Возвращает лимит кэша тел аудита в байтах.
   *
   * @return положительное число байт для кэша аудита.
   */
  public int maxBodyBytes() {
    long bytes = maxBodySize.toBytes();
    return bytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bytes;
  }

  /**
   * Собирает {@link PathPattern} один раз при создании фильтра.
   *
   * @return скомпилированные шаблоны exclude.
   */
  public List<PathPattern> compiledExcludePathPatterns() {
    PathPatternParser parser = PathPatternParser.defaultInstance;
    return excludePathPatterns.stream()
        .map(parser::parse)
        .toList();
  }

  /**
   * Проверяет, совпадает ли путь с любым из шаблонов exclude.
   *
   * @param path     путь внутри приложения.
   * @param patterns скомпилированные шаблоны {@link PathPattern}.
   * @return {@code true}, если путь исключён из аудита.
   */
  public static boolean matchesAny(PathContainer path, List<PathPattern> patterns) {
    return patterns.stream().anyMatch(pattern -> pattern.matches(path));
  }
}
