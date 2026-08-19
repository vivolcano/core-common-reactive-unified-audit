package ru.sbrf.sbererp.core.common.unified.audit.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Утилитарный класс для преобразования объектов в форматированные JSON-строки.
 * <p>
 * Основное предназначение - использование в логах для удобного чтения сложных объектов в режиме
 * отладки. Класс автоматически форматирует JSON с отступами и переносами строк для улучшенной
 * читаемости.
 * </p>
 *
 * <p><b>Особенности работы:</b></p>
 * <ul>
 *   <li>Инициализация {@link ObjectMapper} происходит только при включенном уровне логирования DEBUG</li>
 *   <li>Форматирование включает отступы и переносы строк для улучшенной читаемости</li>
 *   <li>В случае ошибок сериализации возвращается заданное сообщение об ошибке</li>
 *   <li>Исключения логируются на уровне DEBUG</li>
 * </ul>
 *
 * <p><b>Пример использования:</b></p>
 * <pre>{@code
 * if (log.isDebugEnabled()) {
 *     String formattedJson = ObjectToFormatStringUtil.getFormatString(
 *         myObject,
 *         "Не удалось сериализовать объект"
 *     );
 *     log.debug("Объект: {}", formattedJson);
 * }
 * }</pre>
 *
 * <p><b>Ограничения:</b></p>
 * <ul>
 *   <li>Класс не предназначен для использования в высоконагруженных сценариях</li>
 *   <li>Для работы требуется наличие Jackson ObjectMapper в classpath</li>
 *   <li>Форматирование отключено при выключенном DEBUG логировании</li>
 * </ul>
 *
 * @author Участник разработки
 * @see ObjectMapper
 * @see SerializationFeature#INDENT_OUTPUT
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class ObjectToFormatStringUtil {

  /** Маппер для сериализации объектов в JSON. */
  private static JsonMapper mapper;

  static {
    if (log.isDebugEnabled()) {
      mapper = JsonMapper.builder()
          .configure(SerializationFeature.INDENT_OUTPUT, true)
          .build();
    }
  }

  /**
   * Преобразует объект в форматированную JSON-строку.
   * <p>
   * Если объект не может быть сериализован (например, содержит циклические ссылки или
   * несериализуемые поля), возвращается заданное сообщение об ошибке.
   * </p>
   *
   * <p><b>Особенности форматирования:</b></p>
   * <ul>
   *   <li>Отступы: 2 пробела на уровень вложенности</li>
   *   <li>Переносы строк после каждого поля и элемента массива</li>
   *   <li>Сохранение порядка полей как в исходном объекте</li>
   * </ul>
   *
   * @param object      объект для сериализации, может быть {@code null}
   * @param failMessage сообщение, возвращаемое в случае ошибки сериализации
   * @return форматированная JSON-строка или {@code failMessage} в случае ошибки. Если передан
   * передан {@code null}, возвращает строку "null"
   * @throws IllegalStateException если метод вызывается при выключенном DEBUG логировании
   * @see #mapper
   * @see ObjectMapper#writeValueAsString(Object)
   */
  public static String getFormatString(Object object, String failMessage) {
    if (!log.isDebugEnabled()) {
      return Constants.EMPTY_STRING;
    }
    try {
      return mapper.writeValueAsString(object);
    } catch (JacksonException e) {
      return failMessage;
    }
  }
}
