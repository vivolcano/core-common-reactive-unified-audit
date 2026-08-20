package ru.sbrf.sbererp.core.common.reactive.unified.audit.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.sbt.audit.core.model.v2.event.Event;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.adapter.EventAdapter;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditClientProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.MetaModelTitleHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ParamHolder;

final class EventConverterTest {

  @Test
  void convertCopiesHeaderAndParams() {
    EventConverter converter = new EventConverter(new AuditClientProperties(
        Map.of(),
        new MetaModelTitleHolder("1.0.0", "module", "subsystem", "source")
    ));
    ParamHolder param = new ParamHolder("itemName", "name", "name", null);
    EventAdapter adapter = EventAdapter.builder()
        .eventName("ItemCreated")
        .userLogin("audit-user")
        .userName("Ivan Petrov")
        .requestId("req-1")
        .userNode("node")
        .session("session")
        .nodeId("pod")
        .isSuccess(true)
        .build();
    adapter.addParam(param, "widget");

    Event event = converter.convert(adapter);

    assertThat(event.getName()).isEqualTo("ItemCreated");
    assertThat(event.getMetamodelVersion()).isEqualTo("1.0.0");
    assertThat(event.getModule()).isEqualTo("module");
    assertThat(event.getSourceSystem()).isEqualTo("source");
    assertThat(event.getUserLogin()).isEqualTo("audit-user");
    assertThat(event.isSuccess()).isTrue();
    assertThat(event.getParams()).hasSize(1);
    assertThat(event.getParams().getFirst().getName()).isEqualTo("itemName");
    assertThat(event.getParams().getFirst().getValue()).isEqualTo("widget");
  }
}
