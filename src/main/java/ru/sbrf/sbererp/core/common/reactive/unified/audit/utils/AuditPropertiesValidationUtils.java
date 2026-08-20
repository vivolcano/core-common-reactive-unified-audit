package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ObjectUtils;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ParamHolder;
import ru.sbrf.ufs.platform.audit.model.CriticalityEnum;


/**
 * Проверки обязательных полей YAML. Пустое значение → {@link UnifiedAuditException}.
 */
@UtilityClass
public final class AuditPropertiesValidationUtils {

  /**
   * Валидирует поля "name" и "description" как обязательные для заполнения.
   *
   * @param name        имя события или параметра.
   * @param description описание события или параметра.
   */
  public static void validate(String name, String description) {
    validateString(name, AuditConfigurationFieldNames.NAME);
    validateString(description, AuditConfigurationFieldNames.DESCRIPTION);
  }

  /**
   * Валидирует параметры события: имя, описание, режим, флаг успешности и мапу ключ биндера → список
   * {@link ParamHolder}.
   *
   * @param name        имя события.
   * @param description описание события.
   * @param mode        режим отправки события {@link CriticalityEnum}.
   * @param success     признак успешности или неуспешности события.
   * @param paramsMap   мапа ключ биндера → список {@link ParamHolder}.
   */
  public static void validate(String name, String description, CriticalityEnum mode,
      Boolean success,
      Map<String, List<ParamHolder>> paramsMap) {
    validateString(name, AuditConfigurationFieldNames.NAME);
    validateString(description, AuditConfigurationFieldNames.DESCRIPTION);
    validateObject(mode, AuditConfigurationFieldNames.MODE);
    validateObject(success, AuditConfigurationFieldNames.SUCCESS);
    if (ObjectUtils.isEmpty(paramsMap)) {
      return;
    }
    validateMap(paramsMap, AuditConfigurationFieldNames.PARAMS);
  }

  /**
   * Валидирует метаданные заголовка модели аудита: версию, модуль, подсистему и источник системы.
   *
   * @param version      версия модели.
   * @param module       имя модуля.
   * @param subsystem    имя подсистемы.
   * @param sourceSystem источник системы.
   */
  public static void validate(String version, String module, String subsystem,
      String sourceSystem) {
    validateString(version, AuditConfigurationFieldNames.VERSION);
    validateString(module, AuditConfigurationFieldNames.MODULE);
    validateString(subsystem, AuditConfigurationFieldNames.SUBSYSTEM);
    validateString(sourceSystem, AuditConfigurationFieldNames.SOURCE_SYSTEM);
  }

  /**
   * Валидирует класс контроллера и мапу имя метода → список событий.
   *
   * @param controllerClass класс контроллера.
   * @param events          мапа имя метода → список событий YAML.
   */
  public static <T> void validate(Class<?> controllerClass, Map<String, List<T>> events) {
    validateObject(controllerClass, AuditConfigurationFieldNames.CONTROLLER_CLASS);
    validateMap(events, AuditConfigurationFieldNames.EVENTS);
  }

  /**
   * Проверяет, что строковое значение не пустое и не состоит только из пробелов.
   *
   * @param fieldValue значение для проверки.
   * @param fieldName  имя проверяемого поля.
   * @throws UnifiedAuditException если значение пустое.
   */
  private static void validateString(String fieldValue, String fieldName) {
    if (Objects.isNull(fieldValue) || fieldValue.isEmpty() || fieldValue.isBlank()) {
      throw new UnifiedAuditException(AuditExceptionMessages.FIELD_CAN_NOT_BE_EMPTY, fieldName);
    }
  }

  /**
   * Проверяет, что объект не равен {@code null}.
   *
   * @param fieldValue значение для проверки.
   * @param fieldName  имя проверяемого поля.
   * @throws UnifiedAuditException если объект равен {@code null}.
   */
  private static void validateObject(Object fieldValue, String fieldName) {
    if (Objects.isNull(fieldValue)) {
      throw new UnifiedAuditException(AuditExceptionMessages.FIELD_CAN_NOT_BE_EMPTY, fieldName);
    }
  }

  /**
   * Проверяет, что мапа не пустая и все её ключи и значения валидны.
   *
   * @param fieldValue мапа ключ YAML → список значений.
   * @param fieldName  имя проверяемого поля.
   * @throws UnifiedAuditException если мапа пустая или содержит пустые значения.
   */
  private static <T> void validateMap(Map<String, List<T>> fieldValue, String fieldName) {
    if (ObjectUtils.isEmpty(fieldValue)) {
      throw new UnifiedAuditException(AuditExceptionMessages.EMPTY_FIELD_IN_SECTION, fieldName);
    }
    for (Map.Entry<String, List<T>> entry : fieldValue.entrySet()) {
      validateString(entry.getKey(), fieldName);
      validateList(entry.getValue(), fieldName);
    }
  }

  /**
   * Проверяет, что список не пустой.
   *
   * @param fieldValue значение для проверки.
   * @param fieldName  имя проверяемого поля.
   * @throws UnifiedAuditException если список пустой.
   */
  private static void validateList(List<?> fieldValue, String fieldName) {
    if (ObjectUtils.isEmpty(fieldValue)) {
      throw new UnifiedAuditException(AuditExceptionMessages.EMPTY_FIELD_IN_SECTION, fieldName);
    }
  }

}
