package ru.sbrf.sbererp.core.common.unified.audit.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

final class CapturingServerHttpResponseTest {

  @Test
  void writeWithSendsFullPayloadWhenAuditLimitIsExceeded() {
    MockServerHttpResponse delegate = new MockServerHttpResponse();
    CapturingServerHttpResponse capturing = new CapturingServerHttpResponse(delegate, 4);
    DataBuffer body = DefaultDataBufferFactory.sharedInstance.wrap(
        "hello-world".getBytes(StandardCharsets.UTF_8));

    StepVerifier.create(capturing.writeWith(Mono.just(body)))
        .verifyComplete();

    assertThat(capturing.capturedBody()).isEmpty();
    StepVerifier.create(delegate.getBodyAsString())
        .expectNext("hello-world")
        .verifyComplete();
  }

  @Test
  void capturedBodyKeepsPayloadWhenUnderLimit() {
    MockServerHttpResponse delegate = new MockServerHttpResponse();
    CapturingServerHttpResponse capturing = new CapturingServerHttpResponse(delegate, 16);
    DataBuffer body = DefaultDataBufferFactory.sharedInstance.wrap("ok".getBytes(StandardCharsets.UTF_8));

    StepVerifier.create(capturing.writeWith(Mono.just(body)))
        .verifyComplete();

    assertThat(capturing.capturedBody()).isEqualTo("ok".getBytes(StandardCharsets.UTF_8));
    StepVerifier.create(delegate.getBodyAsString())
        .expectNext("ok")
        .verifyComplete();
  }

  @Test
  void writeAndFlushWithStillWritesWhenAuditLimitIsExceeded() {
    MockServerHttpResponse delegate = new MockServerHttpResponse();
    CapturingServerHttpResponse capturing = new CapturingServerHttpResponse(delegate, 3);
    DataBuffer first = DefaultDataBufferFactory.sharedInstance.wrap("ab".getBytes(StandardCharsets.UTF_8));
    DataBuffer second = DefaultDataBufferFactory.sharedInstance.wrap("cd".getBytes(StandardCharsets.UTF_8));

    StepVerifier.create(capturing.writeAndFlushWith(Flux.just(Mono.just(first), Mono.just(second))))
        .verifyComplete();

    assertThat(capturing.capturedBody()).isEmpty();
    StepVerifier.create(delegate.getBodyAsString())
        .expectNext("abcd")
        .verifyComplete();
  }
}
