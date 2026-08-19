package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.Name;
import ru.sbrf.sbererp.core.common.unified.audit.extractor.Extractor;
import ru.sbrf.sbererp.core.common.unified.audit.properties.enums.ConditionOperator;

/**
 * Класс, представляющий условие для фильтрации событий аудита.
 * <p>
 * Условие состоит из имени поля, оператора сравнения и списка ожидаемых значений.
 * Используется для определения, должен ли быть зарегистрирован аудит-событие
 * на основе данных из HTTP-запроса или ответа.
 * <p>
 * Пример использования в конфигурации:
 * <pre>
 * conditions:
 *   request-header:
 *     - field: X-User-Role
 *       operator: EQUALS
 *       values: [ADMIN]
 * </pre>
 *
 * @see Holder
 * @see ConditionOperator
 * @see Extractor
 */
@Getter
public class ConditionHolder implements Holder {

  /**
   * Имя поля, по которому проверяется условие (например, имя заголовка, параметра и т.д.).
   * <p>
   * Может быть задано через конфигурацию с использованием аннотации {@link Name @Name("field")}.
   */
  private final String name;

  /**
   * Оператор сравнения, определяющий логику проверки значения.
   * <p>
   * Например: EQUALS, CONTAINS, NOT_EQUALS и т.д.
   *
   * @see ConditionOperator
   */
  private final ConditionOperator operator;

  /**
   * Список значений, с которыми будет сравниваться извлечённое значение.
   * <p>
   * Если значение из запроса/ответа соответствует хотя бы одному из этих значений
   * (в зависимости от оператора), условие считается выполненным.
   * <p>
   * Список не может быть null — при передаче null устанавливается пустой список.
   */
  private final List<String> values;

  /**
   * Экстрактор, ответственный за извлечение значения из запроса или ответа.
   * <p>
   * Устанавливается при компиляции конфигурации. Должен быть задан до вызова проверки условия.
   *
   * @see Extractor
   */
  @Setter
  private Extractor extractor;

  /**
   * Ключ, используемый для извлечения значения (например, имя заголовка или параметра).
   * <p>
   * Если не задан явно, по умолчанию используется значение поля {@link #name}.
   */
  @Setter
  private String key;

  /**
   * Конструктор для создания условия.
   * <p>
   * Используется Spring Boot для привязки конфигурации из YAML/properties.
   *
   * @param name     имя поля условия (сопоставляется с полем "field" в конфигурации)
   * @param operator оператор сравнения
   * @param values   список ожидаемых значений (может быть null, в этом случае будет использован пустой список)
   */
  @ConstructorBinding
  public ConditionHolder(@Name("field") String name,
      ConditionOperator operator,
      List<String> values) {
    this.name = name;
    this.operator = operator;
    this.values = ObjectUtils.isEmpty(values) ? List.of() : List.copyOf(values);
  }
}
