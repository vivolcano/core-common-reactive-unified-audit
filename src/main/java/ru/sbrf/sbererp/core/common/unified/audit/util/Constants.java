package ru.sbrf.sbererp.core.common.unified.audit.util;

import lombok.experimental.UtilityClass;

/**
 * Класс содержит набор универсальных констант, используемых в модуле аудита.
 * <p>
 * Все значения предназначены для обеспечения читаемости, унификации и стандартизации обработки
 * данных, взаимодействия с HTTP-запросами/ответами, JWT-токенами, метаданными и логированием.
 * <p>
 * Константы включают:
 * <ul>
 *   <li>Строковые разделители и символы</li>
 *   <li>Префиксы для параметров аудита</li>
 *   <li>Имена HTTP-заголовков и кук</li>
 *   <li>Специальные значения для неопределённых идентификаторов</li>
 *   <li>Имена параметров запроса и метаданных</li>
 *   <li>Шаблоны сообщений об ошибках</li>
 *   <li>Индексы и служебные значения</li>
 * </ul>
 * <p>
 * Использование констант позволяет избежать "магических строк" и чисел в коде,
 * упрощает поддержку и централизует управление часто используемыми значениями.
 */
@UtilityClass
public class Constants {

  /**
   * Пустая строка. Используется для инициализации строковых значений.
   */
  public static final String EMPTY_STRING = "";

  /**
   * Пробел.
   * <p>
   * Используется при форматировании строк, объединении значений или разборе текста.
   */
  public static final String SPACE = " ";

  /**
   * Запятая.
   * <p>
   * Используется как разделитель значений в строках (например, при объединении списка значений).
   */
  public static final String COMMA = ",";

  /**
   * Запятая с пробелом.
   * <p>
   * Используется для форматирования строк с перечислением значений с читаемым разделителем. Пример:
   * {@code "value1, value2, value3"}.
   */
  public static final String COMMA_WITH_SPACE = ", ";

  /**
   * Символ подчеркивания.
   * <p>
   * Используется как разделитель в именах параметров, префиксах и ключах.
   */
  public static final String UNDERSCORE = "_";

  /**
   * Символ слэша.
   * <p>
   * Используется в путях URL, разделителях путей файловой системы или в шаблонах.
   */
  public static final String SLASH = "/";

  /**
   * Символ подчеркивания в виде char.
   * <p>
   * Используется при работе с символами, например, в парсинге строк или генерации имён.
   */
  public static final char CHAR_UNDERSCORE = '_';

  /**
   * Символ дефиса в виде char.
   * <p>
   * Используется при разборе или генерации имён, идентификаторов, особенно в HTTP-заголовках.
   */
  public static final char CHAR_DASH = '-';

  /**
   * Префикс для JWT-токена. Формат: {@code jwt_claim_jti:%s}.
   */
  public static final String JWT_CLAIM_JTI_PREFIX = "jwt_claim_jti:%s";

  /**
   * Имя куки JSESSIONID.
   */
  public static final String JSESSIONID = "JSESSIONID";

  /**
   * Префикс для кук. Пример: {@code cookie_JSESSIONID}.
   */
  public static final String COOKIE_WITH_UNDERSCORE = "cookie_";

  /**
   * Название токена авторизации в заголовках (authtoken).
   */
  public static final String AUTHTOKEN = "Authtoken";

  /**
   * Имя хоста (HOSTNAME).
   */
  public static final String HOSTNAME = "HOSTNAME";

  /**
   * Имя пространства имён (например, в Kubernetes).
   */
  public static final String NAMESPACE = "NAMESPACE";

  /**
   * Неизвестный идентификатор узла.
   */
  public static final String UNKNOWN_NODE_ID = "UNKNOWN-NODE-ID";

  /**
   * Пользователь не определён.
   */
  public static final String NO_USER = "NO-USER";

  /** Неопределённое имя пользователя. */
  public static final String NO_USER_NAME = "NO-USER-NAME";

  /**
   * HTTP-заголовок для корреляционного идентификатора запроса.
   */
  public static final String X_CORRELATION_ID = "X-Correlation-ID";

  /**
   * Префикс для корреляционных параметров. Пример: {@code correlation_id}.
   */
  public static final String CORRELATION_WITH_UNDERSCORE = "correlation_";

  /**
   * HTTP-заголовок для идентификатора сессии.
   */
  public static final String X_SESSION_ID = "X-Session-ID";

  /**
   * Неопределенная сессия.
   */
  public static final String NO_SESSION = "NO-SESSION";

  /**
   * Имя параметра идентификатора запроса (request-id).
   */
  public static final String REQUEST_ID = "request-id";

  /**
   * Универсальный путь для всех URL-шаблонов.
   */
  public static final String ALL_URL_PATTERNS = "/*";

  /**
   * Метод HTTP GET.
   */
  public static final String GET_METHOD = "GET";

  /**
   * Полное имя класса Pageable из Spring Data.
   */
  public static final String PAGEABLE_CLASS_NAME = "org.springframework.data.domain.Pageable";

  /**
   * Параметр номера страницы (page).
   */
  public static final String PAGE = "page";

  /**
   * Параметр размера страницы (size).
   */
  public static final String SIZE = "size";

  /**
   * Параметр сортировки (sort).
   */
  public static final String SORT = "sort";

  /**
   * Литерал null в строковом представлении.
   */
  public static final String STRING_LITERAL_NULL = "null";

  /**
   * Шаблон строки для отображения объекта Pageable.
   */
  public static final String PAGEABLE_STRING_PATTERN = "{page=%s, size=%s, sort=[%s]}";

  /**
   * Метка "REQUEST".
   */
  public static final String REQUEST = "REQUEST";

  /**
   * Метка "RESPONSE".
   */
  public static final String RESPONSE = "RESPONSE";

  /**
   * Метка "CLAIMS".
   */
  public static final String CLAIMS = "CLAIMS";

  /**
   * Неопределенный идентификатор запроса.
   */
  public static final String NO_REQUEST_ID = "NO_REQUEST_ID";

  /**
   * Неопределенный sid JWT-токена.
   */
  public static final String NO_CLAIM_SID = "NO-CLAIM-SID";

  /**
   * Префикс для sid JWT-токена. Формат: {@code jwt_claim_sid:%s}.
   */
  public static final String JWT_CLAIM_SID_PREFIX = "jwt_claim_sid:%s";

  /**
   * Имя пользователя (given_name).
   */
  public static final String FIRST_NAME = "given_name";

  /**
   * Отчество (patronymic).
   */
  public static final String MIDDLE_NAME = "patronymic";

  /**
   * Фамилия (family_name).
   */
  public static final String LAST_NAME = "family_name";

  /**
   * Ошибка: compiledConditionChecker == null
   */
  public static final String COMPILED_CONDITION_CHECKER_IS_NULL = "В событии с именем %s compiledConditionChecker == null.";

  /**
   * Ошибка: не задан экстрактор для условия.
   */
  public static final String EXTRACTOR_IS_MISSING = "В событии %s в условии %s не задан экстрактор.";

  /**
   * Поле не найдено по пути.
   */
  public static final String FIELD_NOT_FOUND = "Поле {} не найдено по пути {}";

  /**
   * Невозможно преобразовать метамодель в строку.
   */
  public static final String ERROR_CONVERT_METAMODEL_TO_STRING = "Невозможно преобразовать метамодель в строку";

  /**
   * Невозможно преобразовать событие аудита в строку.
   */
  public static final String ERROR_CONVERT_EVENT_TO_STRING = "Невозможно преобразовать событие аудита в строку";

  /**
   * Найдено поле-коллекция.
   */
  public static final String FIELD_COLLECTION_WAS_FOUND = "Найдено поле-коллекция '{}', возвращаем всю коллекцию";

  /**
   * Найдено поле-объект.
   */
  public static final String FIELD_OBJECT_WAS_FOUND = "Найдено поле-объект '{}'";

  /**
   * Ошибка преобразования JSON-узла в строку.
   */
  public static final String COULD_NOT_CONVERT_JSON_NODE_TO_STRING = "Не удалось преобразовать JSON-узел в строку: {}";

  /**
   * Поле не найдено в JSON.
   */
  public static final String FIELD_NOT_FOUND_IN_JSON = "Поле '{}' не найдено в JSON";

  /**
   * Пропуск пустой коллекции.
   */
  public static final String SKIPPING_EMPTY_COLLECTION = "Пропускаем пустую коллекцию по пути: {}";

  /**
   * Переход внутрь коллекции с одним элементом.
   */
  public static final String GOING_INSIDE_COLLECTION_WITH_ONE_ELEMENT = "Заходим внутрь коллекции с одним элементом по пути: {}";

  /**
   * Пропуск коллекции с N элементами.
   */
  public static final String SKIPPING_COLLECTION = "Пропускаем коллекцию с {} элементами по пути: {}";

  /**
   * Имя утверждения в jwt-токене (sid).
   */
  public static final String SID_CLAIM_NAME = "sid";

  /** Subject JWT-токена (sub). */
  public static final String SUB = "sub";

  /** Идентификатор JWT-токена (jti). */
  public static final String JTI = "jti";
}
