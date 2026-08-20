package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * Шаблоны {@link String#format(String, Object...)} для
 * {@link ru.sbrf.sbererp.core.common.reactive.unified.audit.exception.UnifiedAuditException}.
 */
@UtilityClass
public final class AuditExceptionMessages {

  /** {@code %s} — имя события. */
  public static final String COMPILED_CONDITION_CHECKER_IS_NULL =
      "В событии с именем %s compiledConditionChecker == null.";

  /** {@code %s %s} — имя условия, имя события. */
  public static final String EXTRACTOR_IS_MISSING =
      "В событии %s в условии %s не задан экстрактор.";

  /** Биндер не знает ключ YAML-секции. */
  public static final String EXTRACTION_NOT_SUPPORTED = "Извлечение не поддерживается";

  /** {@code %s} — имя holder. */
  public static final String EXTRACTION_NOT_SUPPORTED_FOR = "Извлечение не поддерживается для: %s";

  /** {@code %s} — имя поля YAML. */
  public static final String FIELD_CAN_NOT_BE_EMPTY =
      "В yaml файле конфигурации поле %s не может быть пустым";

  /** {@code %s} — имя секции YAML. */
  public static final String EMPTY_FIELD_IN_SECTION =
      "В yaml файле конфигурации пустые поля в секции %s";

  /** Резолвер не нашёл событие под статус/условия. */
  public static final String NOT_SUITABLE_EVENT = "Нет подходящего события";
}
