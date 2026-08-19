package ru.sbrf.sbererp.core.common.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * SLF4J message templates for the audit module.
 * <p>
 * Event and metamodel payloads are logged only at DEBUG. INFO carries identifiers
 * (event name, id, version) without request/response bodies or JWT contents.
 */
@UtilityClass
public final class AuditLogMessages {

  public static final String SKIPPING_EXCLUDED_PATH =
      "Skipping audit for excluded path {} {}";

  public static final String SKIPPING_REQUEST_BODY_CACHE =
      "Skipping request body cache for {} {}";

  public static final String CACHED_REQUEST_BODY =
      "Cached request body ({} bytes) for {} {}";

  public static final String CAPTURED_RESPONSE_BODY =
      "Captured response body ({} bytes) for {} {}";

  public static final String REQUEST_BODY_EXCEEDS_LIMIT =
      "Request body exceeds the audit buffer limit and will not be extracted";

  public static final String RESPONSE_BODY_EXCEEDS_LIMIT =
      "Response body exceeds the audit buffer limit and will not be extracted";

  public static final String AUDITING_FAILED_REQUEST =
      "Auditing request after pipeline error for {} {}";

  public static final String NO_HANDLER_METHOD =
      "No HandlerMethod on the exchange; skipping audit for {} {}";

  public static final String NO_AUDIT_MAPPING =
      "No audit mapping for {}.{}";

  public static final String RESOLVED_AUDIT_EVENT =
      "Resolved audit event '{}' for {}.{}";

  public static final String NO_MATCHING_AUDIT_EVENT =
      "No matching audit event for {}.{}";

  public static final String BINDING_EXTRACTORS =
      "Binding audit extractors for controller bean '{}' ({})";

  public static final String CONFIGURED_EXTRACTORS =
      "Configured audit extractors for controller {}";

  public static final String METAMODEL_REGISTERING =
      "Registering audit metamodel version '{}' module '{}' ({} events)";

  public static final String METAMODEL_REGISTERED =
      "Successfully registered audit metamodel with hash '{}'";

  public static final String METAMODEL_REGISTER_FAILED =
      "Failed to register audit metamodel";

  public static final String FAILED_TO_SERIALIZE_METAMODEL =
      "Could not serialize audit metamodel for debug logging";

  public static final String METAMODEL_EVENT_NAMES =
      "Audit metamodel events: {}";

  public static final String AUDIT_METAMODEL_PAYLOAD =
      "Audit metamodel payload:\n{}";

  public static final String SENDING_AUDIT_EVENT =
      "Sending audit event '{}'";

  public static final String SENT_AUDIT_EVENT =
      "Successfully sent audit event '{}' with id '{}'";

  public static final String AUDIT_EVENT_SEND_FAILED =
      "Failed to send audit event '{}'";

  public static final String FAILED_TO_SERIALIZE_EVENT =
      "Could not serialize audit event for debug logging";

  public static final String AUDIT_EVENT_PAYLOAD =
      "Audit event payload:\n{}";

  public static final String EMPTY_REQUEST_BODY =
      "Request body is empty; skipping JSON extraction";

  public static final String EMPTY_RESPONSE_BODY =
      "Response body is empty; skipping JSON extraction";

  public static final String JSON_FIELD_NOT_FOUND =
      "JSON field '{}' not found";

  public static final String JSON_POINTER_NOT_FOUND =
      "JSON field '{}' not found at pointer '{}'";

  public static final String FAILED_TO_PARSE_JSON =
      "Failed to parse JSON body";

  public static final String FAILED_TO_CREATE_JSON_NODE =
      "Failed to create JsonNode from request/response body";

  public static final String FAILED_TO_CONVERT_JSON_NODE =
      "Failed to convert JSON node to string";

  public static final String FAILED_TO_MASK_FIELD =
      "Failed to mask JSON field '{}'";

  public static final String SKIPPING_EMPTY_JSON_ARRAY =
      "Skipping empty JSON array at '{}'";

  public static final String DESCENDING_SINGLE_ELEMENT_ARRAY =
      "Descending into single-element JSON array at '{}'";

  public static final String SKIPPING_MULTI_ELEMENT_ARRAY =
      "Skipping JSON array with {} elements at '{}'";

  public static final String FOUND_JSON_ARRAY_FIELD =
      "Found JSON array field '{}'";

  public static final String FOUND_JSON_OBJECT_FIELD =
      "Found JSON object field '{}'";

  public static final String NO_JWT_SUB_CLAIM =
      "No JWT 'sub' claim; using default user login";

  public static final String NO_JWT_NAME_CLAIMS =
      "No JWT given_name/family_name claims; using default user name";

  public static final String NO_JWT_SID_CLAIM =
      "No JWT 'sid' claim; using default user node";

  public static final String UNABLE_TO_RESOLVE_NODE_ID =
      "Unable to resolve application node id";

  public static final String FAILED_TO_EXTRACT_CONDITION =
      "Failed to extract condition value for field '{}' of event '{}'";

  public static final String CONDITION_OPERATOR_NULL_EXPECTED =
      "Condition operator {}: expected values are null";

  public static final String CONDITION_OPERATOR_INVALID_INTEGER =
      "Condition operator {}: expected value '{}' is not a valid integer";

  public static final String CONDITION_OPERATOR_EVALUATED =
      "Condition operator {} evaluated to {} (actual='{}', expected={})";
}
