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
   * @param name        имя события или параметра
   * @param description описание события или параметра
   */
  public static void validate(String name, String description) {
    validateString(name, AuditConfigurationFieldNames.NAME);
    validateString(description, AuditConfigurationFieldNames.DESCRIPTION);
  }

  /**
   * @param name        имя события
   * @param description описание события
   * @param mode        режим отправки
   * @param success     признак успешности
   * @param paramsMap   ключ биндера → список {@link ParamHolder}
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
   * @param version      версия модели
   * @param module       имя модуля
   * @param subsystem    имя подсистемы
   * @param sourceSystem источник системы
   */
  public static void validate(String version, String module, String subsystem,
      String sourceSystem) {
    validateString(version, AuditConfigurationFieldNames.VERSION);
    validateString(module, AuditConfigurationFieldNames.MODULE);
    validateString(subsystem, AuditConfigurationFieldNames.SUBSYSTEM);
    validateString(sourceSystem, AuditConfigurationFieldNames.SOURCE_SYSTEM);
  }

  /**
   * @param controllerClass класс контроллера
   * @param events          имя метода → список событий YAML
   */
  public static <T> void validate(Class<?> controllerClass, Map<String, List<T>> events) {
    validateObject(controllerClass, AuditConfigurationFieldNames.CONTROLLER_CLASS);
    validateMap(events, AuditConfigurationFieldNames.EVENTS);
  }

  /**
   * @param fieldValue значение для проверки
   * @param fieldName  имя проверяемого поля
   * @throws UnifiedAuditException если значение пустое
   */
  private static void validateString(String fieldValue, String fieldName) {
    if (Objects.isNull(fieldValue) || fieldValue.isBlank()) {
      throw new UnifiedAuditException(AuditExceptionMessages.FIELD_CAN_NOT_BE_EMPTY, fieldName);
    }
  }

  /**
   * @param fieldValue значение для проверки
   * @param fieldName  имя проверяемого поля
   * @throws UnifiedAuditException если объект равен {@code null}
   */
  private static void validateObject(Object fieldValue, String fieldName) {
    if (Objects.isNull(fieldValue)) {
      throw new UnifiedAuditException(AuditExceptionMessages.FIELD_CAN_NOT_BE_EMPTY, fieldName);
    }
  }

  /**
   * @param fieldValue ключ YAML → список значений
   * @param fieldName  имя проверяемого поля
   * @throws UnifiedAuditException если мапа пустая или содержит пустые значения
   */
  private static <T> void validateMap(Map<String, List<T>> fieldValue, String fieldName) {
    if (ObjectUtils.isEmpty(fieldValue)) {
      throw new UnifiedAuditException(AuditExceptionMessages.EMPTY_FIELD_IN_SECTION, fieldName);
    }
    fieldValue.forEach((key, value) -> {
      validateString(key, fieldName);
      validateList(value, fieldName);
    });
  }

  /**
   * @param fieldValue значение для проверки
   * @param fieldName  имя проверяемого поля
   * @throws UnifiedAuditException если список пустой
   */
  private static void validateList(List<?> fieldValue, String fieldName) {
    if (ObjectUtils.isEmpty(fieldValue)) {
      throw new UnifiedAuditException(AuditExceptionMessages.EMPTY_FIELD_IN_SECTION, fieldName);
    }
  }

}
