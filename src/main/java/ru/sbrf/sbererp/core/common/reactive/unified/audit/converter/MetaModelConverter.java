package ru.sbrf.sbererp.core.common.reactive.unified.audit.converter;

import com.sbt.audit.core.model.v2.metamodel.EventMetaInfo;
import com.sbt.audit.core.model.v2.metamodel.EventMetaInfos;
import com.sbt.audit.core.model.v2.metamodel.MetaInfoParam;
import com.sbt.audit.core.model.v2.metamodel.MetaInfoParams;
import com.sbt.audit.core.model.v2.metamodel.Metamodel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditClientProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.EventHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ParamHolder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Собирает {@link Metamodel} из {@link AuditEventsProperties} и заголовка {@link AuditClientProperties}.
 */
@Primary
@Component
@RequiredArgsConstructor
public final class MetaModelConverter {

  private final AuditEventsProperties config;
  private final AuditClientProperties properties;

  /**
   * @return метамодель для АС Единый Аудит
   */
  public Metamodel create() {
    return Metamodel.builder()
        .metamodelVersion(properties.metaModel().version())
        .module(properties.metaModel().module())
        .eventMetaInfos(createEventMetaInfos())
        .build();
  }

  /**
   * @return коллекция {@link EventMetaInfo}
   */
  private EventMetaInfos createEventMetaInfos() {
    return this.config.metamodelEvents().stream()
        .map(this::createMetaInfoParams)
        .collect(Collectors.toCollection(EventMetaInfos::new));
  }

  /**
   * @param event внутреннее представление события
   * @return метаданные события SBT
   */
  private EventMetaInfo createMetaInfoParams(EventHolder event) {
    return EventMetaInfo.builder()
        .name(event.name())
        .description(event.description())
        .mode(event.mode())
        .success(event.success())
        .subsystem(properties.metaModel().subsystem())
        .metaInfoParams(toMetaInfoParams(event.params()))
        .build();
  }

  /**
   * @param params список внутренних параметров события
   * @return коллекция параметров SBT
   */
  private MetaInfoParams toMetaInfoParams(List<ParamHolder> params) {
    return params.stream()
        .map(this::toMetaInfoParam)
        .collect(Collectors.toCollection(MetaInfoParams::new));
  }

  /**
   * @param param внутренний параметр события
   * @return параметр SBT
   */
  private MetaInfoParam toMetaInfoParam(ParamHolder param) {
    return MetaInfoParam.builder()
        .name(param.name())
        .description(param.description())
        .build();
  }
}
