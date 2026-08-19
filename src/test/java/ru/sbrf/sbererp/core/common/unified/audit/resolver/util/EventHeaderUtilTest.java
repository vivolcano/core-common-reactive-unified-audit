package ru.sbrf.sbererp.core.common.unified.audit.resolver.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import ru.sbrf.sbererp.core.common.unified.audit.util.Constants;

class EventHeaderUtilTest {

  @Test
  void getRequestIdReturnsHeaderValue() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api")
        .header(Constants.REQUEST_ID, "req-1")
        .build();

    assertThat(EventHeaderUtil.getRequestId(request)).isEqualTo("req-1");
  }

  @Test
  void getSessionPrefersJwtJti() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api").build();

    String session = EventHeaderUtil.getSession(Map.of(Constants.JTI, "token-id"), request);

    assertThat(session).isEqualTo("jwt_claim_jti:token-id");
  }

  @Test
  void getSessionFallsBackToCookie() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api")
        .cookie(new HttpCookie(Constants.JSESSIONID, "abc"))
        .build();

    String session = EventHeaderUtil.getSession(Map.of(), request);

    assertThat(session).isEqualTo(Constants.COOKIE_WITH_UNDERSCORE + "abc");
  }
}
