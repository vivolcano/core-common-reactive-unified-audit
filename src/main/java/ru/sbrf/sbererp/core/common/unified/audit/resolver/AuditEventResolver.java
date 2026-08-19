package ru.sbrf.sbererp.core.common.unified.audit.resolver;

import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.unified.audit.adapter.EventAdapter;
import ru.sbrf.sbererp.core.common.unified.audit.binder.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ClassEventsHolder;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.EventHolder;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.MethodEventsHolder;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ParamHolder;
import ru.sbrf.sbererp.core.common.unified.audit.resolver.util.EventHeaderUtil;
import ru.sbrf.sbererp.core.common.unified.audit.resolver.util.SecurityContextUtil;
import ru.sbrf.sbererp.core.common.unified.audit.service.AuditClientService;
import ru.sbrf.sbererp.core.common.unified.audit.util.Constants;
import ru.sbrf.sbererp.core.common.unified.audit.util.LogMessage;

/**
 * Основной сервис разрешения и формирования аудит-событий.
 * <p>
 * Определяет, какое событие аудита должно быть отправлено на основе обработанного HTTP-запроса.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventResolver {

  /** Сервис аудита для отправки событий. */
  private final AuditClientService auditClientService;

  /** Конфигурация свойств событий аудита. */
  private final AuditEventsProperties config;

  /**
   * Основной метод аудита — вызывается после обработки запроса. Находит подходящее событие и
   * отправляет его в сервис аудита.
   *
   * @param exchange завершённый обмен
   * @return сигнал завершения отправки (пустой, если событие не требуется)
   */
  public Mono<Void> audit(ServerWebExchange exchange) {
    return SecurityContextUtil.loadTokenParamsMap()
        .map(params -> SecurityContextUtil.storeTokenParams(exchange, params))
        .flatMap(this::resolveAndSend);
  }

  /**
   * Разрешает событие по handler method и отправляет его, если конфигурация найдена.
   *
   * @param exchange обмен с сохранёнными claims
   * @return сигнал завершения
   */
  private Mono<Void> resolveAndSend(ServerWebExchange exchange) {
    return findHandlerMethod(exchange)
        .flatMap(handlerMethod -> findMethodEventsHolder(handlerMethod)
            .map(holder -> sendResolvedEvent(holder, exchange))
            .getOrElse(Mono::empty));
  }

  /**
   * Ищет {@link HandlerMethod} в атрибутах обмена после {@code DispatcherHandler}.
   *
   * @param exchange текущий обмен
   * @return handler method или пустой Mono
   */
  private Mono<HandlerMethod> findHandlerMethod(ServerWebExchange exchange) {
    Object handler = exchange.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
    if (handler instanceof HandlerMethod handlerMethod) {
      return Mono.just(handlerMethod);
    }
    return Mono.empty();
  }

  /**
   * Находит конфигурацию событий для метода контроллера.
   *
   * @param handlerMethod обработчик запроса
   * @return держатель событий метода
   */
  private Option<MethodEventsHolder> findMethodEventsHolder(HandlerMethod handlerMethod) {
    return Option.ofOptional(
        config.getClassEventsHolders().stream()
            .filter(classHolder -> matchesController(classHolder, handlerMethod))
            .map(classHolder -> classHolder.getEventsMap().get(handlerMethod.getMethod().getName()))
            .filter(Objects::nonNull)
            .findFirst()
    );
  }

  /**
   * Проверяет, что handler относится к сконфигурированному контроллеру и метод описан в YAML.
   *
   * @param classEventsHolder конфигурация контроллера
   * @param handlerMethod     обработчик
   * @return {@code true}, если контроллер и метод совпали
   */
  private boolean matchesController(ClassEventsHolder classEventsHolder, HandlerMethod handlerMethod) {
    return handlerMethod.getBeanType().isAssignableFrom(classEventsHolder.getControllerClass())
        && classEventsHolder.getEventsMap().containsKey(handlerMethod.getMethod().getName());
  }

  /**
   * Собирает адаптер события и отправляет его в клиент аудита.
   *
   * @param methodEventsHolder события метода
   * @param exchange           текущий обмен
   * @return сигнал завершения отправки
   */
  private Mono<Void> sendResolvedEvent(
      MethodEventsHolder methodEventsHolder,
      ServerWebExchange exchange) {
    EventHolder eventHolder = getEventHolder(methodEventsHolder, exchange);
    return auditClientService.sendEvent(createEvent(eventHolder, exchange));
  }

  /**
   * Создаёт объект события аудита на основе шаблона и данных обмена.
   *
   * @param eventHolder шаблон события
   * @param exchange    текущий обмен
   * @return адаптер события
   */
  private EventAdapter createEvent(EventHolder eventHolder, ServerWebExchange exchange) {
    Map<String, Object> headerParamsMap = SecurityContextUtil.getTokenParamsMap(exchange);
    EventAdapter eventAdapter = EventAdapter.builder()
        .userLogin(EventHeaderUtil.getUserLogin(headerParamsMap))
        .userName(EventHeaderUtil.getUserName(headerParamsMap))
        .requestId(EventHeaderUtil.getRequestId(exchange.getRequest()))
        .userNode(EventHeaderUtil.getUserNode(headerParamsMap))
        .session(EventHeaderUtil.getSession(headerParamsMap, exchange.getRequest()))
        .nodeId(EventHeaderUtil.getNodeId())
        .eventName(eventHolder.getName())
        .isSuccess(eventHolder.getSuccess())
        .build();
    addEventParams(eventAdapter, eventHolder, exchange);
    return eventAdapter;
  }

  /**
   * Добавляет параметры события через назначенные экстракторы.
   *
   * @param eventAdapter адаптер события
   * @param eventHolder  шаблон
   * @param exchange     текущий обмен
   */
  private void addEventParams(
      EventAdapter eventAdapter,
      EventHolder eventHolder,
      ServerWebExchange exchange) {
    Map<String, List<ParamHolder>> paramsMap = eventHolder.getParamsMap();
    java.util.Arrays.stream(AuditParameterBinder.values())
        .filter(binder -> paramsMap.containsKey(binder.getParamsMapKey()))
        .forEach(binder -> bindParams(eventAdapter, paramsMap.get(binder.getParamsMapKey()), binder, exchange));
  }

  /**
   * Извлекает значения параметров одного биндера и кладёт их в адаптер.
   *
   * @param eventAdapter адаптер
   * @param paramHolders параметры категории
   * @param binder       категория источника
   * @param exchange     текущий обмен
   */
  private void bindParams(
      EventAdapter eventAdapter,
      List<ParamHolder> paramHolders,
      AuditParameterBinder binder,
      ServerWebExchange exchange) {
    String name = binder.name();
    if (name.contains(Constants.REQUEST) || name.equalsIgnoreCase(Constants.CLAIMS)) {
      paramHolders.forEach(paramHolder -> addRequestParam(eventAdapter, paramHolder, exchange));
      return;
    }
    if (name.contains(Constants.RESPONSE)) {
      paramHolders.forEach(paramHolder -> addResponseParam(eventAdapter, paramHolder, exchange));
    }
  }

  /**
   * Добавляет параметр, извлечённый из запроса.
   *
   * @param eventAdapter адаптер
   * @param paramHolder  параметр
   * @param exchange     текущий обмен
   */
  private void addRequestParam(
      EventAdapter eventAdapter,
      ParamHolder paramHolder,
      ServerWebExchange exchange) {
    eventAdapter.addParam(paramHolder, paramHolder.getExtractor().extractRequest(exchange, paramHolder));
  }

  /**
   * Добавляет параметр, извлечённый из ответа.
   *
   * @param eventAdapter адаптер
   * @param paramHolder  параметр
   * @param exchange     текущий обмен
   */
  private void addResponseParam(
      EventAdapter eventAdapter,
      ParamHolder paramHolder,
      ServerWebExchange exchange) {
    eventAdapter.addParam(paramHolder, paramHolder.getExtractor().extractResponse(exchange, paramHolder));
  }

  /**
   * Выбирает конкретное событие на основе условий или статуса ответа.
   *
   * @param methodEventsHolder события метода
   * @param exchange           текущий обмен
   * @return подходящее событие
   */
  private EventHolder getEventHolder(MethodEventsHolder methodEventsHolder, ServerWebExchange exchange) {
    return methodEventsHolder.methodEventHolders().stream()
        .filter(EventHolder::hasConditions)
        .filter(eventHolder -> eventHolder.matchesConditions(exchange))
        .findFirst()
        .orElseGet(() -> eventBySuccess(methodEventsHolder, exchange));
  }

  /**
   * Выбирает событие без условий по признаку успешности HTTP-статуса.
   *
   * @param methodEventsHolder события метода
   * @param exchange           текущий обмен
   * @return событие с совпадающим {@code success}
   */
  private EventHolder eventBySuccess(MethodEventsHolder methodEventsHolder, ServerWebExchange exchange) {
    boolean isSuccess = isSuccessStatus(exchange.getResponse().getStatusCode());
    return methodEventsHolder.methodEventHolders().stream()
        .filter(eventHolder -> !eventHolder.hasConditions())
        .filter(eventHolder -> Objects.equals(eventHolder.getSuccess(), isSuccess))
        .findFirst()
        .orElseThrow(() -> new UnifiedAuditException(LogMessage.NOT_SUITABLE_EVENT));
  }

  /**
   * Считает статус успешным в диапазоне 200–308, как в блокирующей реализации.
   *
   * @param status код ответа; {@code null} трактуется как успех 200
   * @return {@code true}, если статус в диапазоне успеха
   */
  private boolean isSuccessStatus(HttpStatusCode status) {
    int value = Objects.isNull(status) ? HttpStatus.OK.value() : status.value();
    return value >= HttpStatus.OK.value() && value <= HttpStatus.PERMANENT_REDIRECT.value();
  }
}
