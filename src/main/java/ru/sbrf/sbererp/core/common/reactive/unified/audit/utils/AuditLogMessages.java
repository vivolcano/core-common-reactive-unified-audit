package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * Шаблоны сообщений SLF4J модуля аудита.
 * <p>
 * Payload события и метамодели пишутся только на DEBUG. INFO содержит идентификаторы
 * (имя события, id, версия) без тел запроса/ответа и содержимого JWT.
 */
@UtilityClass
public final class AuditLogMessages {

  /** DEBUG: путь совпал с {@code audit.reactive.exclude-path-patterns}. */
  public static final String SKIPPING_EXCLUDED_PATH =
      "Skipping audit for excluded path {} {}";

  /** DEBUG: GET — тело запроса не кэшируется. */
  public static final String SKIPPING_REQUEST_BODY_CACHE =
      "Skipping request body cache for {} {}";

  /** DEBUG: размер кэша тела запроса. */
  public static final String CACHED_REQUEST_BODY =
      "Cached request body ({} bytes) for {} {}";

  /** DEBUG: размер кэша тела ответа. */
  public static final String CAPTURED_RESPONSE_BODY =
      "Captured response body ({} bytes) for {} {}";

  /** WARN: тело запроса превысило лимит аудита. */
  public static final String REQUEST_BODY_EXCEEDS_LIMIT =
      "Request body exceeds the audit buffer limit and will not be extracted";

  /** WARN: тело ответа превысило лимит аудита. */
  public static final String RESPONSE_BODY_EXCEEDS_LIMIT =
      "Response body exceeds the audit buffer limit and will not be extracted";

  /** DEBUG: аудит после ошибки цепочки фильтров. */
  public static final String AUDITING_FAILED_REQUEST =
      "Auditing request after pipeline error for {} {}";

  /** DEBUG: на обмене нет {@code HandlerMethod}. */
  public static final String NO_HANDLER_METHOD =
      "No HandlerMethod on the exchange; skipping audit for {} {}";

  /** DEBUG: для контроллера и метода нет YAML-события. */
  public static final String NO_AUDIT_MAPPING =
      "No audit mapping for {}.{}";

  /** DEBUG: событие найдено по handler method. */
  public static final String RESOLVED_AUDIT_EVENT =
      "Resolved audit event '{}' for {}.{}";

  /** WARN: ни одно YAML-событие не подошло под статус или условия. */
  public static final String NO_MATCHING_AUDIT_EVENT =
      "No matching audit event for {}.{}";

  /** INFO: экстракторы привязаны к бину контроллера. */
  public static final String BINDING_EXTRACTORS =
      "Binding audit extractors for controller bean '{}' ({})";

  /** DEBUG: экстракторы настроены для класса контроллера. */
  public static final String CONFIGURED_EXTRACTORS =
      "Configured audit extractors for controller {}";

  /** INFO: регистрация метамодели на старте. */
  public static final String METAMODEL_REGISTERING =
      "Registering audit metamodel version '{}' module '{}' ({} events)";

  /** INFO: метамодель зарегистрирована, {@code {}} — хеш. */
  public static final String METAMODEL_REGISTERED =
      "Successfully registered audit metamodel with hash '{}'";

  /** ERROR: регистрация метамодели не удалась. */
  public static final String METAMODEL_REGISTER_FAILED =
      "Failed to register audit metamodel";

  /** Текст ошибки сериализации метамодели для DEBUG. */
  public static final String FAILED_TO_SERIALIZE_METAMODEL =
      "Could not serialize audit metamodel for debug logging";

  /** DEBUG: имена событий метамодели. */
  public static final String METAMODEL_EVENT_NAMES =
      "Audit metamodel events: {}";

  /** DEBUG: JSON-payload метамодели. */
  public static final String AUDIT_METAMODEL_PAYLOAD =
      "Audit metamodel payload:\n{}";

  /** INFO: отправка события в АС Единый Аудит. */
  public static final String SENDING_AUDIT_EVENT =
      "Sending audit event '{}'";

  /** INFO: событие отправлено, {@code {}} — идентификатор. */
  public static final String SENT_AUDIT_EVENT =
      "Successfully sent audit event '{}' with id '{}'";

  /** ERROR: отправка события не удалась. */
  public static final String AUDIT_EVENT_SEND_FAILED =
      "Failed to send audit event '{}'";

  /** Текст ошибки сериализации события для DEBUG. */
  public static final String FAILED_TO_SERIALIZE_EVENT =
      "Could not serialize audit event for debug logging";

  /** DEBUG: JSON-payload события. */
  public static final String AUDIT_EVENT_PAYLOAD =
      "Audit event payload:\n{}";

  /** DEBUG: тело запроса пустое, JSON не извлекается. */
  public static final String EMPTY_REQUEST_BODY =
      "Request body is empty; skipping JSON extraction";

  /** DEBUG: тело ответа пустое, JSON не извлекается. */
  public static final String EMPTY_RESPONSE_BODY =
      "Response body is empty; skipping JSON extraction";

  /** DEBUG: поле JSON не найдено по имени. */
  public static final String JSON_FIELD_NOT_FOUND =
      "JSON field '{}' not found";

  /** DEBUG: поле JSON не найдено по JSON Pointer. */
  public static final String JSON_POINTER_NOT_FOUND =
      "JSON field '{}' not found at pointer '{}'";

  /** WARN: не удалось разобрать JSON-тело. */
  public static final String FAILED_TO_PARSE_JSON =
      "Failed to parse JSON body";

  /** WARN: не удалось построить {@code JsonNode} из тела. */
  public static final String FAILED_TO_CREATE_JSON_NODE =
      "Failed to create JsonNode from request/response body";

  /** WARN: не удалось сериализовать JSON-узел в строку. */
  public static final String FAILED_TO_CONVERT_JSON_NODE =
      "Failed to convert JSON node to string";

  /** WARN: не удалось замаскировать поле JSON. */
  public static final String FAILED_TO_MASK_FIELD =
      "Failed to mask JSON field '{}'";

  /** DEBUG: пустой JSON-массив пропускается при поиске поля. */
  public static final String SKIPPING_EMPTY_JSON_ARRAY =
      "Skipping empty JSON array at '{}'";

  /** DEBUG: поиск поля продолжается внутри массива из одного элемента. */
  public static final String DESCENDING_SINGLE_ELEMENT_ARRAY =
      "Descending into single-element JSON array at '{}'";

  /** DEBUG: массив из нескольких элементов пропускается при поиске поля. */
  public static final String SKIPPING_MULTI_ELEMENT_ARRAY =
      "Skipping JSON array with {} elements at '{}'";

  /** DEBUG: найдено JSON-поле-массив. */
  public static final String FOUND_JSON_ARRAY_FIELD =
      "Found JSON array field '{}'";

  /** DEBUG: найдено JSON-поле-объект. */
  public static final String FOUND_JSON_OBJECT_FIELD =
      "Found JSON object field '{}'";

  /** DEBUG: нет claim {@code sub}, используется заглушка логина. */
  public static final String NO_JWT_SUB_CLAIM =
      "No JWT 'sub' claim; using default user login";

  /** DEBUG: нет claims имени, используется заглушка ФИО. */
  public static final String NO_JWT_NAME_CLAIMS =
      "No JWT given_name/family_name claims; using default user name";

  /** DEBUG: нет claim {@code sid}, используется заглушка узла пользователя. */
  public static final String NO_JWT_SID_CLAIM =
      "No JWT 'sid' claim; using default user node";

  /** WARN: не удалось вычислить идентификатор узла приложения. */
  public static final String UNABLE_TO_RESOLVE_NODE_ID =
      "Unable to resolve application node id";

  /** DEBUG: не удалось извлечь значение условия события. */
  public static final String FAILED_TO_EXTRACT_CONDITION =
      "Failed to extract condition value for field '{}' of event '{}'";

  /** DEBUG: у оператора условия список expected равен {@code null}. */
  public static final String CONDITION_OPERATOR_NULL_EXPECTED =
      "Condition operator {}: expected values are null";

  /** DEBUG: ожидаемый размер коллекции не является целым числом. */
  public static final String CONDITION_OPERATOR_INVALID_INTEGER =
      "Condition operator {}: expected value '{}' is not a valid integer";

  /** DEBUG: результат вычисления оператора условия. */
  public static final String CONDITION_OPERATOR_EVALUATED =
      "Condition operator {} evaluated to {} (actual='{}', expected={})";
}
