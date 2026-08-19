package ru.sbrf.sbererp.core.common.unified.audit.adapter;

import com.sbt.audit.core.model.v2.event.Event;
import lombok.Builder;
import org.apache.commons.lang3.ObjectUtils;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ParamHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Внутреннее представление события до конвертации в {@link Event}.
 *
 * @param userLogin  claim {@code sub} либо заглушка.
 * @param userName   ФИО из JWT-claims либо заглушка.
 * @param requestId  заголовок {@code request-id} либо заглушка.
 * @param userNode   claim {@code sid} либо заглушка.
 * @param session    {@code jti} / cookie / заголовок сессии.
 * @param nodeId     идентификатор узла приложения.
 * @param eventName  имя события из YAML.
 * @param params     извлечённые параметры; ключ — {@link ParamHolder}.
 * @param isSuccess  {@code true}, если HTTP-статус в диапазоне 200–308.
 */
@Builder
public record EventAdapter(
    String userLogin,
    String userName,
    String requestId,
    String userNode,
    String session,
    String nodeId,
    String eventName,
    Map<ParamHolder, String> params,
    boolean isSuccess
) {

  /**
   * Нормализует карту параметров, если билдер её не задал.
   */
  public EventAdapter {
    params = Objects.requireNonNullElseGet(params, HashMap::new);
  }

  /**
   * Кладёт параметр в карту, если {@code value} непустое.
   *
   * @param key   описание параметра из YAML.
   * @param value результат экстрактора; {@code null} и пустые значения отбрасываются.
   */
  public void addParam(ParamHolder key, Object value) {
    if (ObjectUtils.isNotEmpty(value)) {
      params.put(key, value.toString());
    }
  }
}
