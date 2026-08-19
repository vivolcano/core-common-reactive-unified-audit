package ru.sbrf.sbererp.core.common.unified.audit.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditLogMessages;

final class AuditBodyCaptureTest {

  @Test
  void appendCopiesBytesWithoutConsumingBuffer() {
    AuditBodyCapture capture = new AuditBodyCapture(16, AuditLogMessages.REQUEST_BODY_EXCEEDS_LIMIT);
    DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap("abcd".getBytes(StandardCharsets.UTF_8));
    int readPosition = buffer.readPosition();

    capture.append(buffer);

    assertThat(buffer.readPosition()).isEqualTo(readPosition);
    assertThat(toString(buffer)).isEqualTo("abcd");
    assertThat(capture.capturedBody()).isEqualTo("abcd".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void appendDropsAuditCacheWhenLimitExceededButLeavesBufferReadable() {
    AuditBodyCapture capture = new AuditBodyCapture(3, AuditLogMessages.REQUEST_BODY_EXCEEDS_LIMIT);
    DataBuffer first = DefaultDataBufferFactory.sharedInstance.wrap("ab".getBytes(StandardCharsets.UTF_8));
    DataBuffer second = DefaultDataBufferFactory.sharedInstance.wrap("cd".getBytes(StandardCharsets.UTF_8));

    capture.append(first);
    capture.append(second);

    assertThat(toString(first)).isEqualTo("ab");
    assertThat(toString(second)).isEqualTo("cd");
    assertThat(capture.capturedBody()).isEmpty();
  }

  private static String toString(DataBuffer buffer) {
    byte[] bytes = new byte[buffer.readableByteCount()];
    int readPosition = buffer.readPosition();
    buffer.read(bytes);
    buffer.readPosition(readPosition);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
