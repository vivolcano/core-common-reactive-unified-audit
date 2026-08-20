package ru.sbrf.sbererp.core.common.reactive.unified.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sbt.audit.core.model.v2.event.Event;
import com.sbt.audit.core.service.AuditService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.adapter.EventAdapter;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.converter.EventConverter;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.AuditClientProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.MetaModelTitleHolder;

final class AuditClientServiceImplTest {

  @Test
  void sendEventRunsOnElasticScheduler() {
    AtomicReference<String> threadName = new AtomicReference<>();
    AuditService auditService = new AuditService() {
      @Override
      public String audit(Event event) {
        threadName.set(Thread.currentThread().getName());
        return "event-id";
      }

      @Override
      public List<String> audit(List<Event> events) {
        return List.of();
      }

      @Override
      public String register(com.sbt.audit.core.model.v2.metamodel.Metamodel metamodel) {
        return "hash";
      }
    };
    EventConverter converter = new EventConverter(new AuditClientProperties(
        Map.of(),
        new MetaModelTitleHolder("1", "module", "subsystem", "source")
    ));
    AuditClientServiceImpl service = new AuditClientServiceImpl(
        auditService,
        converter,
        Schedulers.newBoundedElastic(1, 1, "unified-audit-test")
    );

    EventAdapter event = EventAdapter.builder()
        .eventName("TestEvent")
        .isSuccess(true)
        .build();

    StepVerifier.create(service.sendEvent(event))
        .verifyComplete();

    assertThat(threadName.get()).contains("unified-audit-test");
  }
}
