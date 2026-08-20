package ru.sbrf.sbererp.core.common.reactive.unified.audit.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.AuditReactiveProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.resolver.AuditEventResolver;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditExchangeAttributeNames;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.web.CapturingServerHttpRequest;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.web.CapturingServerHttpResponse;

final class AuditWebFilterTest {

  @Test
  void excludedActuatorPathSkipsAuditAndBodyCapture() {
    AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
    AuditWebFilter filter = newFilter();
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/actuator/health").build()
    );
    WebFilterChain chain = current -> {
      seen.set(current);
      return current.getResponse().setComplete();
    };

    StepVerifier.create(filter.filter(exchange, chain))
        .verifyComplete();

    assertThat((Object) seen.get().getAttribute(AuditExchangeAttributeNames.CACHED_RESPONSE_BODY))
        .isNull();
  }

  @Test
  void unreadRequestBodyIsCapturedForAuditAfterTheChain() {
    AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
    AuditWebFilter filter = newFilter();
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.post("/api/items").body("{\"name\":\"n\"}")
    );
    WebFilterChain chain = current -> {
      seen.set(current);
      return current.getResponse().setComplete();
    };

    StepVerifier.create(filter.filter(exchange, chain))
        .verifyComplete();

    assertThat((byte[]) seen.get().getAttribute(AuditExchangeAttributeNames.CACHED_REQUEST_BODY))
        .isEqualTo("{\"name\":\"n\"}".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void getRequestSkipsRequestBodyCaptureButStillCapturesResponse() {
    AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
    AuditWebFilter filter = newFilter();
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/items").build()
    );
    WebFilterChain chain = current -> {
      seen.set(current);
      return current.getResponse().setComplete();
    };

    StepVerifier.create(filter.filter(exchange, chain))
        .verifyComplete();

    assertThat(seen.get().getRequest()).isNotInstanceOf(CapturingServerHttpRequest.class);
    assertThat(seen.get().getResponse()).isInstanceOf(CapturingServerHttpResponse.class);
    assertThat((Object) seen.get().getAttribute(AuditExchangeAttributeNames.CACHED_REQUEST_BODY))
        .isNull();
    assertThat((byte[]) seen.get().getAttribute(AuditExchangeAttributeNames.CACHED_RESPONSE_BODY))
        .isEmpty();
  }

  private static AuditWebFilter newFilter() {
    return new AuditWebFilter(
        new AuditEventResolver(event -> Mono.empty(), new AuditEventsProperties(List.of())),
        new AuditReactiveProperties(null, null)
    );
  }
}
