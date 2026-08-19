package ru.sbrf.sbererp.core.common.unified.audit.resolver.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import ru.sbrf.sbererp.core.common.unified.audit.util.Constants;
import ru.sbrf.sbererp.core.common.unified.audit.util.LogMessage;

/**
 * Утилиты заполнения шапки события аудита из claims и HTTP-запроса WebFlux.
 */
@Slf4j
@UtilityClass
public class EventHeaderUtil {

  /**
   * Идентификатор узла приложения, полученный из окружения или локальных настроек.
   */
  private static final String NODE_ID = initializeNodeId();

  /**
   * Возвращает идентификатор узла приложения.
   *
   * @return Node ID
   */
  public static String getNodeId() {
    return NODE_ID;
  }

  /**
   * Извлекает логин пользователя из tokenParamsMap.
   *
   * @param tokenParamsMap карта параметров, из которой извлекается логин по ключу {@code sub}
   * @return логин пользователя или константу {@code NO_USER}
   */
  public static String getUserLogin(Map<String, Object> tokenParamsMap) {
    String sub = getStringValue(tokenParamsMap, Constants.SUB);
    if (Objects.isNull(sub) || sub.isEmpty()) {
      log.error(LogMessage.USER_LOGIN_NOT_EXTRACTED);
      return Constants.NO_USER;
    }
    return sub;
  }

  /**
   * Извлекает полное имя пользователя из tokenParamsMap.
   *
   * @param tokenParamsMap карта параметров
   * @return полное имя пользователя или {@code NO-USER-NAME}
   */
  public static String getUserName(Map<String, Object> tokenParamsMap) {
    if (Objects.isNull(tokenParamsMap)) {
      log.error(LogMessage.USER_NAME_NOT_EXTRACTED_FROM_JWT_TOKEN);
      return Constants.NO_USER_NAME;
    }
    String firstName = getStringValue(tokenParamsMap, Constants.FIRST_NAME);
    String middleName = getStringValue(tokenParamsMap, Constants.MIDDLE_NAME);
    String lastName = getStringValue(tokenParamsMap, Constants.LAST_NAME);
    if (Objects.isNull(firstName) && Objects.isNull(lastName)) {
      log.error(LogMessage.USER_NAME_NOT_EXTRACTED_FROM_JWT_TOKEN);
      return Constants.NO_USER_NAME;
    }
    if (Objects.isNull(lastName)) {
      return firstName;
    }
    if (Objects.isNull(middleName)) {
      return firstName + Constants.SPACE + lastName;
    }
    return firstName + Constants.SPACE + middleName + Constants.SPACE + lastName;
  }

  /**
   * Извлекает строковое значение из карты по ключу, обрезая пробелы.
   *
   * @param tokenParamsMap карта параметров
   * @param key            ключ
   * @return строковое значение без пробелов по краям или {@code null}
   */
  public static String getStringValue(Map<String, Object> tokenParamsMap, String key) {
    if (Objects.isNull(tokenParamsMap)) {
      return null;
    }
    Object val = tokenParamsMap.get(key);
    return Objects.isNull(val) ? null : val.toString().trim();
  }

  /**
   * Извлекает ID запроса из HTTP-запроса.
   *
   * @param request HTTP-запрос
   * @return Request ID или {@code NO_REQUEST_ID}
   */
  public static String getRequestId(ServerHttpRequest request) {
    return Optional.ofNullable(request.getHeaders().getFirst(Constants.REQUEST_ID))
        .orElse(Constants.NO_REQUEST_ID);
  }

  /**
   * Извлекает идентификатор узла пользователя (sid) из tokenParamsMap.
   *
   * @param tokenParamsMap карта параметров
   * @return форматированная строка с sid или {@code NO_CLAIM_SID}
   */
  public static String getUserNode(Map<String, Object> tokenParamsMap) {
    String sid = getStringValue(tokenParamsMap, Constants.SID_CLAIM_NAME);
    if (Objects.isNull(sid) || sid.isEmpty()) {
      log.error(LogMessage.USER_LOGIN_NOT_EXTRACTED);
      return Constants.NO_CLAIM_SID;
    }
    return String.format(Constants.JWT_CLAIM_SID_PREFIX, sid);
  }

  /**
   * Извлекает Session ID из tokenParamsMap или HTTP-запроса.
   *
   * @param tokenParamsMap карта параметров
   * @param request        HTTP-запрос
   * @return Session ID или {@code NO_SESSION}
   */
  public static String getSession(Map<String, Object> tokenParamsMap, ServerHttpRequest request) {
    String jti = getStringValue(tokenParamsMap, Constants.JTI);
    if (Objects.nonNull(jti) && !jti.isEmpty()) {
      return String.format(Constants.JWT_CLAIM_JTI_PREFIX, jti);
    }
    String correlationId = request.getHeaders().getFirst(Constants.X_CORRELATION_ID);
    if (Objects.nonNull(correlationId)) {
      return Constants.CORRELATION_WITH_UNDERSCORE + correlationId;
    }
    return Optional.ofNullable(getCookieSessionId(request))
        .orElseGet(() -> headerSessionOrDefault(request));
  }

  /**
   * Извлекает Session ID из cookie {@code JSESSIONID}.
   *
   * @param request HTTP-запрос
   * @return значение cookie или {@code null}
   */
  private static String getCookieSessionId(ServerHttpRequest request) {
    HttpCookie cookie = request.getCookies().getFirst(Constants.JSESSIONID);
    return Objects.isNull(cookie)
        ? null
        : Constants.COOKIE_WITH_UNDERSCORE + cookie.getValue();
  }

  /**
   * Возвращает заголовок сессии или значение по умолчанию.
   *
   * @param request HTTP-запрос
   * @return идентификатор сессии
   */
  private static String headerSessionOrDefault(ServerHttpRequest request) {
    String headerSessionId = request.getHeaders().getFirst(Constants.X_SESSION_ID);
    return Objects.requireNonNullElse(headerSessionId, Constants.NO_SESSION);
  }

  /**
   * Инициализирует идентификатор узла приложения на основе окружения или локальной машины.
   *
   * @return идентификатор узла
   */
  private static String initializeNodeId() {
    String pod = System.getenv(Constants.HOSTNAME);
    String namespace = System.getenv(Constants.NAMESPACE);
    if (Objects.nonNull(pod) && Objects.nonNull(namespace)) {
      return namespace + Constants.UNDERSCORE + pod;
    }
    try {
      InetAddress address = InetAddress.getLocalHost();
      String ip = address.getHostAddress();
      String fqdn = address.getCanonicalHostName();
      return ip + Constants.UNDERSCORE + fqdn.replace(Constants.CHAR_UNDERSCORE, Constants.CHAR_DASH);
    } catch (UnknownHostException exception) {
      log.warn(LogMessage.UNABLE_TO_GET_NODE_ID);
      return Constants.UNKNOWN_NODE_ID;
    }
  }
}
