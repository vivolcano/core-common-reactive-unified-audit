package ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.enums;

import io.vavr.control.Try;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditNumericConstants;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditTextConstants;

/**
 * Операторы YAML {@code conditions.*.operator}.
 *
 * <p>{@link #evaluate(String, List)} сравнивает строку экстрактора со списком ожидаемых значений.
 */
public enum ConditionOperator {

  /** Равно: фактическое значение совпадает с единственным ожидаемым. */
  EQUALS((actual, expected) -> {
    boolean result = expected.size() == AuditNumericConstants.ONE
        && compareValues(actual, expected.getFirst()) == AuditNumericConstants.ZERO;
    logOperation("EQUALS", result, actual, expected);
    return result;
  }),

  /** Не равно: фактическое значение не совпадает с единственным ожидаемым. */
  NOT_EQUALS((actual, expected) -> {
    boolean result = expected.size() == AuditNumericConstants.ONE
        && compareValues(actual, expected.getFirst()) != AuditNumericConstants.ZERO;
    logOperation("NOT_EQUALS", result, actual, expected);
    return result;
  }),

  /** Фактическое значение входит в список ожидаемых. */
  IN((actual, expected) -> {
    boolean result = expected.contains(actual);
    logOperation("IN", result, actual, expected);
    return result;
  }),

  /** Фактическое значение не входит в список ожидаемых. */
  NOT_IN((actual, expected) -> {
    boolean result = !expected.contains(actual);
    logOperation("NOT_IN", result, actual, expected);
    return result;
  }),

  /** Фактическое значение соответствует регулярному выражению. */
  MATCHES((actual, expected) -> {
    boolean result = expected.size() == AuditNumericConstants.ONE
        && Objects.nonNull(actual)
        && actual.matches(expected.getFirst());
    logOperation("MATCHES", result, actual, expected);
    return result;
  }),

  /** Фактическое значение больше единственного ожидаемого. */
  GREATER_THAN((actual, expected) -> {
    boolean result = expected.size() == AuditNumericConstants.ONE
        && compareValues(actual, expected.getFirst()) > AuditNumericConstants.ZERO;
    logOperation("GREATER_THAN", result, actual, expected);
    return result;
  }),

  /** Фактическое значение меньше единственного ожидаемого. */
  LESS_THAN((actual, expected) -> {
    boolean result = expected.size() == AuditNumericConstants.ONE
        && compareValues(actual, expected.getFirst()) < AuditNumericConstants.ZERO;
    logOperation("LESS_THAN", result, actual, expected);
    return result;
  }),

  /** Фактическое значение больше или равно единственному ожидаемому. */
  GREATER_OR_EQUAL((actual, expected) -> {
    boolean result = expected.size() == AuditNumericConstants.ONE
        && compareValues(actual, expected.getFirst()) >= AuditNumericConstants.ZERO;
    logOperation("GREATER_OR_EQUAL", result, actual, expected);
    return result;
  }),

  /** Фактическое значение меньше или равно единственному ожидаемому. */
  LESS_OR_EQUAL((actual, expected) -> {
    boolean result = expected.size() == AuditNumericConstants.ONE
        && compareValues(actual, expected.getFirst()) <= AuditNumericConstants.ZERO;
    logOperation("LESS_OR_EQUAL", result, actual, expected);
    return result;
  }),

  /** Фактическое значение равно {@code null}. */
  IS_NULL((actual, expected) -> {
    boolean result = Objects.isNull(actual);
    logOperation("IS_NULL", result, actual, expected);
    return result;
  }),

  /** Фактическое значение не {@code null}. */
  IS_NOT_NULL((actual, expected) -> {
    boolean result = Objects.nonNull(actual);
    logOperation("IS_NOT_NULL", result, actual, expected);
    return result;
  }),

  /** Строка пустая. */
  STRING_IS_EMPTY((actual, expected) -> {
    boolean result = Objects.nonNull(actual) && actual.isEmpty();
    logOperation("STRING_IS_EMPTY", result, actual, expected);
    return result;
  }),

  /** Строка непустая. */
  STRING_IS_NOT_EMPTY((actual, expected) -> {
    boolean result = Objects.nonNull(actual) && !actual.isEmpty();
    logOperation("STRING_IS_NOT_EMPTY", result, actual, expected);
    return result;
  }),

  /** Строка {@code null} или состоит из пробелов. */
  STRING_IS_BLANK((actual, expected) -> {
    boolean result = Objects.isNull(actual) || actual.isBlank();
    logOperation("STRING_IS_BLANK", result, actual, expected);
    return result;
  }),

  /** Строка непустая и не состоит из пробелов. */
  STRING_IS_NOT_BLANK((actual, expected) -> {
    boolean result = Objects.nonNull(actual) && !actual.isBlank();
    logOperation("STRING_IS_NOT_BLANK", result, actual, expected);
    return result;
  }),

  /** Строка содержит все ожидаемые фрагменты. */
  STRING_CONTAINS((actual, expected) -> {
    boolean result = containsAll(actual, expected);
    logOperation("STRING_CONTAINS", result, actual, expected);
    return result;
  }),

  /** Строка содержит хотя бы один ожидаемый фрагмент. */
  STRING_CONTAINS_ANY((actual, expected) -> {
    boolean result = containsAny(actual, expected);
    logOperation("STRING_CONTAINS_ANY", result, actual, expected);
    return result;
  }),

  /** Строка не содержит ни одного ожидаемого фрагмента. */
  STRING_NOT_CONTAINS((actual, expected) -> {
    boolean result = containsNone(actual, expected);
    logOperation("STRING_NOT_CONTAINS", result, actual, expected);
    return result;
  }),

  /** Строка начинается со всех ожидаемых префиксов. */
  STRING_STARTS_WITH((actual, expected) -> {
    boolean result = startsWithAll(actual, expected);
    logOperation("STRING_STARTS_WITH", result, actual, expected);
    return result;
  }),

  /** Строка заканчивается всеми ожидаемыми суффиксами. */
  STRING_ENDS_WITH((actual, expected) -> {
    boolean result = endsWithAll(actual, expected);
    logOperation("STRING_ENDS_WITH", result, actual, expected);
    return result;
  }),

  /** JSON-массив пуст. */
  COLL_IS_EMPTY((actual, expected) -> {
    boolean result = isJsonArrayEmpty(actual);
    logOperation("COLL_IS_EMPTY", result, actual, expected);
    return result;
  }),

  /** JSON-массив непустой. */
  COLL_IS_NOT_EMPTY((actual, expected) -> {
    boolean result = isJsonArrayNotEmpty(actual);
    logOperation("COLL_IS_NOT_EMPTY", result, actual, expected);
    return result;
  }),

  /** Размер JSON-массива равен ожидаемому. */
  COLL_SIZE_EQUALS((actual, expected) -> {
    boolean result = compareCollectionSize(actual, expected, AuditNumericConstants.ZERO);
    logOperation("COLL_SIZE_EQUALS", result, actual, expected);
    return result;
  }),

  /** Размер JSON-массива больше ожидаемого. */
  COLL_SIZE_GREATER_THAN((actual, expected) -> {
    boolean result = compareCollectionSize(actual, expected, AuditNumericConstants.ONE);
    logOperation("COLL_SIZE_GREATER_THAN", result, actual, expected);
    return result;
  }),

  /** Размер JSON-массива меньше ожидаемого. */
  COLL_SIZE_LESS_THAN((actual, expected) -> {
    boolean result = compareCollectionSize(actual, expected, AuditNumericConstants.MINUS_ONE);
    logOperation("COLL_SIZE_LESS_THAN", result, actual, expected);
    return result;
  }),

  /** Размер JSON-массива больше или равен ожидаемому. */
  COLL_SIZE_GREATER_OR_EQUAL((actual, expected) -> {
    boolean result = compareCollectionSize(actual, expected, AuditNumericConstants.TWO);
    logOperation("COLL_SIZE_GREATER_OR_EQUAL", result, actual, expected);
    return result;
  }),

  /** Размер JSON-массива меньше или равен ожидаемому. */
  COLL_SIZE_LESS_OR_EQUAL((actual, expected) -> {
    boolean result = compareCollectionSize(actual, expected, AuditNumericConstants.MINUS_TWO);
    logOperation("COLL_SIZE_LESS_OR_EQUAL", result, actual, expected);
    return result;
  });

  /** Логгер операторов условия. */
  private static final Logger LOG = LoggerFactory.getLogger(ConditionOperator.class);

  /** Функция сравнения фактического значения со списком ожидаемых. */
  private final BiFunction<String, List<String>, Boolean> evaluator;

  /**
   * @param evaluator фактическое значение → ожидаемые значения → результат
   */
  ConditionOperator(BiFunction<String, List<String>, Boolean> evaluator) {
    this.evaluator = evaluator;
  }

  /**
   * @param actualValue    результат экстрактора; {@code null} допустим
   * @param expectedValues значения из {@code conditions.*.values}; {@code null} → {@code false}
   * @return {@code true}, если оператор выполняется
   */
  public boolean evaluate(String actualValue, List<String> expectedValues) {
    if (Objects.isNull(expectedValues)) {
      LOG.debug(AuditLogMessages.CONDITION_OPERATOR_NULL_EXPECTED, this.name());
      return false;
    }
    if (this.name().startsWith(AuditTextConstants.COLL_SIZE_OPERATOR_PREFIX)
        && expectedValues.size() == AuditNumericConstants.ONE
        && Try.of(() -> Integer.parseInt(expectedValues.getFirst())).isFailure()) {
      LOG.debug(AuditLogMessages.CONDITION_OPERATOR_INVALID_INTEGER, this.name(), expectedValues.getFirst());
      return false;
    }
    return this.evaluator.apply(actualValue, expectedValues);
  }

  private static boolean containsAll(String actual, List<String> expected) {
    return Objects.nonNull(actual) && !expected.isEmpty() && expected.stream().allMatch(actual::contains);
  }

  private static boolean containsAny(String actual, List<String> expected) {
    return Objects.nonNull(actual) && !expected.isEmpty() && expected.stream().anyMatch(actual::contains);
  }

  private static boolean containsNone(String actual, List<String> expected) {
    return Objects.isNull(actual) || expected.isEmpty() || expected.stream().noneMatch(actual::contains);
  }

  private static boolean startsWithAll(String actual, List<String> expected) {
    return Objects.nonNull(actual) && !expected.isEmpty() && expected.stream().allMatch(actual::startsWith);
  }

  private static boolean endsWithAll(String actual, List<String> expected) {
    return Objects.nonNull(actual) && !expected.isEmpty() && expected.stream().allMatch(actual::endsWith);
  }

  private static boolean isJsonArrayEmpty(String actual) {
    if (Objects.isNull(actual)) {
      return false;
    }
    final String trimmed = actual.trim();
    return trimmed.equals(AuditTextConstants.EMPTY_JSON_ARRAY)
        || (trimmed.startsWith(AuditTextConstants.JSON_ARRAY_START)
            && trimmed.endsWith(AuditTextConstants.JSON_ARRAY_END)
            && jsonArrayInnerContent(trimmed).isEmpty());
  }

  private static boolean isJsonArrayNotEmpty(String actual) {
    if (Objects.isNull(actual)) {
      return false;
    }
    final String trimmed = actual.trim();
    return trimmed.startsWith(AuditTextConstants.JSON_ARRAY_START)
        && trimmed.endsWith(AuditTextConstants.JSON_ARRAY_END)
        && !jsonArrayInnerContent(trimmed).isEmpty();
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

  private static String truncateForLog(String value) {
    final int maxLogLength = 200;
    return Objects.isNull(value)
        ? "null"
        : value.length() > maxLogLength
            ? value.substring(0, maxLogLength) + "...[" + (value.length() - maxLogLength) + " more chars]"
            : value;
  }

  /**
   * @param mode 0 равно, 1 больше, -1 меньше, 2 больше или равно, -2 меньше или равно
   */
  private static boolean compareCollectionSize(String actual, List<String> expected, int mode) {
    if (Objects.isNull(actual) || expected.size() != AuditNumericConstants.ONE) {
      return false;
    }
    return Try.of(() -> {
          final int expectedSize = Integer.parseInt(expected.getFirst());
          final int actualSize = getCollectionSize(actual);
          return switch (mode) {
            case AuditNumericConstants.ZERO -> actualSize == expectedSize;
            case AuditNumericConstants.ONE -> actualSize > expectedSize;
            case AuditNumericConstants.MINUS_ONE -> actualSize < expectedSize;
            case AuditNumericConstants.TWO -> actualSize >= expectedSize;
            case AuditNumericConstants.MINUS_TWO -> actualSize <= expectedSize;
            default -> false;
          };
        })
        .getOrElse(false);
  }

  private static int getCollectionSize(String value) {
    if (Objects.isNull(value)) {
      return AuditNumericConstants.MINUS_ONE;
    }
    final String trimmed = value.trim();
    if (!trimmed.startsWith(AuditTextConstants.JSON_ARRAY_START)
        || !trimmed.endsWith(AuditTextConstants.JSON_ARRAY_END)) {
      return AuditNumericConstants.MINUS_ONE;
    }
    final String content = jsonArrayInnerContent(trimmed);
    return content.isEmpty() ? AuditNumericConstants.ZERO : countJsonArrayElements(content);
  }

  private static String jsonArrayInnerContent(String jsonArray) {
    return jsonArray.substring(AuditNumericConstants.ONE, jsonArray.length() - AuditNumericConstants.ONE).trim();
  }

  private static int countJsonArrayElements(String jsonArrayContent) {
    int count = AuditNumericConstants.ZERO;
    int depth = AuditNumericConstants.ZERO;
    boolean inQuotes = false;
    char prevChar = AuditTextConstants.CHAR_NUL;
    for (int i = AuditNumericConstants.ZERO; i < jsonArrayContent.length(); i++) {
      final char c = jsonArrayContent.charAt(i);
      if (c == AuditTextConstants.CHAR_QUOTE && prevChar != AuditTextConstants.CHAR_BACKSLASH) {
        inQuotes = !inQuotes;
      }
      if (!inQuotes) {
        if (c == AuditTextConstants.CHAR_ARRAY_START || c == AuditTextConstants.CHAR_OBJECT_START) {
          depth++;
        } else if (c == AuditTextConstants.CHAR_ARRAY_END || c == AuditTextConstants.CHAR_OBJECT_END) {
          depth--;
        } else if (c == AuditTextConstants.CHAR_COMMA && depth == AuditNumericConstants.ZERO) {
          count++;
        }
      }
      prevChar = c;
    }
    return count + AuditNumericConstants.ONE;
  }

  private static int compareValues(String actual, String expected) {
    if (Objects.isNull(actual) || Objects.isNull(expected)) {
      return AuditNumericConstants.MINUS_ONE;
    }
    return Try.of(() -> Double.compare(Double.parseDouble(actual), Double.parseDouble(expected)))
        .getOrElse(() -> actual.compareTo(expected));
  }
}
