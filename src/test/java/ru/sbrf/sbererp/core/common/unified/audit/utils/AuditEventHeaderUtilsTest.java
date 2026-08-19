package ru.sbrf.sbererp.core.common.unified.audit.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

final class AuditEventHeaderUtilsTest {

  @Test
  void getRequestIdReturnsHeaderValue() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api")
        .header(AuditHttpConstants.REQUEST_ID, "req-1")
        .build();

    assertThat(AuditEventHeaderUtils.getRequestId(request)).isEqualTo("req-1");
  }

  @Test
  void getSessionPrefersJwtJti() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api").build();

    String session = AuditEventHeaderUtils.getSession(Map.of(AuditJwtConstants.JTI, "token-id"), request);

    assertThat(session).isEqualTo("jwt_claim_jti:token-id");
  }

  @Test
  void getSessionFallsBackToCookie() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api")
        .cookie(new HttpCookie(AuditHttpConstants.JSESSIONID, "abc"))
        .build();

    String session = AuditEventHeaderUtils.getSession(Map.of(), request);

    assertThat(session).isEqualTo(AuditHttpConstants.COOKIE_WITH_UNDERSCORE + "abc");
  }
}
