package ru.sbrf.sbererp.core.common.unified.audit.web;

import java.io.ByteArrayOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditExchangeAttributeNames;

/**
 * Копирует тело HTTP для аудита, не забирая байты у исходного {@link DataBuffer}.
 * <p>
 * Если суммарный размер превышает лимит, накопленное отбрасывается: HTTP-поток не прерывается,
 * экстракторы получают пустое тело.
 */
@Slf4j
final class AuditBodyCapture {

  private final int maxBodyBytes;
  private final String overflowMessage;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private int written;
  private boolean exceeded;

  /**
   * @param maxBodyBytes    максимум байт для аудита.
   * @param overflowMessage текст WARN при превышении лимита.
   */
  AuditBodyCapture(int maxBodyBytes, String overflowMessage) {
    this.maxBodyBytes = maxBodyBytes;
    this.overflowMessage = overflowMessage;
  }

  /**
   * Копирует читаемые байты буфера, не сдвигая его {@code readPosition} для downstream.
   *
   * @param dataBuffer очередной фрагмент тела.
   */
  synchronized void append(DataBuffer dataBuffer) {
    if (exceeded || dataBuffer.readableByteCount() <= 0) {
      return;
    }
    int readable = dataBuffer.readableByteCount();
    if (written + readable > maxBodyBytes) {
      exceeded = true;
      written = 0;
      buffer.reset();
      log.warn(overflowMessage);
      return;
    }
    byte[] chunk = new byte[readable];
    int readPosition = dataBuffer.readPosition();
    dataBuffer.read(chunk);
    dataBuffer.readPosition(readPosition);
    buffer.write(chunk, 0, readable);
    written += readable;
  }

  /**
   * @return копия накопленного тела или пустой массив, если лимит превышен либо тело пусто
   */
  synchronized byte[] capturedBody() {
    if (exceeded || written == 0) {
      return AuditExchangeAttributeNames.EMPTY_BODY;
    }
    return buffer.toByteArray();
  }
}
