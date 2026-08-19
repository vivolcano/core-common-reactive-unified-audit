package ru.sbrf.sbererp.core.common.unified.audit.properties.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ObjectUtils;
import ru.sbrf.sbererp.core.common.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ParamHolder;
import ru.sbrf.sbererp.core.common.unified.audit.util.LogMessage;
import ru.sbrf.ufs.platform.audit.model.CriticalityEnum;


/**
 * Утилитарный класс для валидации параметров и структур, используемых в настройках аудита.
 * <p>
 * Обеспечивает проверку обязательных полей на корректность и наличие значений, выбрасывая
 * исключения при обнаружении ошибок.
 */
@UtilityClass
public class ValidateUtil {

  /**
   * Валидирует поля "name" и "description" как обязательные для заполнения.
   *
   * @param name        имя события или параметра
   * @param description описание события или параметра
   */
  public static void validate(String name, String description) {
    validateString(name, MessageStringConstant.NAME);
    validateString(description, MessageStringConstant.DESCRIPTION);
  }

  /**
   * Валидирует параметры события: имя, описание, режим, флаг успешности и карту параметров.
   *
   * @param name        имя события
   * @param description описание события
   * @param mode        режим отправки события
   * @param success     призна успешности или неуспешности события
   * @param paramsMap   карта параметров события
   */
  public static void validate(String name, String description, CriticalityEnum mode,
      Boolean success,
      Map<String, List<ParamHolder>> paramsMap) {
    validateString(name, MessageStringConstant.NAME);
    validateString(description, MessageStringConstant.DESCRIPTION);
    validateObject(mode, MessageStringConstant.MODE);
    validateObject(success, MessageStringConstant.SUCCESS);
    if (ObjectUtils.isEmpty(paramsMap)) {
      return;
    }
    validateMap(paramsMap, MessageStringConstant.PARAMS);
  }

  /**
   * Валидирует метаданные заголовка модели аудита: версию, модуль, подсистему и источник системы.
   *
   * @param version      версия модели
   * @param module       имя модуля
   * @param subsystem    имя подсистемы
   * @param sourceSystem источник системы
   */
  public static void validate(String version, String module, String subsystem,
      String sourceSystem) {
    validateString(version, MessageStringConstant.VERSION);
    validateString(module, MessageStringConstant.MODULE);
    validateString(subsystem, MessageStringConstant.SUBSYSTEM);
    validateString(sourceSystem, MessageStringConstant.SOURCE_SYSTEM);
  }

  /**
   * Валидирует класс контроллера и карту событий, связанных с ним.
   *
   * @param controllerClass класс контроллера
   * @param events          карта событий
   */
  public static <T> void validate(Class<?> controllerClass, Map<String, List<T>> events) {
    validateObject(controllerClass, MessageStringConstant.CONTROLLER_CLASS);
    validateMap(events, MessageStringConstant.EVENTS);
  }

  /**
   * Проверяет, что строковое значение не пустое и не состоит только из пробелов.
   *
   * @param fieldValue значение для проверки
   * @param fieldName  имя проверяемого поля
   * @throws UnifiedAuditException если значение пустое
   */
  private static void validateString(String fieldValue, String fieldName) {
    if (Objects.isNull(fieldValue) || fieldValue.isEmpty() || fieldValue.isBlank()) {
      throw new UnifiedAuditException(LogMessage.FIELD_CAN_NOT_BE_EMPTY, fieldName);
    }
  }

  /**
   * Проверяет, что объект не равен null.
   *
   * @param fieldValue значение для проверки
   * @param fieldName  имя проверяемого поля
   * @throws UnifiedAuditException если объект равен null
   */
  private static void validateObject(Object fieldValue, String fieldName) {
    if (Objects.isNull(fieldValue)) {
      throw new UnifiedAuditException(LogMessage.FIELD_CAN_NOT_BE_EMPTY, fieldName);
    }
  }

  /**
   * Проверяет, что карта не пустая и все её ключи и значения валидны.
   *
   * @param fieldValue значение для проверки
   * @param fieldName  имя проверяемого поля
   * @throws UnifiedAuditException если карта пустая или содержит пустые значения
   */
  private static <T> void validateMap(Map<String, List<T>> fieldValue, String fieldName) {
    if (ObjectUtils.isEmpty(fieldValue)) {
      throw new UnifiedAuditException(LogMessage.EMPTY_FIELD_IN_SECTION, fieldName);
    }
    for (Map.Entry<String, List<T>> entry : fieldValue.entrySet()) {
      validateString(entry.getKey(), fieldName);
      validateList(entry.getValue(), fieldName);
    }
  }

  /**
   * Проверяет, что список не пустой.
   *
   * @param fieldValue значение для проверки
   * @param fieldName  имя проверяемого поля
   * @throws UnifiedAuditException если список пустой
   */
  private static void validateList(List<?> fieldValue, String fieldName) {
    if (ObjectUtils.isEmpty(fieldValue)) {
      throw new UnifiedAuditException(LogMessage.EMPTY_FIELD_IN_SECTION, fieldName);
    }
  }

  /**
   * Внутренний статический класс, содержащий константы имен полей для использования в сообщениях об
   * ошибках.
   */
  @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  static class MessageStringConstant {

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String MODE = "mode";
    public static final String SUCCESS = "success";
    public static final String PARAMS = "params";
    public static final String EVENTS = "events";
    public static final String VERSION = "version";
    public static final String MODULE = "module";
    public static final String SUBSYSTEM = "subsystem";
    public static final String SOURCE_SYSTEM = "sourceSystem";
    public static final String CONTROLLER_CLASS = "controllerClass";
  }
}
