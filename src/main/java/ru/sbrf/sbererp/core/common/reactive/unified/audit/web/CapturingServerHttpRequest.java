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
 * <p>
 * Первый подписчик читает исходный поток. После завершения, если лимит не превышен, следующие
 * {@link #getBody()} получают replay из кэша. Если контроллер тело не читал, фильтр вызывает
 * {@link #captureUnreadBody()} после цепочки. Превышение лимита не обрывает HTTP-поток.
 */
public final class CapturingServerHttpRequest extends ServerHttpRequestDecorator {

  private final AuditBodyCapture capture;
  private final int maxBodyBytes;
  private Flux<DataBuffer> liveBody;
  private boolean subscribed;
  private boolean finished;

  /**
   * Оборачивает исходный запрос декоратором захвата тела.
   *
   * @param delegate     исходный {@link ServerHttpRequest}.
   * @param maxBodyBytes лимит буферизации для аудита.
   */
  public CapturingServerHttpRequest(ServerHttpRequest delegate, int maxBodyBytes) {
    super(delegate);
    this.maxBodyBytes = maxBodyBytes;
    this.capture = new AuditBodyCapture(maxBodyBytes, AuditLogMessages.REQUEST_BODY_EXCEEDS_LIMIT);
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
   * Показывает, подписался ли кто-то на {@link #getBody()}.
   *
   * @return {@code true}, если кто-то уже подписался на {@link #getBody()}.
   */
  public boolean isSubscribed() {
    synchronized (this) {
      return subscribed;
    }
  }

  /**
   * Читает тело для аудита, если контроллер его не потреблял.
   *
   * @return сигнал завершения чтения.
   */
  public Mono<Void> captureUnreadBody() {
    if (isSubscribed()) {
      return Mono.empty();
    }
    return DataBufferUtils.join(getBody(), maxBodyBytes)
        .doOnNext(DataBufferUtils::release)
        .then()
        .onErrorResume(DataBufferLimitException.class, ignored -> Mono.empty());
  }

  /**
   * Первый вызов отдаёт live-поток; после завершения — replay из кэша аудита.
   *
   * @return тело запроса.
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

  /**
   * Помечает, что live-поток тела завершился и дальше можно отдавать replay.
   */
  private void markFinished() {
    synchronized (this) {
      finished = true;
    }
  }

  /**
   * Отдаёт ранее скопированное тело как новый {@link Flux} буферов.
   *
   * @return replay кэша или пустой {@link Flux}, если тело пустое.
   */
  private Flux<DataBuffer> replayCaptured() {
    byte[] bytes = capture.capturedBody();
    if (bytes.length == AuditNumericConstants.ZERO) {
      return Flux.empty();
    }
    return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
  }
}
