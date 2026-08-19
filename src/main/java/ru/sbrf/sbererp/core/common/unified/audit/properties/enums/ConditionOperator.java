package ru.sbrf.sbererp.core.common.unified.audit.properties.enums;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditLogMessages;

/**
 * Операторы YAML {@code conditions.*.operator}.
 * <p>
 * {@link #evaluate(String, List)} сравнивает строку, извлечённую экстрактором, со списком
 * ожидаемых значений. Числа парсятся как {@code double}, JSON-массивы — по текстовому представлению.
 */
public enum ConditionOperator {
  EQUALS((actual, expected) -> {
    boolean result = expected.size() == 1 && compareValues(actual, expected.getFirst()) == 0;
    logOperation("EQUALS", result, actual, expected);
    return result;
  }),

  NOT_EQUALS((actual, expected) -> {
    boolean result = expected.size() == 1 && compareValues(actual, expected.getFirst()) != 0;
    logOperation("NOT_EQUALS", result, actual, expected);
    return result;
  }),

  IN((actual, expected) -> {
    boolean result = expected.contains(actual);
    logOperation("IN", result, actual, expected);
    return result;
  }),

  NOT_IN((actual, expected) -> {
    boolean result = !expected.contains(actual);
    logOperation("NOT_IN", result, actual, expected);
    return result;
  }),

  MATCHES((actual, expected) -> {
    boolean result = expected.size() == 1 && Objects.nonNull(actual) && actual.matches(expected.getFirst());
    logOperation("MATCHES", result, actual, expected);
    return result;
  }),

  GREATER_THAN((actual, expected) -> {
    boolean result = expected.size() == 1 && compareValues(actual, expected.getFirst()) > 0;
    logOperation("GREATER_THAN", result, actual, expected);
    return result;
  }),

  LESS_THAN((actual, expected) -> {
    boolean result = expected.size() == 1 && compareValues(actual, expected.getFirst()) < 0;
    logOperation("LESS_THAN", result, actual, expected);
    return result;
  }),

  GREATER_OR_EQUAL((actual, expected) -> {
    boolean result = expected.size() == 1 && compareValues(actual, expected.getFirst()) >= 0;
    logOperation("GREATER_OR_EQUAL", result, actual, expected);
    return result;
  }),

  LESS_OR_EQUAL((actual, expected) -> {
    boolean result = expected.size() == 1 && compareValues(actual, expected.getFirst()) <= 0;
    logOperation("LESS_OR_EQUAL", result, actual, expected);
    return result;
  }),

  IS_NULL((actual, expected) -> {
    boolean result = Objects.isNull(actual);
    logOperation("IS_NULL", result, actual, expected);
    return result;
  }),

  IS_NOT_NULL((actual, expected) -> {
    boolean result = Objects.nonNull(actual);
    logOperation("IS_NOT_NULL", result, actual, expected);
    return result;
  }),

  STRING_IS_EMPTY((actual, expected) -> {
    boolean result = Objects.nonNull(actual) && actual.isEmpty();
    logOperation("STRING_IS_EMPTY", result, actual, expected);
    return result;
  }),

  STRING_IS_NOT_EMPTY((actual, expected) -> {
    boolean result = Objects.nonNull(actual) && !actual.isEmpty();
    logOperation("STRING_IS_NOT_EMPTY", result, actual, expected);
    return result;
  }),

  STRING_IS_BLANK((actual, expected) -> {
    boolean result = Objects.isNull(actual) || actual.isBlank();
    logOperation("STRING_IS_BLANK", result, actual, expected);
    return result;
  }),

  STRING_IS_NOT_BLANK((actual, expected) -> {
    boolean result = Objects.nonNull(actual) && !actual.isBlank();
    logOperation("STRING_IS_NOT_BLANK", result, actual, expected);
    return result;
  }),

  STRING_CONTAINS((actual, expected) -> {
    boolean result = containsAll(actual, expected);
    logOperation("STRING_CONTAINS", result, actual, expected);
    return result;
  }),

  STRING_CONTAINS_ANY((actual, expected) -> {
    boolean result = containsAny(actual, expected);
    logOperation("STRING_CONTAINS_ANY", result, actual, expected);
    return result;
  }),

  STRING_NOT_CONTAINS((actual, expected) -> {
    boolean result = containsNone(actual, expected);
    logOperation("STRING_NOT_CONTAINS", result, actual, expected);
    return result;
  }),

  STRING_STARTS_WITH((actual, expected) -> {
    boolean result = startsWithAll(actual, expected);
    logOperation("STRING_STARTS_WITH", result, actual, expected);
    return result;
  }),

  STRING_ENDS_WITH((actual, expected) -> {
    boolean result = endsWithAll(actual, expected);
    logOperation("STRING_ENDS_WITH", result, actual, expected);
    return result;
  }),

  COLL_IS_EMPTY((actual, expected) -> {
    boolean result = isJsonArrayEmpty(actual);
    logOperation("COLL_IS_EMPTY", result, actual, expected);
    return result;
  }),

  COLL_IS_NOT_EMPTY((actual, expected) -> {
    boolean result = isJsonArrayNotEmpty(actual);
    logOperation("COLL_IS_NOT_EMPTY", result, actual, expected);
    return result;
  }),

  COLL_SIZE_EQUALS((actual, expected) -> {
    boolean result = compareCollectionSize(actual, expected, 0);
    logOperation("COLL_SIZE_EQUALS", result, actual, expected);
    return result;
  }),

  COLL_SIZE_GREATER_THAN((actual, expected) -> {
    boolean result = compareCollectionSize(actual, expected, 1);
    logOperation("COLL_SIZE_GREATER_THAN", result, actual, expected);
    return result;
  }),

  COLL_SIZE_LESS_THAN((actual, expected) -> {
    boolean result = compareCollectionSize(actual, expected, -1);
    logOperation("COLL_SIZE_LESS_THAN", result, actual, expected);
    return result;
  }),

  COLL_SIZE_GREATER_OR_EQUAL((actual, expected) -> {
    boolean result = compareCollectionSize(actual, expected, 2);
    logOperation("COLL_SIZE_GREATER_OR_EQUAL", result, actual, expected);
    return result;
  }),

  COLL_SIZE_LESS_OR_EQUAL((actual, expected) -> {
    boolean result = compareCollectionSize(actual, expected, -2);
    logOperation("COLL_SIZE_LESS_OR_EQUAL", result, actual, expected);
    return result;
  });

  private static final Logger LOG = LoggerFactory.getLogger(ConditionOperator.class);
  private final BiFunction<String, List<String>, Boolean> evaluator;

  ConditionOperator(BiFunction<String, List<String>, Boolean> evaluator) {
    this.evaluator = evaluator;
  }

  /**
   * Сравнивает извлечённую строку со списком expected из YAML.
   *
   * @param actualValue    результат экстрактора; {@code null} допустим.
   * @param expectedValues значения из {@code conditions.*.values}; {@code null} → {@code false}.
   * @return {@code true}, если оператор выполняется
   */
  public boolean evaluate(String actualValue, List<String> expectedValues) {
    if (Objects.isNull(expectedValues)) {
      LOG.debug(AuditLogMessages.CONDITION_OPERATOR_NULL_EXPECTED, this.name());
      return false;
    }

    if (this.name().startsWith("COLL_SIZE_") && expectedValues.size() == 1) {
      try {
        Integer.parseInt(expectedValues.getFirst());
      } catch (NumberFormatException e) {
        LOG.debug(AuditLogMessages.CONDITION_OPERATOR_INVALID_INTEGER,
            this.name(), expectedValues.getFirst());
        return false;
      }
    }

    return this.evaluator.apply(actualValue, expectedValues);
  }

  /**
   * Проверяет, что строка содержит все ожидаемые фрагменты.
   *
   * @param actual   фактическое значение.
   * @param expected ожидаемые фрагменты.
   * @return {@code true}, если все фрагменты содержатся
   */
  private static boolean containsAll(String actual, List<String> expected) {
    if (Objects.isNull(actual) || expected.isEmpty()) {
      return false;
    }
    return expected.stream().allMatch(actual::contains);
  }

  /**
   * Проверяет, что строка содержит хотя бы один ожидаемый фрагмент.
   *
   * @param actual   фактическое значение.
   * @param expected ожидаемые фрагменты.
   * @return {@code true}, если найден хотя бы один фрагмент
   */
  private static boolean containsAny(String actual, List<String> expected) {
    if (Objects.isNull(actual) || expected.isEmpty()) {
      return false;
    }
    return expected.stream().anyMatch(actual::contains);
  }

  /**
   * Проверяет, что строка не содержит ни одного ожидаемого фрагмента.
   *
   * @param actual   фактическое значение.
   * @param expected ожидаемые фрагменты.
   * @return {@code true}, если ни один фрагмент не содержится
   */
  private static boolean containsNone(String actual, List<String> expected) {
    if (Objects.isNull(actual) || expected.isEmpty()) {
      return true;
    }
    return expected.stream().noneMatch(actual::contains);
  }

  /**
   * Проверяет, что строка начинается со всех ожидаемых префиксов.
   *
   * @param actual   фактическое значение.
   * @param expected ожидаемые префиксы.
   * @return {@code true}, если все префиксы совпали
   */
  private static boolean startsWithAll(String actual, List<String> expected) {
    if (Objects.isNull(actual) || expected.isEmpty()) {
      return false;
    }
    return expected.stream().allMatch(actual::startsWith);
  }

  /**
   * Проверяет, что строка заканчивается всеми ожидаемыми суффиксами.
   *
   * @param actual   фактическое значение.
   * @param expected ожидаемые суффиксы.
   * @return {@code true}, если все суффиксы совпали
   */
  private static boolean endsWithAll(String actual, List<String> expected) {
    if (Objects.isNull(actual) || expected.isEmpty()) {
      return false;
    }
    return expected.stream().allMatch(actual::endsWith);
  }

  /**
   * Проверяет, что значение — пустой JSON-массив.
   *
   * @param actual фактическое значение.
   * @return {@code true}, если массив пуст
   */
  private static boolean isJsonArrayEmpty(String actual) {
    if (Objects.isNull(actual)) {
      return false;
    }
    String trimmed = actual.trim();
    if (trimmed.equals("[]")) {
      return true;
    }
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
      return trimmed.substring(1, trimmed.length() - 1).trim().isEmpty();
    }
    return false;
  }

  /**
   * Проверяет, что значение — непустой JSON-массив.
   *
   * @param actual фактическое значение.
   * @return {@code true}, если массив содержит элементы
   */
  private static boolean isJsonArrayNotEmpty(String actual) {
    if (Objects.isNull(actual)) {
      return false;
    }
    String trimmed = actual.trim();
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
      return !trimmed.substring(1, trimmed.length() - 1).trim().isEmpty();
    }
    return false;
  }
  private static void logOperation(String operatorName, boolean result, String actual, List<String> expected) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(
          AuditLogMessages.CONDITION_OPERATOR_EVALUATED,
          operatorName,
          result,
          truncateForLog(actual),
          truncateForLog(Objects.isNull(expected) ? "null" : expected.toString())
      );
    }
  }

  /**
   * Обрезает строку для логирования если она слишком длинная.
   */
  private static String truncateForLog(String value) {
    if (Objects.isNull(value)) {
      return "null";
    }
    final int maxLogLength = 200;
    if (value.length() > maxLogLength) {
      return value.substring(0, maxLogLength) + "...[" + (value.length() - maxLogLength) + " more chars]";
    }
    return value;
  }

  /**
   * Сравнивает размер JSON-массива с ожидаемым значением.
   *
   * @param actual   фактическое значение.
   * @param expected список из одного ожидаемого размера.
   * @param mode     0 равно, 1 больше, -1 меньше, 2 больше или равно, -2 меньше или равно.
   * @return результат сравнения
   */
  private static boolean compareCollectionSize(String actual, List<String> expected, int mode) {
    if (Objects.isNull(actual) || expected.size() != 1) {
      return false;
    }
    try {
      int expectedSize = Integer.parseInt(expected.getFirst());
      int actualSize = getCollectionSize(actual);
      return switch (mode) {
        case 0 -> actualSize == expectedSize;
        case 1 -> actualSize > expectedSize;
        case -1 -> actualSize < expectedSize;
        case 2 -> actualSize >= expectedSize;
        case -2 -> actualSize <= expectedSize;
        default -> false;
      };
    } catch (NumberFormatException exception) {
      return false;
    }
  }
  private static int getCollectionSize(String value) {
    if (Objects.isNull(value)) {
      return -1;
    }

    String trimmed = value.trim();
    if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
      return -1;
    }

    String content = trimmed.substring(1, trimmed.length() - 1).trim();
    if (content.isEmpty()) {
      return 0;
    }

    return countJsonArrayElements(content);
  }

  /**
   * Подсчитывает количество элементов в строке JSON-массива.
   */
  private static int countJsonArrayElements(String jsonArrayContent) {
    int count = 0;
    int depth = 0;
    boolean inQuotes = false;
    char prevChar = 0;

    for (int i = 0; i < jsonArrayContent.length(); i++) {
      char c = jsonArrayContent.charAt(i);

      if (c == '"' && prevChar != '\\') {
        inQuotes = !inQuotes;
      }

      if (!inQuotes) {
        if (c == '[' || c == '{') {
          depth++;
        } else if (c == ']' || c == '}') {
          depth--;
        } else if (c == ',' && depth == 0) {
          count++;
        }
      }

      prevChar = c;
    }

    return count + 1;
  }

  /**
   * Сравнивает два значения как числа (если возможно), иначе — как строки.
   */
  private static int compareValues(String actual, String expected) {
    if (Objects.isNull(actual) || Objects.isNull(expected)) {
      return -1;
    }

    try {
      double actualNum = Double.parseDouble(actual);
      double expectedNum = Double.parseDouble(expected);
      return Double.compare(actualNum, expectedNum);
    } catch (NumberFormatException e) {
      return actual.compareTo(expected);
    }
  }
}
