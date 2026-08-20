package ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ConditionOperatorTest {

  @Test
  void equalsComparesSingleExpectedValue() {
    assertThat(ConditionOperator.EQUALS.evaluate("42", List.of("42"))).isTrue();
    assertThat(ConditionOperator.EQUALS.evaluate("42", List.of("7"))).isFalse();
    assertThat(ConditionOperator.NOT_EQUALS.evaluate("42", List.of("7"))).isTrue();
  }

  @Test
  void inAndNotInUseExpectedList() {
    assertThat(ConditionOperator.IN.evaluate("b", List.of("a", "b"))).isTrue();
    assertThat(ConditionOperator.NOT_IN.evaluate("c", List.of("a", "b"))).isTrue();
  }

  @Test
  void matchesUsesRegex() {
    assertThat(ConditionOperator.MATCHES.evaluate("item-42", List.of("item-\\d+"))).isTrue();
    assertThat(ConditionOperator.MATCHES.evaluate("item", List.of("item-\\d+"))).isFalse();
  }

  @Test
  void numericComparisonsParseDoubles() {
    assertThat(ConditionOperator.GREATER_THAN.evaluate("10", List.of("3"))).isTrue();
    assertThat(ConditionOperator.LESS_OR_EQUAL.evaluate("3", List.of("3"))).isTrue();
  }

  @Test
  void nullAndBlankOperators() {
    assertThat(ConditionOperator.IS_NULL.evaluate(null, List.of())).isTrue();
    assertThat(ConditionOperator.IS_NOT_NULL.evaluate("x", List.of())).isTrue();
    assertThat(ConditionOperator.STRING_IS_BLANK.evaluate("  ", List.of())).isTrue();
    assertThat(ConditionOperator.STRING_IS_NOT_EMPTY.evaluate("x", List.of())).isTrue();
  }

  @Test
  void stringContainsOperators() {
    assertThat(ConditionOperator.STRING_CONTAINS.evaluate("hello-world", List.of("hello", "world"))).isTrue();
    assertThat(ConditionOperator.STRING_CONTAINS_ANY.evaluate("hello", List.of("zzz", "ell"))).isTrue();
    assertThat(ConditionOperator.STRING_STARTS_WITH.evaluate("prefix-value", List.of("prefix"))).isTrue();
    assertThat(ConditionOperator.STRING_ENDS_WITH.evaluate("value-suffix", List.of("suffix"))).isTrue();
  }

  @Test
  void collectionOperatorsParseJsonArrays() {
    assertThat(ConditionOperator.COLL_IS_EMPTY.evaluate("[]", List.of())).isTrue();
    assertThat(ConditionOperator.COLL_IS_NOT_EMPTY.evaluate("[1,2]", List.of())).isTrue();
    assertThat(ConditionOperator.COLL_SIZE_EQUALS.evaluate("[1,2,3]", List.of("3"))).isTrue();
    assertThat(ConditionOperator.COLL_SIZE_GREATER_THAN.evaluate("[1,2]", List.of("1"))).isTrue();
  }

  @Test
  void evaluateReturnsFalseForNullExpectedValues() {
    assertThat(ConditionOperator.EQUALS.evaluate("1", null)).isFalse();
  }
}
