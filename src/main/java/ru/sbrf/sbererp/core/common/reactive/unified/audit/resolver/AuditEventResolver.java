package ru.sbrf.sbererp.core.common.reactive.unified.audit.resolver;

import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.Arrays;
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
import ru.sbrf.sbererp.core.common.reactive.unified.audit.adapter.EventAdapter;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.binder.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ClassEventsHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.EventHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.MethodEventsHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ParamHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.service.AuditClientService;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditEventHeaderUtils;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditExceptionMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.ReactiveSecurityContextUtils;

/**
 * Разрешает YAML-событие по {@link HandlerMethod} и HTTP-статусу и собирает {@link EventAdapter}.
 *
 * @see ru.sbrf.sbererp.core.common.reactive.unified.audit.filter.AuditWebFilter
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class AuditEventResolver {

  private final AuditClientService auditClientService;
  private final AuditEventsProperties config;

  /**
   * Находит подходящее событие и отправляет его в клиент аудита.
   *
   * @param exchange завершённый обмен
   * @return сигнал завершения, пустой если событие не требуется
   */
  public Mono<Void> audit(ServerWebExchange exchange) {
    return ReactiveSecurityContextUtils.loadTokenParamsMap()
        .map(params -> ReactiveSecurityContextUtils.storeTokenParams(exchange, params))
        .flatMap(this::resolveAndSend);
  }

  /**
   * Разрешает событие по {@link HandlerMethod} и отправляет его, если YAML-маппинг найден.
   *
   * @param exchange обмен с сохранёнными JWT-claims
   * @return сигнал завершения
   */
  private Mono<Void> resolveAndSend(ServerWebExchange exchange) {
    return findHandlerMethod(exchange)
        .flatMap(handlerMethod -> findMethodEventsHolder(handlerMethod)
            .map(holder -> sendResolvedEvent(holder, handlerMethod, exchange))
            .getOrElse(Mono::empty));
  }

  /**
   * Ищет {@link HandlerMethod} в атрибутах обмена после {@link org.springframework.web.reactive.DispatcherHandler}.
   *
   * @param exchange текущий обмен
   * @return handler method либо пустой {@link Mono}
   */
  private Mono<HandlerMethod> findHandlerMethod(ServerWebExchange exchange) {
    return Option.of(exchange.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
        .filter(HandlerMethod.class::isInstance)
        .map(HandlerMethod.class::cast)
        .map(Mono::just)
        .getOrElse(() -> {
          log.debug(
              AuditLogMessages.NO_HANDLER_METHOD,
              exchange.getRequest().getMethod(),
              exchange.getRequest().getPath()
          );
          return Mono.empty();
        });
  }

  /**
   * Находит YAML-конфигурацию событий для метода контроллера.
   *
   * @param handlerMethod обработчик запроса
   * @return держатель событий метода
   */
  private Option<MethodEventsHolder> findMethodEventsHolder(HandlerMethod handlerMethod) {
    return Option.ofOptional(
            config.classEventsHolders().stream()
                .filter(classHolder -> matchesController(classHolder, handlerMethod))
                .map(classHolder -> classHolder.eventsMap().get(handlerMethod.getMethod().getName()))
                .filter(Objects::nonNull)
                .findFirst()
        )
        .onEmpty(() -> log.debug(
            AuditLogMessages.NO_AUDIT_MAPPING,
            handlerMethod.getBeanType().getName(),
            handlerMethod.getMethod().getName()
        ));
  }

  /**
   * Проверяет, что handler относится к сконфигурированному контроллеру и метод описан в YAML.
   *
   * @param classEventsHolder конфигурация контроллера
   * @param handlerMethod     обработчик
   * @return {@code true}, если контроллер и метод совпали
   */
  private boolean matchesController(ClassEventsHolder classEventsHolder, HandlerMethod handlerMethod) {
    return handlerMethod.getBeanType().isAssignableFrom(classEventsHolder.controllerClass())
        && classEventsHolder.eventsMap().containsKey(handlerMethod.getMethod().getName());
  }

  /**
   * Собирает адаптер события и отправляет его в клиент аудита.
   *
   * @param methodEventsHolder события метода
   * @param handlerMethod      найденный handler
   * @param exchange           текущий обмен
   * @return сигнал завершения отправки
   */
  private Mono<Void> sendResolvedEvent(
      MethodEventsHolder methodEventsHolder,
      HandlerMethod handlerMethod,
      ServerWebExchange exchange) {
    return Try.of(() -> {
          final EventHolder eventHolder = getEventHolder(methodEventsHolder, exchange);
          log.debug(
              AuditLogMessages.RESOLVED_AUDIT_EVENT,
              eventHolder.name(),
              handlerMethod.getBeanType().getName(),
              handlerMethod.getMethod().getName()
          );
          return auditClientService.sendEvent(createEvent(eventHolder, exchange));
        })
        .recover(UnifiedAuditException.class, exception -> {
          log.warn(
              AuditLogMessages.NO_MATCHING_AUDIT_EVENT,
              handlerMethod.getBeanType().getName(),
              handlerMethod.getMethod().getName(),
              exception
          );
          return Mono.empty();
        })
        .get();
  }

  /**
   * Создаёт внутреннее событие из шаблона YAML и данных обмена.
   *
   * @param eventHolder шаблон события
   * @param exchange    текущий обмен
   * @return адаптер события
   */
  private EventAdapter createEvent(EventHolder eventHolder, ServerWebExchange exchange) {
    final Map<String, Object> headerParamsMap = ReactiveSecurityContextUtils.getTokenParamsMap(exchange);
    final EventAdapter eventAdapter = EventAdapter.builder()
        .userLogin(AuditEventHeaderUtils.getUserLogin(headerParamsMap))
        .userName(AuditEventHeaderUtils.getUserName(headerParamsMap))
        .requestId(AuditEventHeaderUtils.getRequestId(exchange.getRequest()))
        .userNode(AuditEventHeaderUtils.getUserNode(headerParamsMap))
        .session(AuditEventHeaderUtils.getSession(headerParamsMap, exchange.getRequest()))
        .nodeId(AuditEventHeaderUtils.getNodeId())
        .eventName(eventHolder.name())
        .isSuccess(eventHolder.success())
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
    final Map<String, List<ParamHolder>> paramsMap = eventHolder.paramsMap();
    Arrays.stream(AuditParameterBinder.values())
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
    switch (binder) {
      case REQUEST, REQUEST_HEADER, CLAIMS, PATH_VARIABLE ->
          paramHolders.forEach(paramHolder -> addRequestParam(eventAdapter, paramHolder, exchange));
      case RESPONSE_CODE, RESPONSE_HEADER, RESPONSE_BODY ->
          paramHolders.forEach(paramHolder -> addResponseParam(eventAdapter, paramHolder, exchange));
    }
  }

  private void addRequestParam(
      EventAdapter eventAdapter,
      ParamHolder paramHolder,
      ServerWebExchange exchange) {
    eventAdapter.addParam(paramHolder, paramHolder.getExtractor().extractRequest(exchange, paramHolder));
  }

  private void addResponseParam(
      EventAdapter eventAdapter,
      ParamHolder paramHolder,
      ServerWebExchange exchange) {
    eventAdapter.addParam(paramHolder, paramHolder.getExtractor().extractResponse(exchange, paramHolder));
  }

  /**
   * Выбирает событие по условиям либо по HTTP-статусу.
   *
   * @param methodEventsHolder события метода
   * @param exchange           текущий обмен
   * @return подходящее событие
   */
  private EventHolder getEventHolder(MethodEventsHolder methodEventsHolder, ServerWebExchange exchange) {
    return Option.ofOptional(
            methodEventsHolder.methodEventHolders().stream()
                .filter(EventHolder::hasConditions)
                .filter(eventHolder -> eventHolder.matchesConditions(exchange))
                .findFirst()
        )
        .getOrElse(() -> eventBySuccess(methodEventsHolder, exchange));
  }

  /**
   * Выбирает событие без условий по признаку успешности HTTP-статуса.
   *
   * @param methodEventsHolder события метода
   * @param exchange           текущий обмен
   * @return событие с совпадающим {@code success}
   */
  private EventHolder eventBySuccess(MethodEventsHolder methodEventsHolder, ServerWebExchange exchange) {
    final boolean isSuccess = isSuccessStatus(exchange.getResponse().getStatusCode());
    return methodEventsHolder.methodEventHolders().stream()
        .filter(eventHolder -> !eventHolder.hasConditions())
        .filter(eventHolder -> Objects.equals(eventHolder.success(), isSuccess))
        .findFirst()
        .orElseThrow(() -> new UnifiedAuditException(AuditExceptionMessages.NOT_SUITABLE_EVENT));
  }

  /**
   * Считает статус успешным в диапазоне 200–308, как в блокирующей реализации.
   *
   * @param status код ответа; {@code null} трактуется как 200
   * @return {@code true}, если статус в диапазоне успеха
   */
  private boolean isSuccessStatus(HttpStatusCode status) {
    final int value = Objects.isNull(status) ? HttpStatus.OK.value() : status.value();
    return value >= HttpStatus.OK.value() && value <= HttpStatus.PERMANENT_REDIRECT.value();
  }
}
