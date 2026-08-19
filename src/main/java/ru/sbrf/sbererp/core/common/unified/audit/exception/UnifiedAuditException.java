package ru.sbrf.sbererp.core.common.unified.audit.exception;

/**
 * Исключение, используемое для обработки ошибок, связанных с процессом аудита.
 * <p>
 * Поддерживается два способа инициализации исключения:
 * <ul>
 *   <li>С использованием шаблона сообщения и переменных аргументов.</li>
 *   <li>С передачей готового текста сообщения.</li>
 * </ul>
 */
public class UnifiedAuditException extends RuntimeException {

  /**
   * Инициализирует исключение с форматом сообщения на основе переданного шаблона и аргументов.
   *
   * @param pattern шаблон сообщения об ошибке
   * @param args    аргументы для подстановки в шаблон
   */
  public UnifiedAuditException(String pattern, Object... args) {
    super(String.format(pattern, args));
  }

  /**
   * Инициализирует исключение с готовым текстом сообщения.
   *
   * @param message текст сообщения об ошибке
   */
  public UnifiedAuditException(String message) {
    super(message);
  }
}
