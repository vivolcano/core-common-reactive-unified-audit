package ru.sbrf.sbererp.core.common.reactive.unified.audit.holder;

import static ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditPropertiesValidationUtils.validate;

import io.vavr.control.Option;
import io.vavr.control.Try;
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
import ru.sbrf.sbererp.core.common.reactive.unified.audit.binder.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.Extractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.RequestExtractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditConfigurationFieldNames;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditExceptionMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;
import ru.sbrf.ufs.platform.audit.model.CriticalityEnum;

/**
 * YAML-событие {@code audit.model.*.events.*}.
 *
 * @param name                     имя события.
 * @param description              описание для метамодели.
 * @param mode                     критичность SBT.
 * @param success                  {@code true} — HTTP 200–308, иначе {@code false}.
 * @param paramsMap                ключ биндера → список {@link ParamHolder}.
 * @param conditionsMap            те же ключи → список {@link ConditionHolder}; может быть {@code null}.
 * @param compiledConditionChecker кэш AND-проверки условий.
 * @see #postCompile()
 * @see AuditParameterBinder
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
   * Конструктор привязки YAML: ключи {@code params} и {@code conditions}.
   *
   * @param name        имя события.
   * @param description описание события.
   * @param mode        режим критичности.
   * @param success     признак успешности.
   * @param params      ключ биндера → список {@link ParamHolder}.
   * @param conditions  ключ биндера → список {@link ConditionHolder}; может быть {@code null}.
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
   * Компилирует AND-проверку условий после привязки экстракторов.
   */
  public void postCompile() {
    compiledConditionChecker.compareAndSet(null, compileConditions());
  }

  /**
   * Проверяет, заданы ли условия для события.
   *
   * @return {@code true}, если условия заданы.
   */
  public boolean hasConditions() {
    return ObjectUtils.isNotEmpty(conditionsMap);
  }

  /**
   * Проверяет, заданы ли параметры для события.
   *
   * @return {@code true}, если параметры заданы.
   */
  public boolean hasParams() {
    return ObjectUtils.isNotEmpty(paramsMap);
  }

  /**
   * Проверяет, соответствуют ли текущий обмен заданным условиям события.
   *
   * @param exchange текущий обмен.
   * @return {@code true}, если все условия выполнены.
   * @throws UnifiedAuditException если {@code compiledConditionChecker} не инициализирован.
   */
  public boolean matchesConditions(ServerWebExchange exchange) {
    return Option.of(compiledConditionChecker.get())
        .map(checker -> checker.apply(exchange))
        .getOrElseThrow(() -> new UnifiedAuditException(
            AuditExceptionMessages.COMPILED_CONDITION_CHECKER_IS_NULL, name));
  }

  /**
   * Возвращает плоский список всех параметров события.
   *
   * @return плоский список всех {@link ParamHolder}.
   */
  public List<ParamHolder> params() {
    return hasParams()
        ? paramsMap.values().stream().flatMap(List::stream).toList()
        : List.of();
  }

  /**
   * Компилирует условия в одну функцию проверки обмена.
   *
   * @return функция проверки всех условий события.
   */
  private Function<ServerWebExchange, Boolean> compileConditions() {
    if (!hasConditions()) {
      return EventHolder::alwaysMatch;
    }
    final List<Function<ServerWebExchange, Boolean>> allCheckers = conditionsMap.values().stream()
        .flatMap(List::stream)
        .map(this::compileSingleCondition)
        .toList();
    return exchange -> matchAllCheckers(allCheckers, exchange);
  }

  /**
   * Компилирует одно YAML-условие в функцию проверки обмена.
   *
   * @param condition условие события.
   * @return функция, возвращающая {@code true}, если условие выполнено.
   */
  private Function<ServerWebExchange, Boolean> compileSingleCondition(ConditionHolder condition) {
    return exchange -> evaluateCondition(condition, exchange);
  }

  /**
   * Извлекает значение условия и сравнивает его оператором.
   *
   * @param condition условие события.
   * @param exchange  текущий обмен.
   * @return результат оператора.
   */
  private boolean evaluateCondition(ConditionHolder condition, ServerWebExchange exchange) {
    return condition.operator().evaluate(extractValue(condition, exchange), condition.values());
  }

  /**
   * Извлекает значение для условия с помощью привязанного экстрактора.
   *
   * @param condition условие события.
   * @param exchange  текущий обмен.
   * @return извлечённое значение либо {@code null}.
   */
  private String extractValue(ConditionHolder condition, ServerWebExchange exchange) {
    final Extractor extractor = Option.of(condition.getExtractor())
        .getOrElseThrow(() -> new UnifiedAuditException(
            AuditExceptionMessages.EXTRACTOR_IS_MISSING, condition.getName(), name));
    return Try.of(() -> extractor instanceof RequestExtractor
            ? extractor.extractRequest(exchange, condition)
            : extractor.extractResponse(exchange, condition))
        .onFailure(exception -> log.debug(
            AuditLogMessages.FAILED_TO_EXTRACT_CONDITION, condition.getName(), name, exception))
        .getOrNull();
  }

  /**
   * Проверяет, что все скомпилированные условия выполнены.
   *
   * @param checkers список проверок.
   * @param exchange текущий обмен.
   * @return {@code true}, если все проверки вернули {@code true}.
   */
  private static boolean matchAllCheckers(
      List<Function<ServerWebExchange, Boolean>> checkers,
      ServerWebExchange exchange) {
    return checkers.stream().allMatch(checker -> checker.apply(exchange));
  }

  /**
   * Функция «условия отсутствуют»: обмен всегда подходит.
   *
   * @param exchange текущий обмен.
   * @return {@code true}.
   */
  private static boolean alwaysMatch(ServerWebExchange exchange) {
    return true;
  }
}
