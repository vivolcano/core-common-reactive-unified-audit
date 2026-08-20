package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

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

  /** Запятая как символ. */
  public static final char CHAR_COMMA = ',';

  /** Кавычка JSON-строки. */
  public static final char CHAR_QUOTE = '"';

  /** Обратный слэш JSON. */
  public static final char CHAR_BACKSLASH = '\\';

  /** Начало JSON-массива. */
  public static final char CHAR_ARRAY_START = '[';

  /** Конец JSON-массива. */
  public static final char CHAR_ARRAY_END = ']';

  /** Начало JSON-объекта. */
  public static final char CHAR_OBJECT_START = '{';

  /** Конец JSON-объекта. */
  public static final char CHAR_OBJECT_END = '}';

  /** Нулевой символ: «предыдущего символа ещё не было». */
  public static final char CHAR_NUL = '\0';

  /** Пустой JSON-массив. */
  public static final String EMPTY_JSON_ARRAY = "[]";

  /** Открывающая скобка JSON-массива. */
  public static final String JSON_ARRAY_START = "[";

  /** Закрывающая скобка JSON-массива. */
  public static final String JSON_ARRAY_END = "]";

  /** Regex-разделитель вложенности маски {@code a.b.c}. */
  public static final String JSON_PATH_DOT_SPLIT_REGEX = "\\.";

  /** Первый элемент массива в JSON Pointer. */
  public static final String JSON_POINTER_FIRST_INDEX = "0";

  /** Префикс имени операторов размера коллекции. */
  public static final String COLL_SIZE_OPERATOR_PREFIX = "COLL_SIZE_";

  /** Литерал {@code null} в строковом представлении. */
  public static final String STRING_LITERAL_NULL = "null";
}
