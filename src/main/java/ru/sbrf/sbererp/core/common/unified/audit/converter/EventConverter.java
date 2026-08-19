package ru.sbrf.sbererp.core.common.unified.audit.converter;

import com.sbt.audit.core.model.v2.event.Event;
import com.sbt.audit.core.model.v2.event.EventParam;
import com.sbt.audit.core.model.v2.event.EventParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sbrf.sbererp.core.common.unified.audit.adapter.EventAdapter;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditClientProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ParamHolder;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Компонент-конвертер, отвечающий за преобразование событий аудита из внутреннего формата
 * {@link EventAdapter} во внешний формат модели событий аудита SBT {@link Event}.
 */
@Component
@RequiredArgsConstructor
public class EventConverter {

  /**
   * Свойства клиента аудита, используемые для заполнения шапики события.
   */
  private final AuditClientProperties properties;

  /**
   * Метод преобразования события аудита из внутреннего формата в формат библиотеки SBT.
   *
   * @param baseEvent исходное внутреннее представление события аудита
   * @param <T>       тип расширенного адаптера события, наследуемого от {@link EventAdapter}
   * @return экземпляр события в стандартной модели SBT
   */
  public <T extends EventAdapter> Event convert(T baseEvent) {
    return Event.builder()
        .name(baseEvent.getEventName())
        .metamodelVersion(properties.metaModel().version())
        .module(properties.metaModel().module())
        .nodeId(baseEvent.getNodeId())
        .userNode(baseEvent.getUserNode())
        .session(baseEvent.getSession())
        .sourceSystem(properties.metaModel().sourceSystem())
        .tags(baseEvent.getTags())
        .userLogin(baseEvent.getUserLogin())
        .userName(baseEvent.getUserName())
        .requestId(baseEvent.getRequestId())
        .params(createParams(baseEvent.getParams()))
        .isSuccess(baseEvent.isSuccess())
        .build();
  }

  /**
   * Приватный вспомогательный метод для создания коллекций параметров события.
   * <p>
   * Преобразует карту параметров в стандартный формат коллекции параметров SBT.
   *
   * @param params карта параметров события, где ключ — это объект {@link ParamHolder},
   *               а значение — строковое представление параметра
   * @return коллекция стандартных параметров SBT
   */
  private EventParams createParams(Map<ParamHolder, String> params) {
    return params.entrySet().stream()
        .map(it -> createParam(it.getKey(), it.getValue()))
        .collect(Collectors.toCollection(EventParams::new));
  }

  /**
   * Приватный вспомогательный метод для создания отдельного параметра события.
   *
   * @param event объект параметра внутреннего представления
   * @param value строковое значение параметра
   * @return стандартный параметр SBT
   */
  private EventParam createParam(ParamHolder event, String value) {
    return EventParam.builder()
        .name(event.getName())
        .value(value)
        .build();
  }
}
