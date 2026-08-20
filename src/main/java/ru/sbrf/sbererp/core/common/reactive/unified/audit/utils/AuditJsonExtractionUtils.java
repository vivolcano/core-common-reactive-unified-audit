package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

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
 * <p>
 * Тела читаются из атрибутов {@link AuditExchangeAttributeNames}; MIME — JSON / problem+json / text.
 */
@Slf4j
@UtilityClass
public final class AuditJsonExtractionUtils {

  /** Jackson-маппер для разбора и сериализации тел аудита. */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Парсит тело HTTP-запроса в строку JSON из кэша обмена.
   *
   * @param exchange текущий обмен.
   * @return строка JSON или {@code null}, если тело отсутствует или MIME-тип не поддерживается.
   */
  public static String parseRequestBody(ServerWebExchange exchange) {
    byte[] body = cachedBody(exchange, AuditExchangeAttributeNames.CACHED_REQUEST_BODY);
    MediaType contentType = exchange.getRequest().getHeaders().getContentType();
    if (!isExtractableBody(body, contentType)) {
      if (ObjectUtils.isEmpty(body)) {
        log.debug(AuditLogMessages.EMPTY_REQUEST_BODY);
      }
      return null;
    }
    return new String(body, StandardCharsets.UTF_8);
  }

  /**
   * Парсит тело HTTP-ответа в строку JSON из кэша обмена.
   *
   * @param exchange текущий обмен.
   * @return строка JSON или {@code null}, если тело отсутствует или MIME-тип не поддерживается.
   */
  public static String parseResponseBody(ServerWebExchange exchange) {
    byte[] body = cachedBody(exchange, AuditExchangeAttributeNames.CACHED_RESPONSE_BODY);
    MediaType contentType = exchange.getResponse().getHeaders().getContentType();
    if (!isExtractableBody(body, contentType)) {
      if (ObjectUtils.isEmpty(body)) {
        log.debug(AuditLogMessages.EMPTY_RESPONSE_BODY);
      }
      return null;
    }
    return new String(body, StandardCharsets.UTF_8);
  }

  /**
   * Читает кэшированное тело из атрибутов обмена.
   *
   * @param exchange      текущий обмен.
   * @param attributeName ключ атрибута.
   * @return байты тела или пустой массив.
   */
  private static byte[] cachedBody(ServerWebExchange exchange, String attributeName) {
    Object stored = exchange.getAttribute(attributeName);
    return stored instanceof byte[] bytes ? bytes : AuditExchangeAttributeNames.EMPTY_BODY;
  }

  /**
   * Проверяет, что тело непустое и MIME-тип подходит для извлечения JSON/текста.
   *
   * @param body        байты тела.
   * @param contentType тип содержимого.
   * @return {@code true}, если тело можно разбирать.
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
   * Объект/массив маскируется по {@link ParamHolder#masks()}.
   *
   * @param jsonString JSON тела; {@code null} даёт {@code null}.
   * @param holder     имя/ключ поля и опциональные маски.
   * @return текстовое значение, JSON узла или {@code null}, если поле не найдено.
   */
  public static String extractFieldValue(String jsonString, Holder holder) {
    String fieldName = Objects.requireNonNullElse(holder.getKey(), holder.getName());
    JsonPointer pointer = findFieldPath(jsonString, fieldName);
    if (Objects.isNull(pointer)) {
      log.debug(AuditLogMessages.JSON_FIELD_NOT_FOUND, fieldName);
      return null;
    }
    JsonNode rootNode = readTree(jsonString);
    JsonNode fieldNode = Objects.requireNonNull(rootNode).at(pointer);
    if (fieldNode.isMissingNode()) {
      log.debug(AuditLogMessages.JSON_POINTER_NOT_FOUND, fieldName, pointer);
      return null;
    }
    JsonNode processedNode = processFieldForExtraction(fieldNode, fieldName);
    if (holder instanceof ParamHolder paramHolder) {
      if (ObjectUtils.isEmpty(paramHolder.masks())) {
        return convertNodeToString(processedNode);
      }
      return applyMasking(processedNode, paramHolder.masks());
    }
    return convertNodeToString(processedNode);
  }

  /**
   * Подготавливает JSON-строку, применяя маскировку указанных полей.
   *
   * <p>Используется, когда нужно передать всё тело как один параметр события, но с частичным
   * скрытием данных.
   *
   * @param jsonString JSON-строка, которую нужно обработать.
   * @param holder     {@link Holder} с опциональными масками {@link ParamHolder#masks()}.
   * @return обработанная JSON-строка или исходная, если произошла ошибка.
   */
  public static String preparationString(String jsonString, Holder holder) {
    try {
      JsonNode rootNode = OBJECT_MAPPER.readTree(jsonString);
      if (Objects.isNull(rootNode) || !rootNode.isObject()) {
        return null;
      }
      if (holder instanceof ParamHolder paramHolder) {
        List<String> masks = paramHolder.masks();
        if (ObjectUtils.isNotEmpty(masks)) {
          maskFields((ObjectNode) rootNode, masks);
        }
      }
      return OBJECT_MAPPER.writeValueAsString(rootNode);
    } catch (Exception e) {
      log.warn(AuditLogMessages.FAILED_TO_PARSE_JSON, e);
      return jsonString;
    }
  }

  /**
   * Извлекает параметры пагинации (страница, размер страницы, сортировка) из HTTP-запроса.
   *
   * @param request HTTP-запрос, из которого извлекаются параметры.
   * @return строка формата page/size/sort.
   */
  public static String getPageableParams(ServerHttpRequest request) {
    MultiValueMap<String, String> queryParams = request.getQueryParams();
    String page = queryParams.getFirst(AuditHttpConstants.PAGE);
    String size = queryParams.getFirst(AuditHttpConstants.SIZE);
    List<String> sortParams = queryParams.get(AuditHttpConstants.SORT);
    String sortLog = formatSortParams(sortParams);
    return String.format(AuditHttpConstants.PAGEABLE_STRING_PATTERN, page, size, sortLog);
  }

  /**
   * Форматирует значения параметра {@code sort} для логирования пагинации.
   *
   * @param sortParams значения query-параметра sort.
   * @return строка сортировки или литерал {@code null}.
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
   * Обрезает пробелы параметра сортировки либо возвращает литерал {@code null}.
   *
   * @param param значение sort.
   * @return обрезанная строка или {@code "null"}.
   */
  private static String trimOrNullLiteral(String param) {
    return Objects.isNull(param) ? AuditTextConstants.STRING_LITERAL_NULL : param.trim();
  }

  // Приватные методы для заполнения параметров события

  /**
   * Ищет в JSON-дереве путь до указанного поля, учитывая коллекции.
   *
   * <p>Поиск выполняется с учётом следующих правил для коллекций:
   * <ul>
   *   <li>пустая коллекция — поиск внутри не выполняется;</li>
   *   <li>коллекция с одним элементом — поиск продолжается внутри единственного элемента;</li>
   *   <li>коллекция с несколькими элементами — поиск внутри коллекции не выполняется.</li>
   * </ul>
   *
   * @param jsonString JSON-строка, в которой ищется поле.
   * @param fieldName  имя поля, которое нужно найти.
   * @return {@link JsonPointer}, указывающий на путь к полю, или {@code null}, если поле не найдено.
   */
  private static JsonPointer findFieldPath(String jsonString, String fieldName) {
    JsonNode rootNode = readTree(jsonString);
    if (Objects.isNull(rootNode) || !rootNode.isObject()) {
      return null;
    }
    JsonPointer pointer =
        findIntoDepthFieldPath(rootNode, fieldName, JsonPointer.compile(AuditTextConstants.EMPTY_STRING));
    if (Objects.isNull(pointer)) {
      return null;
    }
    return pointer;
  }

  /**
   * Рекурсивно ищет указанный путь до поля в JSON-структуре. Не заходит внутрь коллекций с более
   * чем одним элементом.
   *
   * @param node        текущий JSON-узел {@link JsonNode} для поиска.
   * @param fieldName   имя искомого поля.
   * @param currentPath текущий путь в формате JSON Pointer.
   * @return {@link JsonPointer} путь к найденному полю или {@code null}, если поле не найдено.
   */
  private static JsonPointer findIntoDepthFieldPath(
      JsonNode node, String fieldName, JsonPointer currentPath) {

    if (node.isObject()) {
      for (String currentField : node.propertyNames()) {
        JsonNode childNode = node.get(currentField);
        JsonPointer newPath = JsonPointer.valueOf(currentPath + AuditTextConstants.SLASH + currentField);

        // 1. Если нашли искомое поле - возвращаем путь
        if (currentField.equals(fieldName)) {
          return newPath;
        }

        // 2. Рекурсивно ищем в дочерних узлах
        JsonPointer foundPath = findIntoDepthFieldPath(childNode, fieldName, newPath);
        if (Objects.nonNull(foundPath)) {
          return foundPath;
        }
      }
    } else if (node.isArray()) {
      // Обрабатываем массив
      ArrayNode arrayNode = (ArrayNode) node;
      int size = arrayNode.size();

      if (size == AuditNumericConstants.ZERO) {
        // Пустая коллекция - не ищем внутри
        log.debug(AuditLogMessages.SKIPPING_EMPTY_JSON_ARRAY, currentPath);
        return null;
      } else if (size == AuditNumericConstants.ONE) {
        log.debug(AuditLogMessages.DESCENDING_SINGLE_ELEMENT_ARRAY, currentPath);
        return findIntoDepthFieldPath(
            arrayNode.get(AuditNumericConstants.ZERO),
            fieldName,
            JsonPointer.valueOf(currentPath + AuditTextConstants.SLASH + AuditTextConstants.JSON_POINTER_FIRST_INDEX)
        );
      } else {
        log.debug(AuditLogMessages.SKIPPING_MULTI_ELEMENT_ARRAY, size, currentPath);
        return null;
      }
    }
    return null;
  }

  /**
   * Обрабатывает узел поля для извлечения значения. Для коллекций возвращает всю коллекцию, для
   * примитивов - значение.
   *
   * @param fieldNode найденный JSON-узел поля.
   * @param fieldName имя поля.
   * @return обработанный JSON-узел.
   */
  private static JsonNode processFieldForExtraction(JsonNode fieldNode, String fieldName) {
    if (fieldNode.isArray()) {
      log.debug(AuditLogMessages.FOUND_JSON_ARRAY_FIELD, fieldName);
      return fieldNode;
    }

    if (fieldNode.isObject()) {
      log.debug(AuditLogMessages.FOUND_JSON_OBJECT_FIELD, fieldName);
      return fieldNode;
    }

    return fieldNode;
  }

  /**
   * Преобразует JSON-узел в строку. Для примитивных значений возвращает текстовое представление,
   * для объектов и массивов - JSON-строку.
   *
   * @param node JSON-узел для преобразования.
   * @return строковое представление узла или {@code null}, если узел {@code null}.
   */
  private static String convertNodeToString(JsonNode node) {
    if (Objects.isNull(node) || node.isNull()) {
      return null;
    }
    if (node.isValueNode()) {
      return node.asText();
    }
    // Для объектов и массивов возвращаем JSON-строку
    try {
      return OBJECT_MAPPER.writeValueAsString(node);
    } catch (JacksonException e) {
      log.warn(AuditLogMessages.FAILED_TO_CONVERT_JSON_NODE, e);
      return node.toString();
    }
  }

  /**
   * Применяет маскировку к JSON-узлу согласно указанным путям полей.
   *
   * @param node  JSON-узел для маскировки.
   * @param masks список путей до полей, которые нужно скрыть.
   * @return строка с замаскированным JSON-узлом.
   */
  private static String applyMasking(JsonNode node, List<String> masks) {
    if (node.isObject()) {
      ObjectNode nodeToMask = (ObjectNode) node.deepCopy();
      maskFields(nodeToMask, masks);
      return convertNodeToString(nodeToMask);
    } else if (node.isArray()) {
      ArrayNode arrayNode = (ArrayNode) node.deepCopy();
      maskArrayElements(arrayNode, masks);
      return convertNodeToString(arrayNode);
    }
    return convertNodeToString(node);
  }

  /**
   * Маскирует объекты внутри JSON-массива.
   *
   * @param arrayNode массив для маскирования.
   * @param masks     пути скрываемых полей.
   */
  private static void maskArrayElements(ArrayNode arrayNode, List<String> masks) {
    arrayNode.forEach(element -> maskObjectElement(element, masks));
  }

  /**
   * Маскирует поля, если элемент массива является объектом.
   *
   * @param element элемент массива.
   * @param masks   пути скрываемых полей.
   */
  private static void maskObjectElement(JsonNode element, List<String> masks) {
    if (element.isObject()) {
      maskFields((ObjectNode) element, masks);
    }
  }

  /**
   * Читает JSON-строку и создает объект JSONNode.
   *
   * @param jsonString JSON-строка, которую нужно прочитать.
   * @return {@link JsonNode} или {@code null}, если чтение строки невозможно.
   */
  private static JsonNode readTree(String jsonString) {
    try {
      return OBJECT_MAPPER.readTree(jsonString);
    } catch (JacksonException | IllegalArgumentException e) {
      log.warn(AuditLogMessages.FAILED_TO_CREATE_JSON_NODE, e);
      return null;
    }
  }

  // Приватные методы для реализации с маскированием полей в теле и передачи тела целиком в событие
  // как одного
  // параметра

  /**
   * Применяет маскировку полей в JSON-объекте согласно указанным путям.
   *
   * @param node  JSON-объект, в котором будут скрыты поля.
   * @param masks список путей до полей, которые нужно скрыть.
   */
  private static void maskFields(ObjectNode node, List<String> masks) {
    if (Objects.isNull(node) || ObjectUtils.isEmpty(masks)) {
      return;
    }
    masks.forEach(path -> maskSinglePath(node, path));
  }

  /**
   * Маскирует одно поле по пути, игнорируя ошибки конкретного пути.
   *
   * @param node JSON-объект.
   * @param path путь маскирования.
   */
  private static void maskSinglePath(ObjectNode node, String path) {
    try {
      if (path.startsWith(AuditTextConstants.SLASH)) {
        maskFieldUsingPointer(node, JsonPointer.compile(path));
        return;
      }
      maskFieldInCollections(node, path);
    } catch (Exception exception) {
      log.warn(AuditLogMessages.FAILED_TO_MASK_FIELD, path, exception);
    }
  }

  /**
   * Рекурсивно проходит по JSON-объектам и массивам, скрывая указанные поля.
   *
   * @param node      начальный JSON-узел, с которым начинается обход.
   * @param fieldPath путь до поля, которое нужно скрыть.
   */
  private static void maskFieldInCollections(JsonNode node, String fieldPath) {
    String[] pathParts = fieldPath.split(AuditTextConstants.JSON_PATH_DOT_SPLIT_REGEX);
    processNode(node, pathParts, AuditNumericConstants.ZERO);
  }

  /**
   * Рекурсивно обрабатывает узлы JSON-объекта или массива, скрывая указанное поле.
   *
   * @param node         текущий JSON-узел, над которым производится обработка.
   * @param pathParts    массив частей пути до поля, которое нужно скрыть.
   * @param currentIndex индекс текущей части пути.
   */
  private static void processNode(JsonNode node, String[] pathParts, int currentIndex) {
    if (Objects.isNull(node) || currentIndex >= pathParts.length) {
      return;
    }

    String currentField = pathParts[currentIndex];
    boolean isLastField = currentIndex == pathParts.length - AuditNumericConstants.ONE;

    if (node.isArray()) {
      // Обработка массива
      for (JsonNode arrayItem : node) {
        if (isLastField) {
          if (arrayItem.isObject()) {
            ((ObjectNode) arrayItem).remove(currentField);
          }
        } else {
          processNode(arrayItem.get(currentField), pathParts, currentIndex + AuditNumericConstants.ONE);
        }
      }
    } else if (node.isObject()) {
      // Обработка объекта
      if (isLastField) {
        ((ObjectNode) node).remove(currentField);
      } else if (node.has(currentField)) {
        processNode(node.get(currentField), pathParts, currentIndex + AuditNumericConstants.ONE);
      }
    }
  }

  /**
   * Прямо удаляет поле из родительского JSON-узла по указанному JSONPointer.
   *
   * @param node    родительский JSON-объект, содержащий поле.
   * @param pointer {@link JsonPointer}, указывающий на поле, которое нужно удалить.
   */
  private static void maskFieldUsingPointer(ObjectNode node, JsonPointer pointer) {
    JsonNode parentNode = node.at(pointer.head());
    String fieldName = pointer.last().getMatchingProperty();

    if (Objects.nonNull(parentNode)) {
      if (parentNode.isArray()) {
        for (JsonNode arrayItem : parentNode) {
          if (arrayItem.isObject() && arrayItem.has(fieldName)) {
            ((ObjectNode) arrayItem).remove(fieldName);
          }
        }
      } else if (parentNode.isObject() && parentNode.has(fieldName)) {
        ((ObjectNode) parentNode).remove(fieldName);
      }
    }
  }
}
