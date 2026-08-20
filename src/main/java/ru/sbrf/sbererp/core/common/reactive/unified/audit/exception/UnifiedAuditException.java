package ru.sbrf.sbererp.core.common.reactive.unified.audit.exception;

/**
 * Непроверяемое исключение конфигурации и разрешения событий аудита.
 *
 * <p>Текст формируется через {@link String#format(String, Object...)}.
 */
public final class UnifiedAuditException extends RuntimeException {

  /**
   * @param pattern шаблон {@link String#format(String, Object...)}
   * @param args    аргументы шаблона
   */
  public UnifiedAuditException(String pattern, Object... args) {
    super(String.format(pattern, args));
  }
}
