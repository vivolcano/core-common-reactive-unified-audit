package ru.sbrf.sbererp.core.common.reactive.unified.audit.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.test.StepVerifier;

final class CapturingServerHttpRequestTest {

  @Test
  void getBodyDeliversFullPayloadWhenAuditLimitIsExceeded() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/api")
        .body("hello-world");
    CapturingServerHttpRequest capturing = new CapturingServerHttpRequest(request, 4);

    StepVerifier.create(DataBufferUtils.join(capturing.getBody()))
        .assertNext(buffer -> {
          assertThat(toString(buffer)).isEqualTo("hello-world");
          DataBufferUtils.release(buffer);
        })
        .verifyComplete();

    assertThat(capturing.capturedBody()).isEmpty();
  }

  @Test
  void capturedBodyKeepsPayloadWhenUnderLimit() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/api")
        .body("ok");
    CapturingServerHttpRequest capturing = new CapturingServerHttpRequest(request, 16);

    StepVerifier.create(DataBufferUtils.join(capturing.getBody()))
        .assertNext(buffer -> {
          assertThat(toString(buffer)).isEqualTo("ok");
          DataBufferUtils.release(buffer);
        })
        .verifyComplete();

    assertThat(capturing.capturedBody()).isEqualTo("ok".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void secondSubscriberReplaysCachedBody() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/api")
        .body("replay-me");
    CapturingServerHttpRequest capturing = new CapturingServerHttpRequest(request, 64);

    StepVerifier.create(DataBufferUtils.join(capturing.getBody()))
        .assertNext(DataBufferUtils::release)
        .verifyComplete();

    StepVerifier.create(DataBufferUtils.join(capturing.getBody()))
        .assertNext(buffer -> {
          assertThat(toString(buffer)).isEqualTo("replay-me");
          DataBufferUtils.release(buffer);
        })
        .verifyComplete();
  }

  @Test
  void captureUnreadBodyFillsAuditCacheWhenNobodyReadTheBody() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/api")
        .body("{\"id\":1}");
    CapturingServerHttpRequest capturing = new CapturingServerHttpRequest(request, 64);

    StepVerifier.create(capturing.captureUnreadBody())
        .verifyComplete();

    assertThat(capturing.capturedBody()).isEqualTo("{\"id\":1}".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void captureUnreadBodyIsNoOpAfterControllerAlreadySubscribed() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/api")
        .body("first-read");
    CapturingServerHttpRequest capturing = new CapturingServerHttpRequest(request, 64);

    StepVerifier.create(DataBufferUtils.join(capturing.getBody()).doOnNext(DataBufferUtils::release).then())
        .verifyComplete();

    StepVerifier.create(capturing.captureUnreadBody())
        .verifyComplete();

    assertThat(capturing.capturedBody()).isEqualTo("first-read".getBytes(StandardCharsets.UTF_8));
  }

  private static String toString(DataBuffer buffer) {
    byte[] bytes = new byte[buffer.readableByteCount()];
    buffer.read(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
