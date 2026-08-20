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
 * <p>
 * Буферы не склеиваются и не ограничивают запись клиенту. Если размер превышает лимит,
 * кэш аудита остаётся пустым, HTTP-ответ продолжается без изменений.
 */
public final class CapturingServerHttpResponse extends ServerHttpResponseDecorator {

  private final AuditBodyCapture capture;

  /**
   * Оборачивает исходный ответ декоратором захвата тела.
   *
   * @param delegate     исходный {@link ServerHttpResponse}.
   * @param maxBodyBytes лимит буферизации для аудита.
   */
  public CapturingServerHttpResponse(ServerHttpResponse delegate, int maxBodyBytes) {
    super(delegate);
    this.capture = new AuditBodyCapture(maxBodyBytes, AuditLogMessages.RESPONSE_BODY_EXCEEDS_LIMIT);
  }

  /**
   * Возвращает тело, скопированное для аудита.
   *
   * @return тело для аудита или пустой массив при превышении лимита.
   */
  public byte[] capturedBody() {
    return capture.capturedBody();
  }

  /**
   * Пишет тело делегату, параллельно копируя байты в кэш аудита.
   *
   * @param body поток буферов тела ответа.
   * @return сигнал завершения записи в делегат.
   */
  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return super.writeWith(tee(body));
  }

  /**
   * Сохраняет границы flush и копирует каждый фрагмент для аудита.
   *
   * @param body поток порций тела ответа.
   * @return сигнал завершения записи в делегат.
   */
  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return super.writeAndFlushWith(Flux.from(body).map(this::tee));
  }

  /**
   * Копирует каждый буфер в кэш аудита и пропускает тот же поток дальше.
   *
   * @param body исходный поток буферов {@link DataBuffer}.
   * @return тот же поток с копированием в кэш аудита.
   */
  private Flux<? extends DataBuffer> tee(Publisher<? extends DataBuffer> body) {
    return Flux.from(body).doOnNext(capture::append);
  }
}
