package ru.sbrf.sbererp.core.common.reactive.unified.audit.filter;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditReactiveProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.resolver.AuditEventResolver;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditExchangeAttributeNames;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditHttpConstants;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.web.CapturingServerHttpRequest;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.web.CapturingServerHttpResponse;

/**
 * Копирует тела запроса/ответа для аудита и после {@link WebFilterChain} вызывает
 * {@link AuditEventResolver}.
 * <p>
 * HTTP-потоки не прерываются лимитом аудита. GET не копирует request body.
 * Пути из {@code audit.reactive.exclude-path-patterns} пропускаются целиком.
 * Если контроллер не читал тело, оно дочитывается после цепочки для YAML {@code request-body}.
 */
@Slf4j
public final class AuditWebFilter implements WebFilter {

  private final AuditEventResolver auditEventResolver;
  private final AuditReactiveProperties reactiveProperties;
  private final List<PathPattern> excludePathPatterns;

  /**
   * Создаёт WebFlux-фильтр аудита: захват тел и вызов резолвера после цепочки.
   *
   * @param auditEventResolver резолвер события {@link AuditEventResolver} после цепочки.
   * @param reactiveProperties лимит тел и exclude-пути {@link AuditReactiveProperties}.
   */
  public AuditWebFilter(
      AuditEventResolver auditEventResolver,
      AuditReactiveProperties reactiveProperties) {
    this.auditEventResolver = auditEventResolver;
    this.reactiveProperties = reactiveProperties;
    this.excludePathPatterns = reactiveProperties.compiledExcludePathPatterns();
  }

  /**
   * Оборачивает обмен декораторами захвата тел, пропускает цепочку и отправляет событие аудита.
   *
   * @param exchange текущий обмен.
   * @param chain    оставшаяся цепочка фильтров.
   * @return сигнал завершения.
   */
  @Override
  @NonNull
  public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    if (isExcluded(exchange)) {
      log.debug(
          AuditLogMessages.SKIPPING_EXCLUDED_PATH,
          exchange.getRequest().getMethod(),
          exchange.getRequest().getPath()
      );
      return chain.filter(exchange);
    }
    CapturingServerHttpRequest capturingRequest = capturingRequest(exchange);
    ServerWebExchange withRequest = Objects.isNull(capturingRequest)
        ? exchange
        : exchange.mutate().request(capturingRequest).build();
    return filterWithResponseCapture(withRequest, chain, capturingRequest);
  }

  /**
   * Проверяет, что путь запроса совпал с одним из exclude-шаблонов.
   *
   * @param exchange текущий {@link ServerWebExchange}.
   * @return {@code true}, если путь совпал с {@code audit.reactive.exclude-path-patterns}.
   */
  private boolean isExcluded(ServerWebExchange exchange) {
    return AuditReactiveProperties.matchesAny(
        exchange.getRequest().getPath().pathWithinApplication(),
        excludePathPatterns
    );
  }

  /**
   * Для GET не подписывается на тело запроса.
   *
   * @param exchange текущий обмен.
   * @return декоратор захвата или {@code null} для GET.
   */
  private CapturingServerHttpRequest capturingRequest(ServerWebExchange exchange) {
    if (isGetRequest(exchange)) {
      log.debug(
          AuditLogMessages.SKIPPING_REQUEST_BODY_CACHE,
          exchange.getRequest().getMethod(),
          exchange.getRequest().getPath()
      );
      return null;
    }
    return new CapturingServerHttpRequest(exchange.getRequest(), reactiveProperties.maxBodyBytes());
  }

  /**
   * Определяет, что запрос не несёт тело, которое нужно копировать.
   *
   * @param exchange текущий {@link ServerWebExchange}.
   * @return {@code true}, если метод GET.
   */
  private boolean isGetRequest(ServerWebExchange exchange) {
    HttpMethod method = exchange.getRequest().getMethod();
    return Objects.equals(HttpMethod.GET, method)
        || AuditHttpConstants.GET_METHOD.equalsIgnoreCase(method.name());
  }

  /**
   * Оборачивает ответ декоратором захвата тела, выполняет цепочку и аудирует результат.
   *
   * @param exchange         текущий обмен.
   * @param chain            цепочка фильтров.
   * @param capturingRequest декоратор запроса или {@code null} для GET.
   * @return сигнал завершения.
   */
  private Mono<Void> filterWithResponseCapture(
      ServerWebExchange exchange,
      WebFilterChain chain,
      CapturingServerHttpRequest capturingRequest) {
    CapturingServerHttpResponse capturingResponse =
        new CapturingServerHttpResponse(exchange.getResponse(), reactiveProperties.maxBodyBytes());
    ServerWebExchange mutated = exchange.mutate().response(capturingResponse).build();
    return chain.filter(mutated)
        .then(Mono.defer(() -> afterChain(mutated, capturingRequest, capturingResponse)))
        .onErrorResume(error -> auditThenRethrow(mutated, capturingRequest, capturingResponse, error));
  }

  /**
   * Дочитывает непрочитанное тело запроса, сохраняет кэш и отправляет событие.
   *
   * @param exchange          обмен с декораторами.
   * @param capturingRequest  декоратор запроса или {@code null}.
   * @param capturingResponse декоратор ответа.
   * @return сигнал завершения аудита.
   */
  private Mono<Void> afterChain(
      ServerWebExchange exchange,
      CapturingServerHttpRequest capturingRequest,
      CapturingServerHttpResponse capturingResponse) {
    return captureUnreadRequestBody(capturingRequest)
        .then(Mono.fromRunnable(() -> storeBodies(exchange, capturingRequest, capturingResponse)))
        .then(Mono.defer(() -> auditEventResolver.audit(exchange)));
  }

  /**
   * Дочитывает тело запроса, если контроллер его не потреблял.
   *
   * @param capturingRequest декоратор запроса {@link CapturingServerHttpRequest} или {@code null}.
   * @return сигнал дочитывания тела или пустой {@link Mono}.
   */
  private Mono<Void> captureUnreadRequestBody(CapturingServerHttpRequest capturingRequest) {
    if (Objects.isNull(capturingRequest)) {
      return Mono.empty();
    }
    return capturingRequest.captureUnreadBody();
  }

  /**
   * Кладёт кэш тел в атрибуты обмена.
   *
   * @param exchange          текущий обмен.
   * @param capturingRequest  декоратор запроса или {@code null}.
   * @param capturingResponse декоратор ответа.
   */
  private void storeBodies(
      ServerWebExchange exchange,
      CapturingServerHttpRequest capturingRequest,
      CapturingServerHttpResponse capturingResponse) {
    storeRequestBody(exchange, capturingRequest);
    byte[] responseBody = capturingResponse.capturedBody();
    exchange.getAttributes()
        .put(AuditExchangeAttributeNames.CACHED_RESPONSE_BODY, responseBody);
    log.debug(
        AuditLogMessages.CAPTURED_RESPONSE_BODY,
        responseBody.length,
        exchange.getRequest().getMethod(),
        exchange.getRequest().getPath()
    );
  }

  /**
   * Кладёт кэш тела запроса в атрибуты, если декоратор использовался.
   *
   * @param exchange         текущий обмен.
   * @param capturingRequest декоратор запроса или {@code null}.
   */
  private void storeRequestBody(
      ServerWebExchange exchange,
      CapturingServerHttpRequest capturingRequest) {
    if (Objects.isNull(capturingRequest)) {
      return;
    }
    byte[] requestBody = capturingRequest.capturedBody();
    exchange.getAttributes().put(AuditExchangeAttributeNames.CACHED_REQUEST_BODY, requestBody);
    log.debug(
        AuditLogMessages.CACHED_REQUEST_BODY,
        requestBody.length,
        exchange.getRequest().getMethod(),
        exchange.getRequest().getPath()
    );
  }

  /**
   * Выполняет аудит при ошибке цепочки и пробрасывает исходное исключение.
   *
   * @param exchange          обмен.
   * @param capturingRequest  декоратор запроса или {@code null}.
   * @param capturingResponse декоратор ответа.
   * @param error             ошибка цепочки.
   * @return сигнал ошибки после аудита.
   */
  private Mono<Void> auditThenRethrow(
      ServerWebExchange exchange,
      CapturingServerHttpRequest capturingRequest,
      CapturingServerHttpResponse capturingResponse,
      Throwable error) {
    log.debug(
        AuditLogMessages.AUDITING_FAILED_REQUEST,
        exchange.getRequest().getMethod(),
        exchange.getRequest().getPath()
    );
    return afterChain(exchange, capturingRequest, capturingResponse).then(Mono.error(error));
  }
}
