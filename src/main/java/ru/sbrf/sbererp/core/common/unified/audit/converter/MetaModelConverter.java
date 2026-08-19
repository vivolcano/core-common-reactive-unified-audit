package ru.sbrf.sbererp.core.common.unified.audit.converter;

import com.sbt.audit.core.model.v2.metamodel.EventMetaInfo;
import com.sbt.audit.core.model.v2.metamodel.EventMetaInfos;
import com.sbt.audit.core.model.v2.metamodel.MetaInfoParam;
import com.sbt.audit.core.model.v2.metamodel.MetaInfoParams;
import com.sbt.audit.core.model.v2.metamodel.Metamodel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditClientProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.EventHolder;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ParamHolder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Собирает {@link Metamodel} из {@link AuditEventsProperties} и заголовка {@link AuditClientProperties}.
 * <p>
 * Вызывается один раз из {@code MetaModelSender} на {@code ApplicationReadyEvent}.
 */
@Primary
@Component
@RequiredArgsConstructor
public final class MetaModelConverter {

  /**
   * Свойства событий аудита, содержащие метаданные событий и параметры.
   */
  private final AuditEventsProperties config;

  /**
   * Клиентские свойства аудита, задающие глобальные параметры метамодели (версия, модуль и др.).
   */
  private final AuditClientProperties properties;

  /**
   * Создает полную метамодель событий аудита.
   *
   * @return полная структура метамодели для АС Единый Аудит
   */
  public Metamodel create() {
    return Metamodel.builder()
        .metamodelVersion(properties.metaModel().version())
        .module(properties.metaModel().module())
        .eventMetaInfos(createEventMetaInfos())
        .build();
  }

  /**
   * Генерирует коллекцию объектов метаданных событий.
   *
   * @return коллекция объектов {@link EventMetaInfo}
   */
  private EventMetaInfos createEventMetaInfos() {
    return this.config.metamodelEvents().stream()
        .map(this::createMetaInfoParams)
        .collect(Collectors.toCollection(EventMetaInfos::new));
  }

  /**
   * Преобразует внутреннее представление события в объект метаданных события для АС Единый Аудит.
   *
   * @param event внутреннее представление события.
   * @return объект метаданных события SBT
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
   * Преобразует список внутренних параметров события в коллекцию стандартных параметров для АС Единый Аудит.
   *
   * @param params список внутренних параметров события.
   * @return коллекция стандартных параметров SBT
   */
  private MetaInfoParams toMetaInfoParams(List<ParamHolder> params) {
    return params.stream()
        .map(this::toMetaInfoParam)
        .collect(Collectors.toCollection(MetaInfoParams::new));
  }

  /**
   * Преобразует один внутренний параметр события в стандартный параметр для АС Единый Аудит.
   *
   * @param param внутренний параметр события.
   * @return стандартный параметр SBT
   */
  private MetaInfoParam toMetaInfoParam(ParamHolder param) {
    return MetaInfoParam.builder()
        .name(param.name())
        .description(param.description())
        .build();
  }
}
