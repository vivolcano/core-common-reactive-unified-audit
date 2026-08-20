package ru.sbrf.sbererp.core.common.reactive.unified.audit.web;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;

/**
 * {@link ServerHttpResponse}, который копирует тело для аудита и сразу пишет его делегату.
 *
 * <p>Превышение лимита оставляет кэш пустым и не меняет HTTP-ответ.
 */
public final class CapturingServerHttpResponse extends ServerHttpResponseDecorator {

  private final AuditBodyCapture capture;

  /**
   * @param delegate     исходный ответ
   * @param maxBodyBytes лимит буферизации для аудита
   */
  public CapturingServerHttpResponse(ServerHttpResponse delegate, int maxBodyBytes) {
    super(delegate);
    this.capture = new AuditBodyCapture(maxBodyBytes, AuditLogMessages.RESPONSE_BODY_EXCEEDS_LIMIT);
  }

  /**
   * @return тело для аудита либо пустой массив при превышении лимита
   */
  public byte[] capturedBody() {
    return capture.capturedBody();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return super.writeWith(tee(body));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return super.writeAndFlushWith(Flux.from(body).map(this::tee));
  }

  /**
   * @param body исходный поток буферов
   * @return тот же поток с копированием в кэш аудита
   */
  private Flux<? extends DataBuffer> tee(Publisher<? extends DataBuffer> body) {
    return Flux.from(body).doOnNext(capture::append);
  }
}
