package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import static ru.sbrf.sbererp.core.common.unified.audit.properties.util.ValidateUtil.validate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.unified.audit.binder.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.unified.audit.extractor.Extractor;
import ru.sbrf.sbererp.core.common.unified.audit.extractor.RequestExtractor;
import ru.sbrf.sbererp.core.common.unified.audit.util.Constants;
import ru.sbrf.ufs.platform.audit.model.CriticalityEnum;

/**
 * Класс, представляющий информацию об событии аудита.
 * <p>
 * Содержит метаданные события: имя, описание, режим отправки, признак успешности, параметры и
 * условия, при которых событие должно быть зафиксировано. Используется для конфигурации
 * аудит-событий через внешние свойства (YAML/properties).
 *
 * @see ParamHolder
 * @see ConditionHolder
 * @see Extractor
 * @see AuditParameterBinder
 */
@Getter
@ToString
public class EventHolder {

  /**
   * Имя события аудита.
   */
  private final String name;

  /**
   * Описание события аудита.
   */
  private final String description;

  /**
   * Режим отправки события, определяющий его критичность.
   */
  private final CriticalityEnum mode;

  /**
   * Признак успешности события.
   */
  private final Boolean success;

  /**
   * Карта параметров события, сгруппированных по категориям извлечения.
   */
  private final Map<String, List<ParamHolder>> paramsMap;

  /**
   * Карта условий, при которых событие должно быть зафиксировано.
   */
  private final Map<String, List<ConditionHolder>> conditionsMap;

  /**
   * Скомпилированный проверочный функциональный объект для условий события.
   */
  private Function<ServerWebExchange, Boolean> compiledConditionChecker;

  /**
   * Конструктор класса.
   * <p>
   * Вызывается Spring Boot при привязке конфигурации.
   *
   * @param name        имя события, не должно быть пустым
   * @param description описание события, не должно быть пустым
   * @param mode        режим критичности, не должен быть null
   * @param success     признак успешности, не должен быть null
   * @param params      карта параметров, не должна быть null, пустой или содержать пустые списки
   * @param conditions  карта условий (может быть null или пустой)
   * @throws UnifiedAuditException если хотя бы одно обязательное поле не прошло валидацию
   */
  @ConstructorBinding
  public EventHolder(String name,
      String description,
      CriticalityEnum mode,
      Boolean success,
      Map<String, List<ParamHolder>> params,
      Map<String, List<ConditionHolder>> conditions) {
    validate(name, description, mode, success, params);
    this.name = name;
    this.description = description;
    this.mode = mode;
    this.success = success;
    this.paramsMap = params;
    this.conditionsMap = conditions;
  }

  /**
   * Публичный метод прекомпиляции условий. Вызывается постпроцессором после настройки
   * экстракторов.
   */
  public void postCompile() {
    if (Objects.nonNull(this.compiledConditionChecker)) {
      return;
    }
    this.compiledConditionChecker = compileConditions();
  }

  /**
   * Проверяет, заданы ли условия для события.
   *
   * @return {@code true}, если условия заданы и не пусты
   */
  public boolean hasConditions() {
    return ObjectUtils.isNotEmpty(conditionsMap);
  }

  /**
   * Проверяет, заданы ли параметры для события.
   *
   * @return {@code true}, если параметры заданы и не пусты
   */
  public boolean hasParams() {
    return ObjectUtils.isNotEmpty(paramsMap);
  }

  /**
   * Проверяет, соответствуют ли текущий обмен заданным условиям события.
   *
   * @param exchange текущий обмен
   * @return {@code true}, если все условия выполнены
   * @throws UnifiedAuditException если {@code compiledConditionChecker} не инициализирован
   */
  public boolean matchesConditions(ServerWebExchange exchange) {
    if (Objects.isNull(compiledConditionChecker)) {
      throw new UnifiedAuditException(Constants.COMPILED_CONDITION_CHECKER_IS_NULL, name);
    }
    return compiledConditionChecker.apply(exchange);
  }

  /**
   * Возвращает плоский список всех параметров события из всех категорий.
   *
   * @return неизменяемый список всех {@link ParamHolder}
   */
  public List<ParamHolder> getParams() {
    return hasParams()
        ? paramsMap.values().stream().flatMap(List::stream).toList()
        : List.of();
  }

  /**
   * Компилирует условия в одну функцию проверки обмена.
   *
   * @return функция, проверяющая все условия события
   */
  private Function<ServerWebExchange, Boolean> compileConditions() {
    if (!hasConditions()) {
      return EventHolder::alwaysMatch;
    }
    List<Function<ServerWebExchange, Boolean>> allCheckers =
        conditionsMap.values().stream()
            .flatMap(List::stream)
            .map(this::compileSingleCondition)
            .toList();
    return exchange -> matchAllCheckers(allCheckers, exchange);
  }

  /**
   * Компилирует одно условие в функцию проверки.
   *
   * @param condition условие для компиляции
   * @return функция, возвращающая {@code true}, если условие выполнено
   */
  private Function<ServerWebExchange, Boolean> compileSingleCondition(ConditionHolder condition) {
    return exchange -> evaluateCondition(condition, exchange);
  }

  /**
   * Извлекает значение условия и сравнивает его оператором.
   *
   * @param condition условие
   * @param exchange  текущий обмен
   * @return результат оператора
   */
  private boolean evaluateCondition(ConditionHolder condition, ServerWebExchange exchange) {
    String actualValue = extractValue(condition, exchange);
    return condition.getOperator().evaluate(actualValue, condition.getValues());
  }

  /**
   * Извлекает значение для условия с помощью соответствующего экстрактора.
   *
   * @param condition условие
   * @param exchange  текущий обмен
   * @return извлечённое значение или {@code null}
   */
  private String extractValue(ConditionHolder condition, ServerWebExchange exchange) {
    Extractor extractor = condition.getExtractor();
    if (Objects.isNull(extractor)) {
      throw new UnifiedAuditException(Constants.EXTRACTOR_IS_MISSING, condition.getName(), name);
    }
    try {
      if (extractor instanceof RequestExtractor) {
        return extractor.extractRequest(exchange, condition);
      }
      return extractor.extractResponse(exchange, condition);
    } catch (Exception exception) {
      return null;
    }
  }

  /**
   * Проверяет, что все скомпилированные условия выполнены.
   *
   * @param checkers список проверок
   * @param exchange текущий обмен
   * @return {@code true}, если все проверки вернули {@code true}
   */
  private static boolean matchAllCheckers(
      List<Function<ServerWebExchange, Boolean>> checkers,
      ServerWebExchange exchange) {
    return checkers.stream().allMatch(checker -> checker.apply(exchange));
  }

  /**
   * Функция «условия отсутствуют» — обмен всегда подходит.
   *
   * @param exchange текущий обмен
   * @return {@code true}
   */
  private static boolean alwaysMatch(ServerWebExchange exchange) {
    return true;
  }
}
