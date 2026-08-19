package ru.sbrf.sbererp.core.common.unified.audit.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.sbt.audit.core.model.v2.metamodel.Metamodel;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditClientProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ClassEventsHolder;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.EventHolder;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.MetaModelTitleHolder;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ParamHolder;
import ru.sbrf.ufs.platform.audit.model.CriticalityEnum;

final class MetaModelConverterTest {

  @Test
  void createBuildsEventMetaInfosFromYamlHolders() {
    EventHolder event = new EventHolder(
        "ItemCreated",
        "created",
        CriticalityEnum.UNCRITICAL,
        true,
        Map.of("request", List.of(new ParamHolder("itemName", "name", "name", null))),
        null
    );
    AuditEventsProperties events = new AuditEventsProperties(List.of(
        new ClassEventsHolder(MetaModelConverterTest.class, Map.of("create", List.of(event)))
    ));
    MetaModelConverter converter = new MetaModelConverter(
        events,
        new AuditClientProperties(
            Map.of(),
            new MetaModelTitleHolder("1.0.0", "module", "subsystem", "source")
        )
    );

    Metamodel metamodel = converter.create();

    assertThat(metamodel.getMetamodelVersion()).isEqualTo("1.0.0");
    assertThat(metamodel.getModule()).isEqualTo("module");
    assertThat(metamodel.getEventMetaInfos()).hasSize(1);
    assertThat(metamodel.getEventMetaInfos().getFirst().getName()).isEqualTo("ItemCreated");
    assertThat(metamodel.getEventMetaInfos().getFirst().getSubsystem()).isEqualTo("subsystem");
    assertThat(metamodel.getEventMetaInfos().getFirst().getMetaInfoParams().getFirst().getName())
        .isEqualTo("itemName");
  }
}
