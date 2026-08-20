package ru.sbrf.sbererp.core.common.reactive.unified.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ClassEventsHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.EventHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ParamHolder;
import ru.sbrf.ufs.platform.audit.model.CriticalityEnum;

final class AuditParameterBinderTest {

  @Test
  void yamlKeysAreDashSeparatedEnumNames() {
    assertThat(AuditParameterBinder.REQUEST.getParamsMapKey()).isEqualTo("request");
    assertThat(AuditParameterBinder.REQUEST_HEADER.getParamsMapKey()).isEqualTo("request-header");
    assertThat(AuditParameterBinder.RESPONSE_BODY.getParamsMapKey()).isEqualTo("response-body");
    assertThat(AuditParameterBinder.PATH_VARIABLE.getParamsMapKey()).isEqualTo("path-variable");
    assertThat(AuditParameterBinder.CLAIMS.getParamsMapKey()).isEqualTo("claims");
  }

  @Test
  void requestBinderKeepsBodyFieldExtractorsWhenWholeBodyParamIsPresent() {
    EventHolder event = new EventHolder(
        "ItemCreated",
        "created",
        CriticalityEnum.UNCRITICAL,
        true,
        Map.of("request", List.of(
            new ParamHolder("itemName", "name", "name", null),
            new ParamHolder("payload", "body", "request", List.of("secret"))
        )),
        null
    );
    ClassEventsHolder classEventsHolder = new ClassEventsHolder(
        BinderSampleController.class,
        Map.of("create", List.of(event))
    );

    AuditParameterBinder.configureAuditParameters(classEventsHolder, BinderSampleController.class);

    ParamHolder itemName = event.paramsMap().get("request").getFirst();
    ParamHolder payload = event.paramsMap().get("request").get(1);
    assertThat(itemName.getExtractor()).isEqualTo(RequestExtractor.REQUEST_BODY_FIELD);
    assertThat(payload.getExtractor()).isEqualTo(RequestExtractor.REQUEST_BODY);
  }

  static final class BinderSampleController {
    public Mono<String> create(@RequestBody Map<String, String> request) {
      return Mono.just("ok");
    }
  }
}
