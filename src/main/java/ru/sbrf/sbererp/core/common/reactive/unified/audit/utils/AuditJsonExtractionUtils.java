package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import io.vavr.control.Try;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.Holder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ParamHolder;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Извлечение и маскирование JSON-полей из кэшированных тел {@link ServerWebExchange}.
 */
@Slf4j
@UtilityClass
public final class AuditJsonExtractionUtils {

  /** Jackson-маппер для разбора и сериализации тел аудита. */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * @param exchange текущий обмен
   * @return JSON-строка тела запроса либо {@code null}
   */
  public static String parseRequestBody(ServerWebExchange exchange) {
    final byte[] body = cachedBody(exchange, AuditExchangeAttributeNames.CACHED_REQUEST_BODY);
    final MediaType contentType = exchange.getRequest().getHeaders().getContentType();
    if (!isExtractableBody(body, contentType)) {
      if (ObjectUtils.isEmpty(body)) {
        log.debug(AuditLogMessages.EMPTY_REQUEST_BODY);
      }
      return null;
    }
    return new String(body, StandardCharsets.UTF_8);
  }

  /**
   * @param exchange текущий обмен
   * @return JSON-строка тела ответа либо {@code null}
   */
  public static String parseResponseBody(ServerWebExchange exchange) {
    final byte[] body = cachedBody(exchange, AuditExchangeAttributeNames.CACHED_RESPONSE_BODY);
    final MediaType contentType = exchange.getResponse().getHeaders().getContentType();
    if (!isExtractableBody(body, contentType)) {
      if (ObjectUtils.isEmpty(body)) {
        log.debug(AuditLogMessages.EMPTY_RESPONSE_BODY);
      }
      return null;
    }
    return new String(body, StandardCharsets.UTF_8);
  }

  /**
   * @param exchange      текущий обмен
   * @param attributeName ключ атрибута
   * @return байты тела либо пустой массив
   */
  private static byte[] cachedBody(ServerWebExchange exchange, String attributeName) {
    final Object stored = exchange.getAttribute(attributeName);
    return stored instanceof byte[] bytes ? bytes : AuditExchangeAttributeNames.EMPTY_BODY;
  }

  /**
   * @param body        байты тела
   * @param contentType тип содержимого
   * @return {@code true}, если тело можно разбирать
   */
  private static boolean isExtractableBody(byte[] body, MediaType contentType) {
    if (ObjectUtils.isEmpty(body) || Objects.isNull(contentType)) {
      return false;
    }
    return contentType.isCompatibleWith(MediaType.APPLICATION_JSON)
        || contentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        || contentType.isCompatibleWith(MediaType.TEXT_PLAIN);
  }

  /**
   * Ищет поле по имени рекурсивно. В массив заходит только если в нём ровно один элемент.
   *
   * @param jsonString JSON тела
   * @param holder     имя/ключ поля и опциональные маски
   * @return текстовое значение, JSON узла либо {@code null}
   */
  public static String extractFieldValue(String jsonString, Holder holder) {
    final String fieldName = Objects.requireNonNullElse(holder.getKey(), holder.getName());
    final JsonPointer pointer = findFieldPath(jsonString, fieldName);
    if (Objects.isNull(pointer)) {
      log.debug(AuditLogMessages.JSON_FIELD_NOT_FOUND, fieldName);
      return null;
    }
    final JsonNode rootNode = readTree(jsonString);
    final JsonNode fieldNode = Objects.requireNonNull(rootNode).at(pointer);
    if (fieldNode.isMissingNode()) {
      log.debug(AuditLogMessages.JSON_POINTER_NOT_FOUND, fieldName, pointer);
      return null;
    }
    final JsonNode processedNode = processFieldForExtraction(fieldNode, fieldName);
    return holder instanceof ParamHolder paramHolder && ObjectUtils.isNotEmpty(paramHolder.masks())
        ? applyMasking(processedNode, paramHolder.masks())
        : convertNodeToString(processedNode);
  }

  /**
   * Маскирует поля {@link ParamHolder#masks()} и возвращает тело как один параметр события.
   *
   * @param jsonString JSON-строка
   * @param holder     держатель с опциональными масками
   * @return обработанная JSON-строка либо исходная при ошибке
   */
  public static String preparationString(String jsonString, Holder holder) {
    return Try.of(() -> {
          final JsonNode rootNode = OBJECT_MAPPER.readTree(jsonString);
          if (Objects.isNull(rootNode) || !rootNode.isObject()) {
            return null;
          }
          if (holder instanceof ParamHolder paramHolder && ObjectUtils.isNotEmpty(paramHolder.masks())) {
            maskFields((ObjectNode) rootNode, paramHolder.masks());
          }
          return OBJECT_MAPPER.writeValueAsString(rootNode);
        })
        .onFailure(exception -> log.warn(AuditLogMessages.FAILED_TO_PARSE_JSON, exception))
        .getOrElse(jsonString);
  }

  /**
   * @param request HTTP-запрос
   * @return строка формата page/size/sort
   */
  public static String getPageableParams(ServerHttpRequest request) {
    final MultiValueMap<String, String> queryParams = request.getQueryParams();
    final String page = queryParams.getFirst(AuditHttpConstants.PAGE);
    final String size = queryParams.getFirst(AuditHttpConstants.SIZE);
    final String sortLog = formatSortParams(queryParams.get(AuditHttpConstants.SORT));
    return String.format(AuditHttpConstants.PAGEABLE_STRING_PATTERN, page, size, sortLog);
  }

  /**
   * @param sortParams значения query-параметра {@code sort}
   * @return строка сортировки либо литерал {@code null}
   */
  private static String formatSortParams(List<String> sortParams) {
    if (ObjectUtils.isEmpty(sortParams)) {
      return AuditTextConstants.STRING_LITERAL_NULL;
    }
    return sortParams.stream()
        .map(AuditJsonExtractionUtils::trimOrNullLiteral)
        .collect(Collectors.joining(AuditTextConstants.COMMA_WITH_SPACE));
  }

  /**
   * @param param значение {@code sort}
   * @return обрезанная строка либо {@code "null"}
   */
  private static String trimOrNullLiteral(String param) {
    return Objects.isNull(param) ? AuditTextConstants.STRING_LITERAL_NULL : param.trim();
  }

  /**
   * Ищет путь до поля. В массив заходит только при ровно одном элементе.
   *
   * @param jsonString JSON-строка
   * @param fieldName  имя поля
   * @return {@link JsonPointer} либо {@code null}
   */
  private static JsonPointer findFieldPath(String jsonString, String fieldName) {
    final JsonNode rootNode = readTree(jsonString);
    return Objects.isNull(rootNode) || !rootNode.isObject()
        ? null
        : findIntoDepthFieldPath(rootNode, fieldName, JsonPointer.compile(AuditTextConstants.EMPTY_STRING));
  }

  /**
   * @param node        текущий JSON-узел
   * @param fieldName   имя искомого поля
   * @param currentPath текущий JSON Pointer
   * @return путь к полю либо {@code null}
   */
  private static JsonPointer findIntoDepthFieldPath(
      JsonNode node, String fieldName, JsonPointer currentPath) {

    if (node.isObject()) {
      for (String currentField : node.propertyNames()) {
        final JsonNode childNode = node.get(currentField);
        final JsonPointer newPath = JsonPointer.valueOf(currentPath + AuditTextConstants.SLASH + currentField);
        if (currentField.equals(fieldName)) {
          return newPath;
        }
        final JsonPointer foundPath = findIntoDepthFieldPath(childNode, fieldName, newPath);
        if (Objects.nonNull(foundPath)) {
          return foundPath;
        }
      }
      return null;
    }
    if (!node.isArray()) {
      return null;
    }
    final ArrayNode arrayNode = (ArrayNode) node;
    final int size = arrayNode.size();
    if (size == AuditNumericConstants.ZERO) {
      log.debug(AuditLogMessages.SKIPPING_EMPTY_JSON_ARRAY, currentPath);
      return null;
    }
    if (size != AuditNumericConstants.ONE) {
      log.debug(AuditLogMessages.SKIPPING_MULTI_ELEMENT_ARRAY, size, currentPath);
      return null;
    }
    log.debug(AuditLogMessages.DESCENDING_SINGLE_ELEMENT_ARRAY, currentPath);
    return findIntoDepthFieldPath(
        arrayNode.get(AuditNumericConstants.ZERO),
        fieldName,
        JsonPointer.valueOf(currentPath + AuditTextConstants.SLASH + AuditTextConstants.JSON_POINTER_FIRST_INDEX)
    );
  }

  /**
   * @param fieldNode найденный JSON-узел
   * @param fieldName имя поля
   * @return тот же узел
   */
  private static JsonNode processFieldForExtraction(JsonNode fieldNode, String fieldName) {
    if (fieldNode.isArray()) {
      log.debug(AuditLogMessages.FOUND_JSON_ARRAY_FIELD, fieldName);
    } else if (fieldNode.isObject()) {
      log.debug(AuditLogMessages.FOUND_JSON_OBJECT_FIELD, fieldName);
    }
    return fieldNode;
  }

  /**
   * @param node JSON-узел
   * @return текстовое значение, JSON-строка либо {@code null}
   */
  private static String convertNodeToString(JsonNode node) {
    if (Objects.isNull(node) || node.isNull()) {
      return null;
    }
    return node.isValueNode()
        ? node.asText()
        : Try.of(() -> OBJECT_MAPPER.writeValueAsString(node))
            .onFailure(exception -> log.warn(AuditLogMessages.FAILED_TO_CONVERT_JSON_NODE, exception))
            .getOrElse(node::toString);
  }

  /**
   * @param node  JSON-узел
   * @param masks пути скрываемых полей
   * @return строка с замаскированным JSON
   */
  private static String applyMasking(JsonNode node, List<String> masks) {
    if (node.isObject()) {
      final ObjectNode nodeToMask = (ObjectNode) node.deepCopy();
      maskFields(nodeToMask, masks);
      return convertNodeToString(nodeToMask);
    }
    if (node.isArray()) {
      final ArrayNode arrayNode = (ArrayNode) node.deepCopy();
      maskArrayElements(arrayNode, masks);
      return convertNodeToString(arrayNode);
    }
    return convertNodeToString(node);
  }

  private static void maskArrayElements(ArrayNode arrayNode, List<String> masks) {
    arrayNode.forEach(element -> maskObjectElement(element, masks));
  }

  private static void maskObjectElement(JsonNode element, List<String> masks) {
    if (element.isObject()) {
      maskFields((ObjectNode) element, masks);
    }
  }

  /**
   * @param jsonString JSON-строка
   * @return {@link JsonNode} либо {@code null}
   */
  private static JsonNode readTree(String jsonString) {
    return Try.of(() -> OBJECT_MAPPER.readTree(jsonString))
        .recover(JacksonException.class, exception -> {
          log.warn(AuditLogMessages.FAILED_TO_CREATE_JSON_NODE, exception);
          return null;
        })
        .recover(IllegalArgumentException.class, exception -> {
          log.warn(AuditLogMessages.FAILED_TO_CREATE_JSON_NODE, exception);
          return null;
        })
        .get();
  }

  /**
   * @param node  JSON-объект
   * @param masks пути скрываемых полей
   */
  private static void maskFields(ObjectNode node, List<String> masks) {
    if (Objects.isNull(node) || ObjectUtils.isEmpty(masks)) {
      return;
    }
    masks.forEach(path -> maskSinglePath(node, path));
  }

  /**
   * @param node JSON-объект
   * @param path путь маскирования
   */
  private static void maskSinglePath(ObjectNode node, String path) {
    Try.run(() -> {
          if (path.startsWith(AuditTextConstants.SLASH)) {
            maskFieldUsingPointer(node, JsonPointer.compile(path));
            return;
          }
          maskFieldInCollections(node, path);
        })
        .onFailure(exception -> log.warn(AuditLogMessages.FAILED_TO_MASK_FIELD, path, exception));
  }

  /**
   * @param node      начальный JSON-узел
   * @param fieldPath путь до скрываемого поля
   */
  private static void maskFieldInCollections(JsonNode node, String fieldPath) {
    final String[] pathParts = fieldPath.split(AuditTextConstants.JSON_PATH_DOT_SPLIT_REGEX);
    processNode(node, pathParts, AuditNumericConstants.ZERO);
  }

  /**
   * @param node         текущий JSON-узел
   * @param pathParts    части пути до поля
   * @param currentIndex индекс текущей части пути
   */
  private static void processNode(JsonNode node, String[] pathParts, int currentIndex) {
    if (Objects.isNull(node) || currentIndex >= pathParts.length) {
      return;
    }
    final String currentField = pathParts[currentIndex];
    final boolean isLastField = currentIndex == pathParts.length - AuditNumericConstants.ONE;
    if (node.isArray()) {
      node.forEach(arrayItem -> {
        if (isLastField) {
          if (arrayItem.isObject()) {
            ((ObjectNode) arrayItem).remove(currentField);
          }
          return;
        }
        processNode(arrayItem.get(currentField), pathParts, currentIndex + AuditNumericConstants.ONE);
      });
      return;
    }
    if (!node.isObject()) {
      return;
    }
    if (isLastField) {
      ((ObjectNode) node).remove(currentField);
      return;
    }
    if (node.has(currentField)) {
      processNode(node.get(currentField), pathParts, currentIndex + AuditNumericConstants.ONE);
    }
  }

  /**
   * @param node    родительский JSON-объект
   * @param pointer {@link JsonPointer} скрываемого поля
   */
  private static void maskFieldUsingPointer(ObjectNode node, JsonPointer pointer) {
    final JsonNode parentNode = node.at(pointer.head());
    final String fieldName = pointer.last().getMatchingProperty();
    if (Objects.isNull(parentNode)) {
      return;
    }
    if (parentNode.isArray()) {
      parentNode.forEach(arrayItem -> {
        if (arrayItem.isObject() && arrayItem.has(fieldName)) {
          ((ObjectNode) arrayItem).remove(fieldName);
        }
      });
      return;
    }
    if (parentNode.isObject() && parentNode.has(fieldName)) {
      ((ObjectNode) parentNode).remove(fieldName);
    }
  }
}
