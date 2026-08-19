package ru.sbrf.sbererp.core.common.unified.audit.adapter;

import com.sbt.audit.core.model.v2.event.Event;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.ObjectUtils;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ParamHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс-адаптер для создания событий аудита, предназначен для сбора и передачи основных атрибутов и
 * параметров событий.
 * <p>
 * Содержит информацию о пользователе, сессии, узле, идентификаторе запроса, названии события, тегах
 * и параметрах. Экземпляр класса после создания преобразуется в экземпляр
 * {@link Event}
 */
@Getter
@ToString
@Builder
public class EventAdapter {

    /**
     * Логин пользователя, инициировавшего действие.
     */
    private final String userLogin;

    /**
     * Полное имя пользователя, инициировавшего действие.
     */
    private final String userName;

    /**
     * Идентификатор текущего запроса.
     */
    private final String requestId;

    /**
     * Узел, на котором произошло действие.
     */
    private final String userNode;

    /**
     * Уникальный идентификатор текущей сессии.
     */
    private final String session;

    /**
     * Идентификатор узла, связанный с событием.
     */
    private final String nodeId;

    /**
     * Название регистрируемого события.
     */
    private final String eventName;

    /**
     * Список тегов, ассоциированных с событием.
     */
    @Builder.Default
    private final List<String> tags = new ArrayList<>();

    /**
     * Параметры события, представленные парами ключ-значение, где ключ — объект {@link ParamHolder},
     * а значение — строковое представление параметра.
     */
    @Builder.Default
    private final Map<ParamHolder, String> params = new HashMap<>();

    /**
     * Флаг успешности выполнения операции
     */
    private final boolean isSuccess;

    /**
     * Добавляет новый параметр в событие.
     * <p>
     * Если значение не является пустым, добавляет пару ключ-значение в карту параметров.
     *
     * @param key   ключ параметра типа {@link ParamHolder}
     * @param value значение параметра
     */
    public void addParam(ParamHolder key, Object value) {
        if (ObjectUtils.isNotEmpty(value)) {
            params.put(key, value.toString());
        }
    }

    /**
     * Добавляет тег к событию.
     * <p>
     * Если значение не является пустым, преобразует его в строку и добавляет в список тегов.
     *
     * @param value добавляемое значение тега
     */
    public void addTag(Object value) {
        if (ObjectUtils.isNotEmpty(value)) {
            tags.add(value.toString());
        }
    }
}
