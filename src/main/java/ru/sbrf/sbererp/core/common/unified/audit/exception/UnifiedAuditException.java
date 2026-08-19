package ru.sbrf.sbererp.core.common.unified.audit.exception;

/**
 * Непроверяемое исключение конфигурации и разрешения событий аудита.
 * <p>
 * Текст формируется через {@link String#format(String, Object...)}. Не используется для ошибок
 * сети/клиента SBT — они логируются и глотаются в {@code AuditClientServiceImpl}.
 */
public final class UnifiedAuditException extends RuntimeException {

  /**
   * @param pattern шаблон {@link String#format(String, Object...)}.
   * @param args    аргументы шаблона; допускается пустой массив.
   */
  public UnifiedAuditException(String pattern, Object... args) {
    super(String.format(pattern, args));
  }
}
