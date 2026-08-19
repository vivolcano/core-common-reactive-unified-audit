package ru.sbrf.sbererp.core.common.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * Текстовые литералы и разделители, общие для модуля аудита.
 */
@UtilityClass
public final class AuditTextConstants {

  /** Пустая строка. */
  public static final String EMPTY_STRING = "";

  /** Пробел. */
  public static final String SPACE = " ";

  /** Запятая. */
  public static final String COMMA = ",";

  /** Запятая с пробелом. */
  public static final String COMMA_WITH_SPACE = ", ";

  /** Подчёркивание. */
  public static final String UNDERSCORE = "_";

  /** Слэш. */
  public static final String SLASH = "/";

  /** Подчёркивание как символ. */
  public static final char CHAR_UNDERSCORE = '_';

  /** Дефис как символ. */
  public static final char CHAR_DASH = '-';

  /** Литерал {@code null} в строковом представлении. */
  public static final String STRING_LITERAL_NULL = "null";
}
