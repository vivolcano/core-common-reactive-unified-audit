package ru.sbrf.sbererp.core.common.reactive.unified.audit.filter;

import io.vavr.control.Option;
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
 * {@link WebFilter}, который копирует тела запроса/ответа и после цепочки вызывает {@link AuditEventResolver}.
 *
 * <p>GET не копирует request body. Пути {@code audit.reactive.exclude-path-patterns} пропускаются.
 */
@Slf4j
public final class AuditWebFilter implements WebFilter {

  private final AuditEventResolver auditEventResolver;
  private final AuditReactiveProperties reactiveProperties;
  private final List<PathPattern> excludePathPatterns;

  /**
   * Создаёт WebFlux-фильтр аудита: захват тел и вызов резолвера после цепочки.
   *
   * @param auditEventResolver резолвер события после цепочки.
   * @param reactiveProperties лимит тел и exclude-пути.
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
    final CapturingServerHttpRequest capturingRequest = capturingRequest(exchange);
    final ServerWebExchange withRequest = Objects.isNull(capturingRequest)
        ? exchange
        : exchange.mutate().request(capturingRequest).build();
    return filterWithResponseCapture(withRequest, chain, capturingRequest);
  }

  /**
   * @param exchange текущий обмен.
   * @return {@code true}, если путь совпал с {@code audit.reactive.exclude-path-patterns}.
   */
  private boolean isExcluded(ServerWebExchange exchange) {
    return AuditReactiveProperties.matchesAny(
        exchange.getRequest().getPath().pathWithinApplication(),
        excludePathPatterns
    );
  }

  /**
   * @param exchange текущий обмен.
   * @return декоратор захвата либо {@code null} для GET.
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
   * @param exchange текущий обмен.
   * @return {@code true}, если метод GET.
   */
  private boolean isGetRequest(ServerWebExchange exchange) {
    final HttpMethod method = exchange.getRequest().getMethod();
    return Objects.equals(HttpMethod.GET, method)
        || AuditHttpConstants.GET_METHOD.equalsIgnoreCase(method.name());
  }

  /**
   * Оборачивает ответ декоратором захвата, выполняет цепочку и аудирует результат.
   *
   * @param exchange         текущий обмен.
   * @param chain            цепочка фильтров.
   * @param capturingRequest декоратор запроса либо {@code null} для GET.
   * @return сигнал завершения.
   */
  private Mono<Void> filterWithResponseCapture(
      ServerWebExchange exchange,
      WebFilterChain chain,
      CapturingServerHttpRequest capturingRequest) {
    final CapturingServerHttpResponse capturingResponse =
        new CapturingServerHttpResponse(exchange.getResponse(), reactiveProperties.maxBodyBytes());
    final ServerWebExchange mutated = exchange.mutate().response(capturingResponse).build();
    return chain.filter(mutated)
        .then(Mono.defer(() -> afterChain(mutated, capturingRequest, capturingResponse)))
        .onErrorResume(error -> auditThenRethrow(mutated, capturingRequest, capturingResponse, error));
  }

  /**
   * Дочитывает непрочитанное тело запроса, сохраняет кэш и отправляет событие.
   *
   * @param exchange          обмен с декораторами.
   * @param capturingRequest  декоратор запроса либо {@code null}.
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
   * @param capturingRequest декоратор запроса либо {@code null}.
   * @return сигнал дочитывания тела либо пустой {@link Mono}.
   */
  private Mono<Void> captureUnreadRequestBody(CapturingServerHttpRequest capturingRequest) {
    return Objects.isNull(capturingRequest) ? Mono.empty() : capturingRequest.captureUnreadBody();
  }

  /**
   * Кладёт кэш тел в атрибуты обмена.
   *
   * @param exchange          текущий обмен.
   * @param capturingRequest  декоратор запроса либо {@code null}.
   * @param capturingResponse декоратор ответа.
   */
  private void storeBodies(
      ServerWebExchange exchange,
      CapturingServerHttpRequest capturingRequest,
      CapturingServerHttpResponse capturingResponse) {
    storeRequestBody(exchange, capturingRequest);
    final byte[] responseBody = capturingResponse.capturedBody();
    exchange.getAttributes().put(AuditExchangeAttributeNames.CACHED_RESPONSE_BODY, responseBody);
    log.debug(
        AuditLogMessages.CAPTURED_RESPONSE_BODY,
        responseBody.length,
        exchange.getRequest().getMethod(),
        exchange.getRequest().getPath()
    );
  }

  /**
   * @param exchange         текущий обмен.
   * @param capturingRequest декоратор запроса либо {@code null}.
   */
  private void storeRequestBody(
      ServerWebExchange exchange,
      CapturingServerHttpRequest capturingRequest) {
    Option.of(capturingRequest).forEach(request -> {
      final byte[] requestBody = request.capturedBody();
      exchange.getAttributes().put(AuditExchangeAttributeNames.CACHED_REQUEST_BODY, requestBody);
      log.debug(
          AuditLogMessages.CACHED_REQUEST_BODY,
          requestBody.length,
          exchange.getRequest().getMethod(),
          exchange.getRequest().getPath()
      );
    });
  }

  /**
   * Выполняет аудит при ошибке цепочки и пробрасывает исходное исключение.
   *
   * @param exchange          обмен.
   * @param capturingRequest  декоратор запроса либо {@code null}.
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
