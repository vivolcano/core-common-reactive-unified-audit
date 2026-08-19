package ru.sbrf.sbererp.core.common.unified.audit.properties;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.server.PathContainer;
import org.springframework.util.unit.DataSize;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * YAML {@code audit.reactive}: лимит кэша тел и пути, которые фильтр аудита пропускает.
 *
 * @param maxBodySize          максимум тела для аудита; {@code null} → {@link #DEFAULT_MAX_BODY_SIZE}.
 * @param excludePathPatterns  ant-style пути без аудита; {@code null} → {@link #DEFAULT_EXCLUDE_PATH_PATTERNS}.
 */
@ConfigurationProperties(prefix = "audit.reactive")
public record AuditReactiveProperties(DataSize maxBodySize, List<String> excludePathPatterns) {

  /**
   * Значение лимита тела по умолчанию.
   */
  public static final DataSize DEFAULT_MAX_BODY_SIZE = DataSize.ofMegabytes(1);

  /**
   * Служебные пути, которые не аудируются и не копируют тела.
   */
  public static final List<String> DEFAULT_EXCLUDE_PATH_PATTERNS = List.of(
      "/actuator/**",
      "/swagger-ui.html",
      "/swagger-ui/**",
      "/v3/api-docs",
      "/v3/api-docs/**",
      "/webjars/**",
      "/favicon.ico"
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
   * @return положительное число байт для кэша аудита
   */
  public int maxBodyBytes() {
    long bytes = maxBodySize.toBytes();
    return bytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bytes;
  }

  /**
   * Собирает {@link PathPattern} один раз при создании фильтра.
   *
   * @return скомпилированные шаблоны exclude
   */
  public List<PathPattern> compiledExcludePathPatterns() {
    PathPatternParser parser = PathPatternParser.defaultInstance;
    return excludePathPatterns.stream()
        .map(parser::parse)
        .toList();
  }

  /**
   * @param path путь внутри приложения.
   * @param patterns скомпилированные шаблоны.
   * @return {@code true}, если путь исключён из аудита
   */
  public static boolean matchesAny(PathContainer path, List<PathPattern> patterns) {
    return patterns.stream().anyMatch(pattern -> pattern.matches(path));
  }
}
