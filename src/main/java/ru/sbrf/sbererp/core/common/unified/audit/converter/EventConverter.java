package ru.sbrf.sbererp.core.common.unified.audit.converter;

import com.sbt.audit.core.model.v2.event.Event;
import com.sbt.audit.core.model.v2.event.EventParam;
import com.sbt.audit.core.model.v2.event.EventParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sbrf.sbererp.core.common.unified.audit.adapter.EventAdapter;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditClientProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ParamHolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Конвертирует {@link EventAdapter} в модель клиента SBT {@link Event}.
 * <p>
 * Шапку метамодели (version/module/sourceSystem) берёт из {@link AuditClientProperties}.
 */
@Component
@RequiredArgsConstructor
public final class EventConverter {

  private final AuditClientProperties properties;

  /**
   * Собирает {@link Event} из адаптера и {@link AuditClientProperties#metaModel()}.
   *
   * @param baseEvent внутреннее событие после резолвера.
   * @return модель SBT для {@code AuditService#audit}
   */
  public Event convert(EventAdapter baseEvent) {
    return Event.builder()
        .name(baseEvent.eventName())
        .metamodelVersion(properties.metaModel().version())
        .module(properties.metaModel().module())
        .nodeId(baseEvent.nodeId())
        .userNode(baseEvent.userNode())
        .session(baseEvent.session())
        .sourceSystem(properties.metaModel().sourceSystem())
        .tags(List.of())
        .userLogin(baseEvent.userLogin())
        .userName(baseEvent.userName())
        .requestId(baseEvent.requestId())
        .params(createParams(baseEvent.params()))
        .isSuccess(baseEvent.isSuccess())
        .build();
  }

  /**
   * Копирует карту адаптера в {@link EventParams}.
   *
   * @param params ключ — YAML-параметр, значение — строка экстрактора.
   * @return коллекция SBT; пустая, если карта пустая
   */
  private EventParams createParams(Map<ParamHolder, String> params) {
    return params.entrySet().stream()
        .map(it -> createParam(it.getKey(), it.getValue()))
        .collect(Collectors.toCollection(EventParams::new));
  }

  /**
   * @param event YAML-параметр.
   * @param value извлечённое значение.
   * @return элемент {@link EventParams}
   */
  private EventParam createParam(ParamHolder event, String value) {
    return EventParam.builder()
        .name(event.name())
        .value(value)
        .build();
  }
}
