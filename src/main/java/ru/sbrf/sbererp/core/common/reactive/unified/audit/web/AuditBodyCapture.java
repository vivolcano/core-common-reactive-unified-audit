package ru.sbrf.sbererp.core.common.reactive.unified.audit.web;

import java.io.ByteArrayOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditExchangeAttributeNames;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditNumericConstants;

/**
 * Копия HTTP-тела для аудита без потребления исходного {@link DataBuffer}.
 * При превышении лимита кэш сбрасывается целиком, HTTP-поток не прерывается.
 */
@Slf4j
final class AuditBodyCapture {

  private final int maxBodyBytes;
  private final String overflowMessage;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private int written;
  private boolean exceeded;

  /**
   * Создаёт буфер копии HTTP-тела для аудита.
   *
   * @param maxBodyBytes    лимит кэша аудита в байтах.
   * @param overflowMessage WARN при сбросе кэша.
   */
  AuditBodyCapture(int maxBodyBytes, String overflowMessage) {
    this.maxBodyBytes = maxBodyBytes;
    this.overflowMessage = overflowMessage;
  }

  /**
   * Копирует читаемые байты и возвращает {@link DataBuffer#readPosition()}, чтобы HTTP видел тот же фрагмент.
   * Если {@code written + chunk} больше лимита, кэш очищается, последующие фрагменты игнорируются.
   *
   * @param dataBuffer текущий фрагмент тела.
   */
  synchronized void append(DataBuffer dataBuffer) {
    if (exceeded || dataBuffer.readableByteCount() <= AuditNumericConstants.ZERO) {
      return;
    }
    final int readable = dataBuffer.readableByteCount();
    if (written + readable > maxBodyBytes) {
      exceeded = true;
      written = AuditNumericConstants.ZERO;
      buffer.reset();
      log.warn(overflowMessage);
      return;
    }
    final byte[] chunk = new byte[readable];
    final int readPosition = dataBuffer.readPosition();
    dataBuffer.read(chunk);
    dataBuffer.readPosition(readPosition);
    buffer.write(chunk, AuditNumericConstants.ZERO, readable);
    written += readable;
  }

  /**
   * Возвращает копию накопленных байт.
   *
   * @return копия накопленных байт либо пустой массив, если кэш пуст или сброшен.
   */
  synchronized byte[] capturedBody() {
    return exceeded || written == AuditNumericConstants.ZERO
        ? AuditExchangeAttributeNames.EMPTY_BODY
        : buffer.toByteArray();
  }
}
