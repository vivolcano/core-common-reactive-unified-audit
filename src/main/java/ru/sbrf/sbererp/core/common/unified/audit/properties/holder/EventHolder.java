package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import static ru.sbrf.sbererp.core.common.unified.audit.utils.AuditPropertiesValidationUtils.validate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.unified.audit.binder.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.unified.audit.extractor.Extractor;
import ru.sbrf.sbererp.core.common.unified.audit.extractor.RequestExtractor;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditConfigurationFieldNames;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditExceptionMessages;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditLogMessages;
import ru.sbrf.ufs.platform.audit.model.CriticalityEnum;

/**
 * YAML-событие {@code audit.model.*.events.*}.
 * <p>
 * {@code params}/{@code conditions} биндятся из YAML; {@code compiledConditionChecker}
 * заполняется {@link #postCompile()} после {@link AuditParameterBinder}.
 *
 * @param name                     имя события.
 * @param description              описание для метамодели.
 * @param mode                     критичность SBT.
 * @param success                  {@code true} — HTTP 200–308, {@code false} — иначе.
 * @param paramsMap                параметры по ключу биндера ({@code REQUEST}, {@code RESPONSE}, …).
 * @param conditionsMap            условия по тем же ключам; может быть {@code null}.
 * @param compiledConditionChecker кэш AND-проверки условий.
 */
@Slf4j
public record EventHolder(
    String name,
    String description,
    CriticalityEnum mode,
    Boolean success,
    Map<String, List<ParamHolder>> paramsMap,
    Map<String, List<ConditionHolder>> conditionsMap,
    AtomicReference<Function<ServerWebExchange, Boolean>> compiledConditionChecker
) {

  /**
   * Конструктор привязки Spring Boot: YAML-ключи {@code params} и {@code conditions}.
   *
   * @param name        имя события, не должно быть пустым.
   * @param description описание события, не должно быть пустым.
   * @param mode        режим критичности, не должен быть null.
   * @param success     признак успешности, не должен быть null.
   * @param params      карта параметров, не должна быть null, пустой или содержать пустые списки.
   * @param conditions  карта условий (может быть null или пустой).
   * @throws UnifiedAuditException если хотя бы одно обязательное поле не прошло валидацию
   */
  @ConstructorBinding
  public EventHolder(
      String name,
      String description,
      CriticalityEnum mode,
      Boolean success,
      @Name(AuditConfigurationFieldNames.PARAMS) Map<String, List<ParamHolder>> params,
      @Name(AuditConfigurationFieldNames.CONDITIONS) Map<String, List<ConditionHolder>> conditions) {
    this(name, description, mode, success, params, conditions, new AtomicReference<>());
  }

  /**
   * Нормализует кэш проверки условий, если канонический конструктор вызван напрямую.
   */
  public EventHolder {
    validate(name, description, mode, success, paramsMap);
    compiledConditionChecker = Objects.requireNonNullElseGet(compiledConditionChecker, AtomicReference::new);
  }

  /**
   * Публичный метод прекомпиляции условий. Вызывается постпроцессором после настройки
   * экстракторов.
   */
  public void postCompile() {
    compiledConditionChecker.compareAndSet(null, compileConditions());
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
   * @param exchange текущий обмен.
   * @return {@code true}, если все условия выполнены
   * @throws UnifiedAuditException если {@code compiledConditionChecker} не инициализирован
   */
  public boolean matchesConditions(ServerWebExchange exchange) {
    Function<ServerWebExchange, Boolean> checker = compiledConditionChecker.get();
    if (Objects.isNull(checker)) {
      throw new UnifiedAuditException(AuditExceptionMessages.COMPILED_CONDITION_CHECKER_IS_NULL, name);
    }
    return checker.apply(exchange);
  }

  /**
   * Возвращает плоский список всех параметров события из всех категорий.
   *
   * @return неизменяемый список всех {@link ParamHolder}
   */
  public List<ParamHolder> params() {
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
   * @param condition условие для компиляции.
   * @return функция, возвращающая {@code true}, если условие выполнено
   */
  private Function<ServerWebExchange, Boolean> compileSingleCondition(ConditionHolder condition) {
    return exchange -> evaluateCondition(condition, exchange);
  }

  /**
   * Извлекает значение условия и сравнивает его оператором.
   *
   * @param condition условие.
   * @param exchange  текущий обмен.
   * @return результат оператора
   */
  private boolean evaluateCondition(ConditionHolder condition, ServerWebExchange exchange) {
    String actualValue = extractValue(condition, exchange);
    return condition.operator().evaluate(actualValue, condition.values());
  }

  /**
   * Извлекает значение для условия с помощью соответствующего экстрактора.
   *
   * @param condition условие.
   * @param exchange  текущий обмен.
   * @return извлечённое значение или {@code null}
   */
  private String extractValue(ConditionHolder condition, ServerWebExchange exchange) {
    Extractor extractor = condition.getExtractor();
    if (Objects.isNull(extractor)) {
      throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTOR_IS_MISSING, condition.getName(), name);
    }
    try {
      if (extractor instanceof RequestExtractor) {
        return extractor.extractRequest(exchange, condition);
      }
      return extractor.extractResponse(exchange, condition);
    } catch (Exception exception) {
      log.debug(AuditLogMessages.FAILED_TO_EXTRACT_CONDITION, condition.getName(), name, exception);
      return null;
    }
  }

  /**
   * Проверяет, что все скомпилированные условия выполнены.
   *
   * @param checkers список проверок.
   * @param exchange текущий обмен.
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
   * @param exchange текущий обмен.
   * @return {@code true}
   */
  private static boolean alwaysMatch(ServerWebExchange exchange) {
    return true;
  }
}
