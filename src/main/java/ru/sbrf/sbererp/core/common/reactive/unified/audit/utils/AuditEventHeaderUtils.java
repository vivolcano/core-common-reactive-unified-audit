package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Шапка события: nodeId, login/ФИО/sid из JWT-claims, session из jti/cookie/заголовков.
 */
@Slf4j
@UtilityClass
public final class AuditEventHeaderUtils {

  /**
   * Идентификатор узла приложения, полученный из окружения или локальных настроек.
   */
  private static final String NODE_ID = initializeNodeId();

  /**
   * Возвращает идентификатор узла приложения.
   *
   * @return Node ID.
   */
  public static String getNodeId() {
    return NODE_ID;
  }

  /**
   * Извлекает логин пользователя из мапы JWT-claim → значение.
   *
   * @param tokenParamsMap мапа JWT-claim → значение, логин берётся по ключу {@code sub}.
   * @return логин пользователя или константу {@link AuditJwtConstants#NO_USER}.
   */
  public static String getUserLogin(Map<String, Object> tokenParamsMap) {
    String sub = getStringValue(tokenParamsMap, AuditJwtConstants.SUB);
    if (Objects.isNull(sub) || sub.isEmpty()) {
      log.debug(AuditLogMessages.NO_JWT_SUB_CLAIM);
      return AuditJwtConstants.NO_USER;
    }
    return sub;
  }

  /**
   * Извлекает полное имя пользователя из мапы JWT-claim → значение.
   *
   * @param tokenParamsMap мапа JWT-claim → значение.
   * @return полное имя пользователя или {@link AuditJwtConstants#NO_USER_NAME}.
   */
  public static String getUserName(Map<String, Object> tokenParamsMap) {
    if (Objects.isNull(tokenParamsMap)) {
      log.debug(AuditLogMessages.NO_JWT_NAME_CLAIMS);
      return AuditJwtConstants.NO_USER_NAME;
    }
    String firstName = getStringValue(tokenParamsMap, AuditJwtConstants.FIRST_NAME);
    String middleName = getStringValue(tokenParamsMap, AuditJwtConstants.MIDDLE_NAME);
    String lastName = getStringValue(tokenParamsMap, AuditJwtConstants.LAST_NAME);
    if (Objects.isNull(firstName) && Objects.isNull(lastName)) {
      log.debug(AuditLogMessages.NO_JWT_NAME_CLAIMS);
      return AuditJwtConstants.NO_USER_NAME;
    }
    if (Objects.isNull(lastName)) {
      return firstName;
    }
    if (Objects.isNull(middleName)) {
      return firstName + AuditTextConstants.SPACE + lastName;
    }
    return firstName + AuditTextConstants.SPACE + middleName + AuditTextConstants.SPACE + lastName;
  }

  /**
   * Извлекает строковое значение из мапы JWT-claim → значение по ключу, обрезая пробелы.
   *
   * @param tokenParamsMap мапа JWT-claim → значение.
   * @param key            ключ claim.
   * @return строковое значение без пробелов по краям или {@code null}.
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
   * @param request HTTP-запрос {@link ServerHttpRequest}.
   * @return идентификатор запроса или {@link AuditJwtConstants#NO_REQUEST_ID}.
   */
  public static String getRequestId(ServerHttpRequest request) {
    return Optional.ofNullable(request.getHeaders().getFirst(AuditHttpConstants.REQUEST_ID))
        .orElse(AuditJwtConstants.NO_REQUEST_ID);
  }

  /**
   * Извлекает идентификатор узла пользователя (sid) из мапы JWT-claim → значение.
   *
   * @param tokenParamsMap мапа JWT-claim → значение.
   * @return форматированная строка с sid или {@link AuditJwtConstants#NO_CLAIM_SID}.
   */
  public static String getUserNode(Map<String, Object> tokenParamsMap) {
    String sid = getStringValue(tokenParamsMap, AuditJwtConstants.SID_CLAIM_NAME);
    if (Objects.isNull(sid) || sid.isEmpty()) {
      log.debug(AuditLogMessages.NO_JWT_SID_CLAIM);
      return AuditJwtConstants.NO_CLAIM_SID;
    }
    return String.format(AuditJwtConstants.JWT_CLAIM_SID_PREFIX, sid);
  }

  /**
   * Извлекает Session ID из мапы JWT-claim → значение или HTTP-запроса.
   *
   * @param tokenParamsMap мапа JWT-claim → значение.
   * @param request        HTTP-запрос {@link ServerHttpRequest}.
   * @return Session ID или {@link AuditJwtConstants#NO_SESSION}.
   */
  public static String getSession(Map<String, Object> tokenParamsMap, ServerHttpRequest request) {
    String jti = getStringValue(tokenParamsMap, AuditJwtConstants.JTI);
    if (Objects.nonNull(jti) && !jti.isEmpty()) {
      return String.format(AuditJwtConstants.JWT_CLAIM_JTI_PREFIX, jti);
    }
    String correlationId = request.getHeaders().getFirst(AuditHttpConstants.X_CORRELATION_ID);
    if (Objects.nonNull(correlationId)) {
      return AuditHttpConstants.CORRELATION_WITH_UNDERSCORE + correlationId;
    }
    return Optional.ofNullable(getCookieSessionId(request))
        .orElseGet(() -> headerSessionOrDefault(request));
  }

  /**
   * Извлекает Session ID из cookie {@code JSESSIONID}.
   *
   * @param request HTTP-запрос {@link ServerHttpRequest}.
   * @return значение cookie или {@code null}.
   */
  private static String getCookieSessionId(ServerHttpRequest request) {
    HttpCookie cookie = request.getCookies().getFirst(AuditHttpConstants.JSESSIONID);
    return Objects.isNull(cookie)
        ? null
        : AuditHttpConstants.COOKIE_WITH_UNDERSCORE + cookie.getValue();
  }

  /**
   * Возвращает заголовок сессии или значение по умолчанию.
   *
   * @param request HTTP-запрос {@link ServerHttpRequest}.
   * @return идентификатор сессии.
   */
  private static String headerSessionOrDefault(ServerHttpRequest request) {
    String headerSessionId = request.getHeaders().getFirst(AuditHttpConstants.X_SESSION_ID);
    return Objects.requireNonNullElse(headerSessionId, AuditJwtConstants.NO_SESSION);
  }

  /**
   * Инициализирует идентификатор узла приложения на основе окружения или локальной машины.
   *
   * @return идентификатор узла.
   */
  private static String initializeNodeId() {
    String pod = System.getenv(AuditJwtConstants.HOSTNAME);
    String namespace = System.getenv(AuditJwtConstants.NAMESPACE);
    if (Objects.nonNull(pod) && Objects.nonNull(namespace)) {
      return namespace + AuditTextConstants.UNDERSCORE + pod;
    }
    try {
      InetAddress address = InetAddress.getLocalHost();
      String ip = address.getHostAddress();
      String fqdn = address.getCanonicalHostName();
      return ip + AuditTextConstants.UNDERSCORE + fqdn.replace(AuditTextConstants.CHAR_UNDERSCORE, AuditTextConstants.CHAR_DASH);
    } catch (Exception exception) {
      log.warn(AuditLogMessages.UNABLE_TO_RESOLVE_NODE_ID, exception);
      return AuditJwtConstants.UNKNOWN_NODE_ID;
    }
  }
}
