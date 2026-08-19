package ru.sbrf.sbererp.core.common.unified.audit.web;

import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ServerHttpResponse}, который запоминает записанное тело для последующего аудита.
 * <p>
 * Декоратор перехватывает {@link #writeWith} и {@link #writeAndFlushWith}, копирует буферы в массив
 * байтов и сразу передаёт их делегату. Объём буферизации ограничен {@code maxBodyBytes}.
 */
public final class CapturingServerHttpResponse extends ServerHttpResponseDecorator {

  /**
   * Число буферов тела ответа, запрашиваемых у writer'а за один раз.
   */
  private static final int RESPONSE_BODY_PREFETCH = 8;

  private final AtomicReference<byte[]> capturedBody =
      new AtomicReference<>(AuditExchangeAttributes.EMPTY_BODY);
  private final int maxBodyBytes;

  /**
   * Создаёт декоратор ответа.
   *
   * @param delegate     исходный ответ
   * @param maxBodyBytes лимит буферизации
   */
  public CapturingServerHttpResponse(ServerHttpResponse delegate, int maxBodyBytes) {
    super(delegate);
    this.maxBodyBytes = maxBodyBytes;
  }

  /**
   * Возвращает захваченное тело ответа.
   *
   * @return тело ответа или пустой массив
   */
  public byte[] capturedBody() {
    return capturedBody.get();
  }

  /**
   * Собирает тело ответа в память, запоминает его и сразу пишет делегату.
   *
   * @param body поток буферов тела ответа
   * @return сигнал завершения записи в делегат
   */
  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return DataBufferUtils.join(
            Flux.from(body)
                .limitRate(RESPONSE_BODY_PREFETCH)
                .doOnDiscard(DataBuffer.class, DataBufferUtils::release),
            maxBodyBytes
        )
        .defaultIfEmpty(bufferFactory().wrap(AuditExchangeAttributes.EMPTY_BODY))
        .flatMap(this::writeJoinedBuffer);
  }

  /**
   * Сводит chunked-запись к {@link #writeWith}.
   *
   * @param body поток порций тела ответа
   * @return сигнал завершения записи в делегат
   */
  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return writeWith(Flux.from(body).concatMap(Flux::from));
  }

  /**
   * Копирует соединённый буфер в кэш и пишет его делегату.
   *
   * @param joined соединённый буфер тела
   * @return сигнал завершения записи
   */
  private Mono<Void> writeJoinedBuffer(DataBuffer joined) {
    byte[] bytes = readBytes(joined);
    DataBufferUtils.release(joined);
    capturedBody.set(bytes);
    return super.writeWith(Mono.just(bufferFactory().wrap(bytes)));
  }

  /**
   * Копирует содержимое буфера в новый массив байтов.
   *
   * @param dataBuffer буфер с телом ответа
   * @return копия читаемых байтов буфера
   */
  private byte[] readBytes(DataBuffer dataBuffer) {
    byte[] bytes = new byte[dataBuffer.readableByteCount()];
    dataBuffer.read(bytes);
    return bytes;
  }
}
