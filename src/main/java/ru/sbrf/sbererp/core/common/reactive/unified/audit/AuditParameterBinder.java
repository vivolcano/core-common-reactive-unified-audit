package ru.sbrf.sbererp.core.common.reactive.unified.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestBody;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.Extractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.*;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditExceptionMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditTextConstants;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Сопоставляет ключ YAML ({@code request}, {@code response-header}, …) с {@link Extractor}.
 * <p>
 * {@link #configureAuditParameters} вызывается из
 * {@link ru.sbrf.sbererp.core.common.reactive.unified.audit.postprocessor.AuditEventBinderPostProcessor}
 * для каждого контроллера из {@code audit.model}. После привязки вызывает {@link EventHolder#postCompile()}.
 */
@Slf4j
public enum AuditParameterBinder {

  /**
   * Экстрактор для параметров, связанных с HTTP-запросом. Привязывает параметры к аннотированным
   * входным параметрам метода контроллера.
   * <p>
   * Поддерживает параметры, переданные через тело запроса (с аннотацией {@link RequestBody}), а
   * также именованные параметры. Если параметр не найден в сигнатуре метода, пытается привязать его
   * к телу запроса.
   */
  REQUEST {
    @Override
    void setExtractors(List<? extends Holder> holders, Map<String, Parameter> parametersMap) {
      boolean hasRequestBody = hasRequestBodyParameter(parametersMap);
      List<Holder> paramHoldersNotMethodParam = new ArrayList<>();
      for (Holder holder : holders) {
        String parameterFieldName = Objects.requireNonNullElse(holder.getKey(), holder.getName());
        if (parametersMap.containsKey(parameterFieldName)) {
          Parameter parameter = parametersMap.get(parameterFieldName);
          parametersMap.remove(parameterFieldName);
          RequestExtractor.setExtractor(parameter, holder);
        } else {
          paramHoldersNotMethodParam.add(holder);
        }
      }
      if (hasRequestBody) {
        for (Holder holderNotMethodParam : paramHoldersNotMethodParam) {
          RequestExtractor.setExtractor(holderNotMethodParam);
        }
      }
    }
  },

  /**
   * Экстрактор для HTTP-заголовков запроса. Назначает экстрактор
   * {@link RequestExtractor#REQUEST_HEADER} всем указанным параметрам.
   * <p>
   * Используется для извлечения значений из заголовков входящего HTTP-запроса.
   */
  REQUEST_HEADER {
    @Override
    void setExtractors(List<? extends Holder> holders) {
      for (Holder holder : holders) {
        holder.setExtractor(RequestExtractor.REQUEST_HEADER);
        if (Objects.isNull(holder.getKey())) {
          holder.setKey(holder.getName());
        }
      }
    }
  },

  /**
   * Экстрактор для JWT claims. Назначает экстрактор {@link RequestExtractor#CLAIM} всем указанным
   * параметрам.
   * <p>
   * Используется для извлечения значений из JWT токена.
   */
  CLAIMS {
    @Override
    void setExtractors(List<? extends Holder> holders) {
      for (Holder holder : holders) {
        holder.setExtractor(RequestExtractor.CLAIM);
        if (Objects.isNull(holder.getKey())) {
          holder.setKey(holder.getName());
        }
      }
    }
  },

  /**
   * Экстрактор для кода HTTP-ответа. Назначает экстрактор {@link ResponseExtractor#RESPONSE_CODE}
   * всем указанным параметрам.
   * <p>
   * Позволяет включить HTTP-статус ответа (например, 200, 404) в событие аудита.
   */
  RESPONSE_CODE {
    @Override
    void setExtractors(List<? extends Holder> holders) {
      for (Holder holder : holders) {
        holder.setExtractor(ResponseExtractor.RESPONSE_CODE);
      }
    }
  },

  /**
   * Экстрактор для HTTP-заголовков ответа. Назначает экстрактор
   * {@link ResponseExtractor#RESPONSE_HEADER} всем указанным параметрам.
   * <p>
   * Используется для извлечения значений из заголовков исходящего HTTP-ответа.
   */
  RESPONSE_HEADER {
    @Override
    void setExtractors(List<? extends Holder> holders) {
      for (Holder holder : holders) {
        holder.setExtractor(ResponseExtractor.RESPONSE_HEADER);
        if (Objects.isNull(holder.getKey())) {
          holder.setKey(holder.getName());
        }
      }
    }
  },

  /**
   * Экстрактор для тела HTTP-ответа. Назначает экстрактор {@link ResponseExtractor#RESPONSE_BODY}
   * всем указанным параметрам.
   * <p>
   * Позволяет включить содержимое тела ответа (например, JSON) в событие аудита.
   */
  RESPONSE_BODY {
    @Override
    void setExtractors(List<? extends Holder> holders) {
      for (Holder holder : holders) {
        holder.setExtractor(ResponseExtractor.RESPONSE_BODY);
      }
    }
  },

  /**
   * Экстрактор для переменных URI-пути. Назначает экстрактор
   * {@link RequestExtractor#PATH_VARIABLE} всем указанным параметрам.
   * <p>
   * Используется для извлечения значений из URI-шаблона (например, /users/{id}/orders/{orderId}).
   * В отличие от {@link #REQUEST}, не зависит от сигнатуры метода — может использоваться даже
   * если переменная пути не аннотирована как {@link org.springframework.web.bind.annotation.PathVariable}.
   * <p>
   * Значение извлекается из атрибута запроса
   * {@link org.springframework.web.reactive.HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE}.
   * <p>
   * Пример конфигурации в YAML:
   * <pre>
   * conditions:
   *   path-variable:
   *     - field: id
   *       operator: EQUALS
   *       values: ["123"]
   * </pre>
   *
   * @see RequestExtractor#PATH_VARIABLE
   * @see ConditionHolder
   */
  PATH_VARIABLE {
    @Override
    void setExtractors(List<? extends Holder> holders) {
      for (Holder holder : holders) {
        holder.setExtractor(RequestExtractor.PATH_VARIABLE);
        if (Objects.isNull(holder.getKey())) {
          holder.setKey(holder.getName());
        }
      }
    }
  };

  /**
   * Метод по умолчанию, вызываемый при попытке установить экстракторы с неподдерживаемым типом.
   * Переопределяется в конкретных элементах перечисления.
   *
   * @param holders       список {@link Holder}, для которых требуется установить экстракторы.
   * @param parametersMap мапа имя параметра метода → {@link Parameter}.
   * @throws UnifiedAuditException если операция не поддерживается.
   */
  void setExtractors(List<? extends Holder> holders, Map<String, Parameter> parametersMap) {
    throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTION_NOT_SUPPORTED);
  }

  /**
   * Метод по умолчанию, вызываемый при попытке установить экстракторы с неподдерживаемым типом.
   * Переопределяется в конкретных элементах перечисления.
   *
   * @param holders список {@link Holder}, для которых требуется установить экстракторы.
   * @throws UnifiedAuditException если операция не поддерживается.
   */
  void setExtractors(List<? extends Holder> holders) {
    throw new UnifiedAuditException(AuditExceptionMessages.EXTRACTION_NOT_SUPPORTED);
  }

  /**
   * Настраивает параметры аудита для указанного класса контроллера.
   * <p>
   * Рекурсивно обходит всю иерархию классов (интерфейсы, суперклассы, абстрактные классы) и
   * связывает параметры аудита с соответствующими экстракторами.
   *
   * @param classEventsHolder контейнер, хранящий информацию о событиях класса.
   * @param clazz             класс контроллера, для которого настраиваются параметры аудита.
   */
  public static void configureAuditParameters(ClassEventsHolder classEventsHolder, Class<?> clazz) {
    Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(clazz);
    for (Class<?> classInterface : interfaces) {
      processMethodParameters(classEventsHolder, classInterface);
    }

    Class<?> currentClass = clazz;
    while (Objects.nonNull(currentClass) && currentClass != Object.class) {
      processMethodParameters(classEventsHolder, currentClass);
      currentClass = currentClass.getSuperclass();
    }
    log.debug(AuditLogMessages.CONFIGURED_EXTRACTORS, clazz.getName());
  }

  /**
   * Обрабатывает методы класса: находит YAML-события и привязывает {@link Extractor}.
   *
   * @param classEventsHolder контейнер событий контроллера.
   * @param clazz             класс или интерфейс, чьи методы сопоставляются с YAML.
   */
  private static void processMethodParameters(ClassEventsHolder classEventsHolder, Class<?> clazz) {
    for (Method method : clazz.getDeclaredMethods()) {
      if (!classEventsHolder.eventsMap().containsKey(method.getName())) {
        continue;
      }
      List<EventHolder> eventHolders = classEventsHolder.eventsMap()
          .get(method.getName())
          .methodEventHolders();

      for (EventHolder eventHolder : eventHolders) {
        Map<String, Parameter> parametersMap = getParametersMap(method);
        bindParametersToExtractors(eventHolder, parametersMap);

        if (eventHolder.hasConditions()) {
          postCompile(eventHolder, parametersMap);
        }
      }
    }
  }

  /**
   * Выбирает подходящий экстрактор для каждого параметра события и устанавливает его.
   * <p>
   * Определяет тип параметра (запрос/ответ) и делегирует установку экстрактора соответствующему
   * элементу перечисления.
   *
   * @param event         описание события с параметрами {@link EventHolder}.
   * @param parametersMap мапа имя параметра метода → {@link Parameter}.
   */
  private static void bindParametersToExtractors(EventHolder event, Map<String, Parameter> parametersMap) {
    if (!event.hasParams()) {
      return;
    }

    Map<String, List<ParamHolder>> paramsMap = event.paramsMap();

    for (AuditParameterBinder binder : values()) {
      if (paramsMap.containsKey(binder.getParamsMapKey())) {
        List<ParamHolder> paramHolders = paramsMap.get(binder.getParamsMapKey());
        if (binder.equals(REQUEST)) {
          binder.setExtractors(paramHolders, parametersMap);
        } else {
          binder.setExtractors(paramHolders);
        }
      }
    }
  }

  /**
   * Обрабатывает условия, связанные с событием аудита.
   * <p>
   * Устанавливает экстракторы для условий (например, "если код ответа == 200") и выполняет
   * финальную компиляцию события.
   *
   * @param event         описание события с условиями {@link EventHolder}.
   * @param parametersMap мапа имя параметра метода → {@link Parameter}.
   */
  private static void postCompile(EventHolder event, Map<String, Parameter> parametersMap) {
    Map<String, List<ConditionHolder>> conditionsMap = event.conditionsMap();
    for (AuditParameterBinder binder : values()) {
      if (conditionsMap.containsKey(binder.getParamsMapKey())) {
        List<ConditionHolder> conditionHolders = conditionsMap.get(binder.getParamsMapKey());
        if (binder.equals(REQUEST)) {
          binder.setExtractors(conditionHolders, parametersMap);
        } else {
          binder.setExtractors(conditionHolders);
        }
      }
    }
    event.postCompile();
  }

  /**
   * Проверяет, есть ли среди параметров метода аннотация {@link RequestBody}.
   *
   * @param parametersMap мапа имя параметра метода → {@link Parameter}.
   * @return {@code true}, если среди них есть {@link RequestBody}.
   */
  private static boolean hasRequestBodyParameter(Map<String, Parameter> parametersMap) {
    for (Parameter parameter : parametersMap.values()) {
      if (parameter.isAnnotationPresent(RequestBody.class)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Собирает мапу имя параметра метода → {@link Parameter} через отражение.
   * <p>
   * Используется для сопоставления имён из YAML с реальными параметрами метода.
   *
   * @param method отражённый метод контроллера.
   * @return мапа имя параметра → {@link Parameter}.
   */
  private static Map<String, Parameter> getParametersMap(Method method) {
    Parameter[] parameters = method.getParameters();
    Map<String, Parameter> parametersMap = new HashMap<>();
    for (Parameter parameter : parameters) {
      parametersMap.put(parameter.getName(), parameter);
    }
    return parametersMap;
  }

  /**
   * Возвращает ключ для использования в хэш-мап параметров событий. Формируется из имени элемента
   * перечисления.
   * <p>
   * Например, {@code REQUEST_BODY} преобразуется в {@code request-body}.
   *
   * @return строковый ключ YAML ({@code request-body} для {@code REQUEST_BODY}).
   */
  public String getParamsMapKey() {
    return this.name()
        .toLowerCase(Locale.ROOT)
        .replace(AuditTextConstants.CHAR_UNDERSCORE, AuditTextConstants.CHAR_DASH);
  }
}
