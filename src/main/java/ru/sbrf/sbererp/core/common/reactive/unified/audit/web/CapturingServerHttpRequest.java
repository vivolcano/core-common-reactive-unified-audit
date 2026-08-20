package ru.sbrf.sbererp.core.common.reactive.unified.audit.web;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditNumericConstants;

/**
 * {@link ServerHttpRequest}, который копирует тело для аудита и отдаёт его повторно.
 *
 * <p>Превышение лимита не обрывает HTTP-поток. Если контроллер тело не читал, фильтр вызывает
 * {@link #captureUnreadBody()} после цепочки.
 */
public final class CapturingServerHttpRequest extends ServerHttpRequestDecorator {

  private final AuditBodyCapture capture;
  private final int maxBodyBytes;
  private Flux<DataBuffer> liveBody;
  private boolean subscribed;
  private boolean finished;

  /**
   * @param delegate     исходный запрос
   * @param maxBodyBytes лимит буферизации для аудита
   */
  public CapturingServerHttpRequest(ServerHttpRequest delegate, int maxBodyBytes) {
    super(delegate);
    this.maxBodyBytes = maxBodyBytes;
    this.capture = new AuditBodyCapture(maxBodyBytes, AuditLogMessages.REQUEST_BODY_EXCEEDS_LIMIT);
  }

  /**
   * @return тело для аудита либо пустой массив при превышении лимита
   */
  public byte[] capturedBody() {
    return capture.capturedBody();
  }

  /**
   * @return {@code true}, если кто-то уже подписался на {@link #getBody()}
   */
  public boolean isSubscribed() {
    synchronized (this) {
      return subscribed;
    }
  }

  /**
   * Читает тело для аудита, если контроллер его не потреблял.
   *
   * @return сигнал завершения чтения
   */
  public Mono<Void> captureUnreadBody() {
    return isSubscribed()
        ? Mono.empty()
        : DataBufferUtils.join(getBody(), maxBodyBytes)
            .doOnNext(DataBufferUtils::release)
            .then()
            .onErrorResume(DataBufferLimitException.class, ignored -> Mono.empty());
  }

  /**
   * Первый вызов отдаёт live-поток; после завершения — replay из кэша аудита.
   *
   * @return тело запроса
   */
  @Override
  public Flux<DataBuffer> getBody() {
    synchronized (this) {
      subscribed = true;
      if (finished) {
        return replayCaptured();
      }
      if (liveBody == null) {
        liveBody = super.getBody()
            .doOnNext(capture::append)
            .doOnComplete(this::markFinished)
            .doOnError(error -> markFinished());
      }
      return liveBody;
    }
  }

  private void markFinished() {
    synchronized (this) {
      finished = true;
    }
  }

  /**
   * @return replay кэша либо пустой {@link Flux}
   */
  private Flux<DataBuffer> replayCaptured() {
    final byte[] bytes = capture.capturedBody();
    return bytes.length == AuditNumericConstants.ZERO
        ? Flux.empty()
        : Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
  }
}
