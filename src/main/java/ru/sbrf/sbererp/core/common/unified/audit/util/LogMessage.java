package ru.sbrf.sbererp.core.common.unified.audit.util;

import lombok.experimental.UtilityClass;

/**
 * Строковые константы, представляющее шаблоны сообщений для логирования. Содержит различные форматы
 * сообщений для логирования регистрации метамодели и событий аудита в системе Единый аудит.
 */
@UtilityClass
public class LogMessage {

  /**
   * Шаблон для логирования деталей метамодели в систему Единый аудит.
   * <p>
   * Используется для логирования основных данных метамодели аудита, таких как версия, модуль,
   * события аудита.
   */
  public static final String METAMODEL_REGISTERING_START =
      "REGISTERING META MODEL [VERSION={}] [MODULE={}] [EVENTS={}]";

  /**
   * Шаблон для логирования зарегистрированной метамодели в систему Единый аудит.
   * <p>
   * Используется для логирования hash зарегистрированной метамодели аудита.
   */
  public static final String METAMODEL_REGISTERING_SUCCESS = "META MODEL WAS REGISTER SUCCESSFULLY [HASH={}]";

  /**
   * Шаблон для логирования ошибок при регистрации метамодели в систему Единый аудит.
   * <p>
   * Используется для логирования ошибок произошедших при регистрации метамодели аудита.
   */
  public static final String METAMODEL_REGISTERING_FAIL =
      "META MODEL REGISTERING WAS FAILED DUE TO ERROR "
          + "[MESSAGE={}]";

  /**
   * Шаблон для логирования деталей события аудита отправляемого в систему Единый аудит.
   * <p>
   * Используется для логирования названия события аудита.
   */
  public static final String AUDIT_EVENT_SEND_START = "SENDING AUDIT EVENT [EVENT={}]";

  /**
   * Шаблон для логирования зарегистрированного события аудита в систему Единый аудит.
   * <p>
   * Используется для логирования id зарегистрированного события аудита.
   */
  public static final String AUDIT_EVENT_SEND_SUCCESS = "AUDIT EVENT WAS SENT SUCCESSFULLY [ID={}]";

  /**
   * Шаблон для логирования ошибок при отправке событий аудита в систему Единый аудит.
   * <p>
   * Используется для логирования ошибок произошедших при регистрации события аудита.
   */
  public static final String AUDIT_EVENT_SEND_FAIL = "AUDIT EVENT SENDING WAS FAILED DUE TO ERROR [MESSAGE={}]";

  /**
   * Шаблон для логирования деталей списка событий аудита отправленных в систему Единый аудит.
   * <p>
   * Используется для логирования названий списка событий аудита.
   */
  public static final String AUDIT_EVENTS_SEND_START = "SENDING AUDIT EVENTS [EVENTS={}]";

  /**
   * Шаблон для логирования зарегистрированного события аудита в систему Единый аудит.
   * <p>
   * Используется для логирования списка id зарегистрированных событий аудита.
   */
  public static final String AUDIT_EVENTS_SEND_SUCCESS = "AUDIT EVENTS WAS SENT SUCCESSFULLY [IDS={}]";

  /**
   * Шаблон для логирования ошибок при отправке событий аудита в систему Единый аудит.
   * <p>
   * Используется для логирования ошибок произошедших при регистрации списка событий аудита.
   */
  public static final String AUDIT_EVENTS_SEND_FAIL = "AUDIT EVENTS SENDING WAS FAILED DUE TO ERROR [MESSAGE={}]";

  /**
   * Шаблон для логирования сообщения, что аудит выключен.
   * <p>
   * Используется для логирования сообщения, что аудит выключен.
   */
  public static final String AUDIT_DISABLED_DEBUG = "AUDIT IS DISABLED";

  /**
   * Сообщение, используемое при попытке извлечения данных, если операция не поддерживается.
   */
  public static final String EXTRACTION_NOT_SUPPORTED = "Извлечение не поддерживается";

  /**
   * Сообщение, используемое при попытке извлечения данных для определённого объекта или поля, если
   * операция не поддерживается.
   */
  public static final String EXTRACTION_NOT_SUPPORTED_FOR = "Извлечение не поддерживается для: %s";

  /**
   * Сообщение, используемое при ошибке маскирования (скрытия) определённого поля.
   */
  public static final String FAILED_TO_MASK_FIELD = "Не удалось маскировать поле по пути: {}, ошибка: {}";

  /**
   * Сообщение, используемое при отсутствии указанного поля в JSON-документе.
   */
  public static final String FIELD_NOT_FOUND_IN_JSON = "Поле %s не найдено в JSON";

  /**
   * Сообщение, используемое при пустом теле HTTP-запроса.
   */
  public static final String REQUEST_BODY_IS_EMPTY = "Пустое тело запроса, request = {}";

  /**
   * Сообщение, используемое при пустом теле HTTP-ответа.
   */
  public static final String RESPONSE_BODY_IS_EMPTY = "Пустое тело ответа, response = {}";

  /**
   * Сообщение, используемое при невозможности получения FQDN на основе IP-адреса из запроса.
   */
  public static final String CAN_NOT_FQDN_BY_IP = "Невозможно получить FQDN по IP адресу из запроса {%s}";

  /**
   * Сообщение, используемое при неудачной попытке преобразования строки в JSON.
   */
  public static final String CAN_NOT_CONVERTED_TO_JSON = "Не удалось преобразовать строку в json, {}";

  /**
   * Сообщение, используемое при невозможности получения идентификатора узла (NODE_ID).
   */
  public static final String UNABLE_TO_GET_NODE_ID = "Невозможно получить NODE_ID";

  /**
   * Сообщение, используемое при неудачной попытке создания объекта JsonNode из строки.
   */
  public static final String CAN_NOT_CREATE_JSON_NODE_FROM_STRING = "Не удалось создать JsonNode из строки {}";

  /**
   * Сообщение, используемое в процессе обработки бинов Spring, когда пути извлечения данных были
   * успешно определены.
   */
  public static final String POSTPROCESSOR_MESSAGE =
      "Пути извлечения данных логирования для бина с именем {}, "
          + "определены";

  /**
   * Сообщение, используемое при отсутствии указанных HTTP-кодов ответа в конфигурации.
   */
  public static final String MISSING_RESPONSE_CODES = "Отсутвуют коды ответа в: {%s}";

  /**
   * Сообщение, используемое при ошибке обработки дубликатов имён полей в JSON или конфигурации.
   */
  public static final String ERROR_HANDLING_DUPLICATE_FIELD_NAMES = "Ошибка обработки дубликатов имён полей: {%s}";

  /**
   * Сообщение, используемое при неудачной попытке извлечь ФИО пользователя из JWT-токена.
   */
  public static final String USER_NAME_NOT_EXTRACTED_FROM_JWT_TOKEN = "Невозможно получить ФИО пользователя из JWT-токена: {%s}";

  /**
   * Сообщение, используемое при неудачной попытке извлечь JWT-токен из запроса.
   */
  public static final String JWT_TOKEN_NOT_EXTRACTED = "Невозможно извлечь jwt-токен: {%s}";

  /**
   * Сообщение, используемое при отсутствии JWT-токена в заголовках HTTP-запроса.
   */
  public static final String JWT_TOKEN_NOT_FOUND = "JWT-токен не найден в заголовках запроса";

  /**
   * Сообщение, используемое при неудачной попытке извлечь логин пользователя.
   */
  public static final String USER_LOGIN_NOT_EXTRACTED = "Невозможно извлечь user login";

  /**
   * Сообщение, используемое при наличии пустого обязательного поля в YAML-конфигурационном файле.
   */
  public static final String FIELD_CAN_NOT_BE_EMPTY = "В yaml файле конфигурации поле %s не может быть пустым";

  /**
   * Сообщение, используемое при наличии пустых полей в указанной секции YAML-конфигурационного
   * файла.
   */
  public static final String EMPTY_FIELD_IN_SECTION = "В yaml файле конфигурации пустые поля в секции %s";

  /**
   * Нет подходящего события для аудита.
   */
  public static final String NOT_SUITABLE_EVENT = "Нет подходящего события";
  /**
   * Невозможно извлечь узел пользователя.
   */
  public static final String USER_NODE_NOT_EXTRACTED = "Невозможно извлечь userNode";
}
