package ru.sbrf.sbererp.core.common.reactive.unified.audit.holder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.RequestExtractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.ConditionOperator;
import ru.sbrf.ufs.platform.audit.model.CriticalityEnum;

final class EventHolderTest {

  @Test
  void matchesConditionsRequiresAllCompiledCheckers() {
    ConditionHolder headerCondition = new ConditionHolder(
        "X-Audit-Tag",
        ConditionOperator.EQUALS,
        List.of("vip")
    );
    headerCondition.setExtractor(RequestExtractor.REQUEST_HEADER);
    headerCondition.setKey("X-Audit-Tag");

    EventHolder event = new EventHolder(
        "VipEvent",
        "vip",
        CriticalityEnum.UNCRITICAL,
        true,
        Map.of("request-header", List.of(new ParamHolder("tag", "tag", "X-Audit-Tag", null))),
        Map.of("request-header", List.of(headerCondition))
    );
    event.postCompile();

    MockServerWebExchange matching = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api").header("X-Audit-Tag", "vip").build()
    );
    MockServerWebExchange mismatching = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api").header("X-Audit-Tag", "regular").build()
    );

    assertThat(event.matchesConditions(matching)).isTrue();
    assertThat(event.matchesConditions(mismatching)).isFalse();
  }

  @Test
  void matchesConditionsFailsWhenCheckerWasNotCompiled() {
    EventHolder event = new EventHolder(
        "VipEvent",
        "vip",
        CriticalityEnum.UNCRITICAL,
        true,
        Map.of("request-header", List.of(new ParamHolder("tag", "tag", "X-Audit-Tag", null))),
        Map.of("request-header", List.of(new ConditionHolder(
            "X-Audit-Tag",
            ConditionOperator.EQUALS,
            List.of("vip")
        )))
    );

    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api").build()
    );

    assertThatThrownBy(() -> event.matchesConditions(exchange))
        .isInstanceOf(UnifiedAuditException.class);
  }
}
