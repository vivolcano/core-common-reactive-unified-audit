package ru.sbrf.sbererp.core.common.reactive.unified.audit.binder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestBody;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.Extractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.RequestExtractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.ResponseExtractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ClassEventsHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ConditionHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.EventHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.Holder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ParamHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditExceptionMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditTextConstants;

/**
 * Сопоставляет ключ YAML ({@code request}, {@code response-header}, …) с {@link Extractor}.
 * Вызывается из {@link ru.sbrf.sbererp.core.common.reactive.unified.audit.postprocessor.AuditEventBinderPostProcessor},
 * после привязки — {@link EventHolder#postCompile()}.
 */
@Slf4j
public enum AuditParameterBinder {

  /**
   * Параметры HTTP-запроса; не найденные в сигнатуре метода привязываются к {@link RequestBody}.
   */
  REQUEST {
    /**
     * {@inheritDoc}
     */
    @Override
    void setExtractors(List<? extends Holder> holders, Map<String, Parameter> parametersMap) {
      final boolean hasRequestBody = hasRequestBodyParameter(parametersMap);
      final List<Holder> unmatched = new ArrayList<>();
      holders.forEach(holder -> {
        final String fieldName = Objects.requireNonNullElse(holder.getKey(), holder.getName());
        final Parameter parameter = parametersMap.remove(fieldName);
        if (Objects.nonNull(parameter)) {
          RequestExtractor.setExtractor(parameter, holder);
        } else {
          unmatched.add(holder);
        }
      });
      if (hasRequestBody) {
        unmatched.forEach(RequestExtractor::setExtractor);
      }
    }
  },

  /**
   * Заголовки HTTP-запроса через {@link RequestExtractor#REQUEST_HEADER}.
   */
  REQUEST_HEADER {
    /**
     * {@inheritDoc}
     */
    @Override
    void setExtractors(List<? extends Holder> holders) {
      assignExtractor(holders, RequestExtractor.REQUEST_HEADER);
    }
  },

  /**
   * JWT-claims через {@link RequestExtractor#CLAIM}.
   */
  CLAIMS {
    /**
     * {@inheritDoc}
     */
    @Override
    void setExtractors(List<? extends Holder> holders) {
      assignExtractor(holders, RequestExtractor.CLAIM);
    }
  },

  /**
   * HTTP-статус через {@link ResponseExtractor#RESPONSE_CODE}.
   */
  RESPONSE_CODE {
    /**
     * {@inheritDoc}
     */
    @Override
    void setExtractors(List<? extends Holder> holders) {
      holders.forEach(holder -> holder.setExtractor(ResponseExtractor.RESPONSE_CODE));
    }
  },

  /**
   * Заголовки HTTP-ответа через {@link ResponseExtractor#RESPONSE_HEADER}.
   */
  RESPONSE_HEADER {
    /**
     * {@inheritDoc}
     */
    @Override
    void setExtractors(List<? extends Holder> holders) {
      assignExtractor(holders, ResponseExtractor.RESPONSE_HEADER);
    }
  },

  /**
   * Тело HTTP-ответа через {@link ResponseExtractor#RESPONSE_BODY}.
   */
  RESPONSE_BODY {
    /**
     * {@inheritDoc}
     */
    @Override
    void setExtractors(List<? extends Holder> holders) {
      holders.forEach(holder -> holder.setExtractor(ResponseExtractor.RESPONSE_BODY));
    }
  },

  /**
   * Переменные URI через {@link RequestExtractor#PATH_VARIABLE}, без зависимости от сигнатуры метода.
   *
   * @see RequestExtractor#PATH_VARIABLE
   * @see ConditionHolder
   */
  PATH_VARIABLE {
    /**
     * {@inheritDoc}
     */
    @Override
    void setExtractors(List<? extends Holder> holders) {
      assignExtractor(holders, RequestExtractor.PATH_VARIABLE);
    }
  };

  /**
   * Привязывает экстракторы с учётом сигнатуры метода контроллера.
   *
   * @param holders       держатели YAML.
   * @param parametersMap имя параметра метода → {@link Parameter}.
   * @throws UnifiedAuditException если биндер не принимает параметры метода.
   */
  void setExtractors(List<? extends Holder> holders, Map<String, Parameter> parametersMap) {
    throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTION_NOT_SUPPORTED);
  }

  /**
   * Привязывает экстракторы без сигнатуры метода контроллера.
   *
   * @param holders держатели YAML.
   * @throws UnifiedAuditException если биндеру нужны параметры метода.
   */
  void setExtractors(List<? extends Holder> holders) {
    throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTION_NOT_SUPPORTED);
  }

  /**
   * Обходит интерфейсы и суперклассы {@code clazz} и привязывает экстракторы к совпавшим методам YAML.
   *
   * @param classEventsHolder события контроллера.
   * @param clazz             тип бина контроллера.
   */
  public static void configureAuditParameters(ClassEventsHolder classEventsHolder, Class<?> clazz) {
    Arrays.stream(ClassUtils.getAllInterfacesForClass(clazz))
        .forEach(iface -> processMethodParameters(classEventsHolder, iface));
    Stream.<Class<?>>iterate(clazz, Objects::nonNull, type -> type.getSuperclass())
        .takeWhile(current -> !Objects.equals(current, Object.class))
        .forEach(current -> processMethodParameters(classEventsHolder, current));
    log.debug(AuditLogMessages.CONFIGURED_EXTRACTORS, clazz.getName());
  }

  /**
   * Привязывает экстракторы к YAML-событиям, имена которых совпали с методами {@code clazz}.
   *
   * @param classEventsHolder события контроллера.
   * @param clazz             сканируемый класс или интерфейс.
   */
  private static void processMethodParameters(ClassEventsHolder classEventsHolder, Class<?> clazz) {
    Arrays.stream(clazz.getDeclaredMethods())
        .filter(method -> classEventsHolder.eventsMap().containsKey(method.getName()))
        .forEach(method -> {
          final List<EventHolder> eventHolders = classEventsHolder.eventsMap()
              .get(method.getName())
              .methodEventHolders();
          eventHolders.forEach(eventHolder -> {
            final Map<String, Parameter> parametersMap = getParametersMap(method);
            bindParametersToExtractors(eventHolder, parametersMap);
            if (eventHolder.hasConditions()) {
              postCompile(eventHolder, parametersMap);
            }
          });
        });
  }

  /**
   * @param event         событие, чьи {@code params} получают экстракторы.
   * @param parametersMap имя параметра метода → {@link Parameter}.
   */
  private static void bindParametersToExtractors(EventHolder event, Map<String, Parameter> parametersMap) {
    if (!event.hasParams()) {
      return;
    }
    bindHolders(event.paramsMap(), parametersMap);
  }

  /**
   * Привязывает экстракторы условий и компилирует AND-проверку.
   *
   * @param event         событие, чьи {@code conditions} получают экстракторы.
   * @param parametersMap имя параметра метода → {@link Parameter}.
   */
  private static void postCompile(EventHolder event, Map<String, Parameter> parametersMap) {
    bindHolders(event.conditionsMap(), parametersMap);
    event.postCompile();
  }

  /**
   * Привязывает экстракторы ко всем категориям держателей YAML.
   *
   * @param holdersByKey  ключ биндера → список держателей.
   * @param parametersMap имя параметра метода → {@link Parameter}.
   */
  private static void bindHolders(
      Map<String, ? extends List<? extends Holder>> holdersByKey,
      Map<String, Parameter> parametersMap) {
    Arrays.stream(values())
        .filter(binder -> holdersByKey.containsKey(binder.getParamsMapKey()))
        .forEach(binder -> {
          final List<? extends Holder> holders = holdersByKey.get(binder.getParamsMapKey());
          if (Objects.equals(binder, REQUEST)) {
            binder.setExtractors(holders, parametersMap);
          } else {
            binder.setExtractors(holders);
          }
        });
  }

  /**
   * Назначает экстрактор и ключ каждому держателю.
   *
   * @param holders   держатели YAML.
   * @param extractor стратегия извлечения {@link Extractor}.
   */
  private static void assignExtractor(List<? extends Holder> holders, Extractor extractor) {
    holders.forEach(holder -> {
      holder.setExtractor(extractor);
      holder.setKey(Objects.requireNonNullElse(holder.getKey(), holder.getName()));
    });
  }

  /**
   * Проверяет, есть ли в сигнатуре параметр с {@link RequestBody}.
   *
   * @param parametersMap имя параметра метода → {@link Parameter}.
   * @return {@code true}, если есть параметр с {@link RequestBody}.
   */
  private static boolean hasRequestBodyParameter(Map<String, Parameter> parametersMap) {
    return parametersMap.values().stream().anyMatch(parameter -> parameter.isAnnotationPresent(RequestBody.class));
  }

  /**
   * Собирает имя параметра метода → {@link Parameter}.
   *
   * @param method отражённый метод контроллера.
   * @return имя параметра → {@link Parameter}.
   */
  private static Map<String, Parameter> getParametersMap(Method method) {
    return Arrays.stream(method.getParameters())
        .collect(Collectors.toMap(Parameter::getName, Function.identity(), (left, right) -> left, HashMap::new));
  }

  /**
   * Ключ YAML этого биндера, например {@code REQUEST_BODY} → {@code request-body}.
   *
   * @return имя enum в нижнем регистре через дефис.
   */
  public String getParamsMapKey() {
    return name()
        .toLowerCase(Locale.ROOT)
        .replace(AuditTextConstants.CHAR_UNDERSCORE, AuditTextConstants.CHAR_DASH);
  }
}
