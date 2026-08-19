package ru.sbrf.sbererp.core.common.unified.audit.filter;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditReactiveProperties;
import ru.sbrf.sbererp.core.common.unified.audit.resolver.AuditEventResolver;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditExchangeAttributeNames;
import ru.sbrf.sbererp.core.common.unified.audit.web.CachedBodyServerHttpRequest;
import ru.sbrf.sbererp.core.common.unified.audit.web.CapturingServerHttpResponse;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditHttpConstants;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditLogMessages;

/**
 * Буферизует тела запроса/ответа и после {@link WebFilterChain} вызывает {@link AuditEventResolver}.
 * <p>
 * GET не читает request body. Лимит — {@link AuditReactiveProperties#maxBodyBytes()}.
 * Аудит выполняется и при {@code onError} цепочки.
 */
@Slf4j
@RequiredArgsConstructor
public final class AuditWebFilter implements WebFilter {

  /**
   * Число буферов тела запроса, запрашиваемых у Netty за один раз.
   */
  private static final int REQUEST_BODY_PREFETCH = 8;

  private final AuditEventResolver auditEventResolver;
  private final AuditReactiveProperties reactiveProperties;

  /**
   * Кеширует тела, пропускает цепочку и отправляет событие аудита.
   *
   * @param exchange текущий обмен.
   * @param chain    оставшаяся цепочка фильтров.
   * @return сигнал завершения
   */
  @Override
  @NonNull
  public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    if (isGetRequest(exchange)) {
      log.debug(
          AuditLogMessages.SKIPPING_REQUEST_BODY_CACHE,
          exchange.getRequest().getMethod(),
          exchange.getRequest().getPath()
      );
      return filterWithResponseCapture(exchange, chain);
    }
    return cacheRequestBody(exchange)
        .flatMap(cachedBody -> filterWithCachedRequest(exchange, chain, cachedBody));
  }

  /**
   * Проверяет, что метод запроса — GET и тело кешировать не нужно.
   *
   * @param exchange текущий обмен.
   * @return {@code true}, если метод GET
   */
  private boolean isGetRequest(ServerWebExchange exchange) {
    HttpMethod method = exchange.getRequest().getMethod();
    return Objects.equals(HttpMethod.GET, method)
        || AuditHttpConstants.GET_METHOD.equalsIgnoreCase(method.name());
  }

  /**
   * Читает тело запроса в память с ограничением размера.
   *
   * @param exchange текущий обмен.
   * @return байты тела или пустой массив при отсутствии тела/превышении лимита
   */
  private Mono<byte[]> cacheRequestBody(ServerWebExchange exchange) {
    return DataBufferUtils.join(
            exchange.getRequest().getBody().limitRate(REQUEST_BODY_PREFETCH),
            reactiveProperties.maxBodyBytes()
        )
        .map(this::readAndRelease)
        .defaultIfEmpty(AuditExchangeAttributeNames.EMPTY_BODY)
        .onErrorResume(DataBufferLimitException.class, this::emptyBodyOnLimit);
  }

  /**
   * Подменяет запрос обёрткой с кэшем тела и продолжает цепочку.
   *
   * @param exchange   исходный обмен.
   * @param chain      цепочка фильтров.
   * @param cachedBody кэшированное тело.
   * @return сигнал завершения
   */
  private Mono<Void> filterWithCachedRequest(
      ServerWebExchange exchange,
      WebFilterChain chain,
      byte[] cachedBody) {
    CachedBodyServerHttpRequest cachedRequest =
        new CachedBodyServerHttpRequest(exchange.getRequest(), cachedBody);
    ServerWebExchange mutated = exchange.mutate().request(cachedRequest).build();
    mutated.getAttributes().put(AuditExchangeAttributeNames.CACHED_REQUEST_BODY, cachedBody);
    log.debug(
        AuditLogMessages.CACHED_REQUEST_BODY,
        cachedBody.length,
        mutated.getRequest().getMethod(),
        mutated.getRequest().getPath()
    );
    return filterWithResponseCapture(mutated, chain);
  }

  /**
   * Оборачивает ответ декоратором захвата тела, выполняет цепочку и аудирует результат.
   *
   * @param exchange текущий обмен.
   * @param chain    цепочка фильтров.
   * @return сигнал завершения
   */
  private Mono<Void> filterWithResponseCapture(ServerWebExchange exchange, WebFilterChain chain) {
    CapturingServerHttpResponse capturing =
        new CapturingServerHttpResponse(exchange.getResponse(), reactiveProperties.maxBodyBytes());
    ServerWebExchange mutated = exchange.mutate().response(capturing).build();
    return chain.filter(mutated)
        .then(Mono.defer(() -> afterChain(mutated, capturing)))
        .onErrorResume(error -> auditThenRethrow(mutated, capturing, error));
  }

  /**
   * Сохраняет тело ответа в атрибуты обмена и отправляет событие аудита.
   *
   * @param exchange  обмен с кэшем запроса.
   * @param capturing декоратор ответа.
   * @return сигнал завершения аудита
   */
  private Mono<Void> afterChain(
      ServerWebExchange exchange,
      CapturingServerHttpResponse capturing) {
    byte[] responseBody = capturing.capturedBody();
    exchange.getAttributes()
        .put(AuditExchangeAttributeNames.CACHED_RESPONSE_BODY, responseBody);
    log.debug(
        AuditLogMessages.CAPTURED_RESPONSE_BODY,
        responseBody.length,
        exchange.getRequest().getMethod(),
        exchange.getRequest().getPath()
    );
    return auditEventResolver.audit(exchange);
  }

  /**
   * Выполняет аудит при ошибке цепочки и пробрасывает исходное исключение.
   *
   * @param exchange  обмен.
   * @param capturing декоратор ответа.
   * @param error     ошибка цепочки.
   * @return сигнал ошибки после аудита
   */
  private Mono<Void> auditThenRethrow(
      ServerWebExchange exchange,
      CapturingServerHttpResponse capturing,
      Throwable error) {
    log.debug(
        AuditLogMessages.AUDITING_FAILED_REQUEST,
        exchange.getRequest().getMethod(),
        exchange.getRequest().getPath()
    );
    return afterChain(exchange, capturing).then(Mono.error(error));
  }

  /**
   * Копирует байты буфера и освобождает его.
   *
   * @param dataBuffer буфер тела запроса.
   * @return копия байтов
   */
  private byte[] readAndRelease(DataBuffer dataBuffer) {
    byte[] bytes = new byte[dataBuffer.readableByteCount()];
    dataBuffer.read(bytes);
    DataBufferUtils.release(dataBuffer);
    return bytes;
  }

  /**
   * Возвращает пустое тело, если лимит буферизации превышен.
   *
   * @param exception ошибка лимита.
   * @return пустой массив байтов
   */
  private Mono<byte[]> emptyBodyOnLimit(DataBufferLimitException exception) {
    log.warn(AuditLogMessages.REQUEST_BODY_EXCEEDS_LIMIT, exception);
    return Mono.just(AuditExchangeAttributeNames.EMPTY_BODY);
  }
}
