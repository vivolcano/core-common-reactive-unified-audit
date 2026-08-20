package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import io.vavr.control.Option;
import io.vavr.control.Try;
import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Шапка события: nodeId, login/ФИО/sid из JWT-claims, session из jti/cookie/заголовков.
 */
@Slf4j
@UtilityClass
public final class AuditEventHeaderUtils {

  /** Идентификатор узла приложения. */
  private static final String NODE_ID = initializeNodeId();

  /**
   * Возвращает идентификатор узла приложения.
   *
   * @return Node ID приложения.
   */
  public static String getNodeId() {
    return NODE_ID;
  }

  /**
   * Извлекает логин пользователя из JWT-claim {@code sub}.
   *
   * @param tokenParamsMap JWT-claim → значение; логин берётся по ключу {@code sub}.
   * @return логин либо {@link AuditJwtConstants#NO_USER}.
   */
  public static String getUserLogin(Map<String, Object> tokenParamsMap) {
    return Option.of(getStringValue(tokenParamsMap, AuditJwtConstants.SUB))
        .filter(ObjectUtils::isNotEmpty)
        .getOrElse(() -> {
          log.debug(AuditLogMessages.NO_JWT_SUB_CLAIM);
          return AuditJwtConstants.NO_USER;
        });
  }

  /**
   * Извлекает полное имя пользователя из JWT-claims.
   *
   * @param tokenParamsMap JWT-claim → значение.
   * @return полное имя либо {@link AuditJwtConstants#NO_USER_NAME}.
   */
  public static String getUserName(Map<String, Object> tokenParamsMap) {
    if (Objects.isNull(tokenParamsMap)) {
      log.debug(AuditLogMessages.NO_JWT_NAME_CLAIMS);
      return AuditJwtConstants.NO_USER_NAME;
    }
    final String firstName = getStringValue(tokenParamsMap, AuditJwtConstants.FIRST_NAME);
    final String middleName = getStringValue(tokenParamsMap, AuditJwtConstants.MIDDLE_NAME);
    final String lastName = getStringValue(tokenParamsMap, AuditJwtConstants.LAST_NAME);
    if (Objects.isNull(firstName) && Objects.isNull(lastName)) {
      log.debug(AuditLogMessages.NO_JWT_NAME_CLAIMS);
      return AuditJwtConstants.NO_USER_NAME;
    }
    if (Objects.isNull(lastName)) {
      return firstName;
    }
    return Objects.isNull(middleName)
        ? firstName + AuditTextConstants.SPACE + lastName
        : firstName + AuditTextConstants.SPACE + middleName + AuditTextConstants.SPACE + lastName;
  }

  /**
   * Извлекает строковое значение claim и обрезает пробелы.
   *
   * @param tokenParamsMap JWT-claim → значение.
   * @param key            ключ claim.
   * @return строковое значение без пробелов по краям либо {@code null}.
   */
  public static String getStringValue(Map<String, Object> tokenParamsMap, String key) {
    return Option.of(tokenParamsMap)
        .flatMap(params -> Option.of(params.get(key)))
        .map(value -> value.toString().trim())
        .getOrNull();
  }

  /**
   * Извлекает ID запроса из HTTP-заголовка.
   *
   * @param request HTTP-запрос.
   * @return идентификатор запроса либо {@link AuditJwtConstants#NO_REQUEST_ID}.
   */
  public static String getRequestId(ServerHttpRequest request) {
    return Option.of(request.getHeaders().getFirst(AuditHttpConstants.REQUEST_ID))
        .getOrElse(AuditJwtConstants.NO_REQUEST_ID);
  }

  /**
   * Извлекает идентификатор узла пользователя (sid) из JWT-claims.
   *
   * @param tokenParamsMap JWT-claim → значение.
   * @return форматированная строка с sid либо {@link AuditJwtConstants#NO_CLAIM_SID}.
   */
  public static String getUserNode(Map<String, Object> tokenParamsMap) {
    return Option.of(getStringValue(tokenParamsMap, AuditJwtConstants.SID_CLAIM_NAME))
        .filter(ObjectUtils::isNotEmpty)
        .map(sid -> String.format(AuditJwtConstants.JWT_CLAIM_SID_PREFIX, sid))
        .getOrElse(() -> {
          log.debug(AuditLogMessages.NO_JWT_SID_CLAIM);
          return AuditJwtConstants.NO_CLAIM_SID;
        });
  }

  /**
   * Извлекает Session ID из JWT {@code jti}, cookie или заголовков.
   *
   * @param tokenParamsMap JWT-claim → значение.
   * @param request        HTTP-запрос.
   * @return Session ID либо {@link AuditJwtConstants#NO_SESSION}.
   */
  public static String getSession(Map<String, Object> tokenParamsMap, ServerHttpRequest request) {
    return Option.of(getStringValue(tokenParamsMap, AuditJwtConstants.JTI))
        .filter(ObjectUtils::isNotEmpty)
        .map(jti -> String.format(AuditJwtConstants.JWT_CLAIM_JTI_PREFIX, jti))
        .orElse(() -> Option.of(request.getHeaders().getFirst(AuditHttpConstants.X_CORRELATION_ID))
            .map(id -> AuditHttpConstants.CORRELATION_WITH_UNDERSCORE + id))
        .orElse(() -> Option.of(getCookieSessionId(request)))
        .getOrElse(() -> headerSessionOrDefault(request));
  }

  /**
   * Извлекает Session ID из cookie {@code JSESSIONID}.
   *
   * @param request HTTP-запрос.
   * @return значение cookie {@code JSESSIONID} либо {@code null}.
   */
  private static String getCookieSessionId(ServerHttpRequest request) {
    final HttpCookie cookie = request.getCookies().getFirst(AuditHttpConstants.JSESSIONID);
    return Objects.isNull(cookie)
        ? null
        : AuditHttpConstants.COOKIE_WITH_UNDERSCORE + cookie.getValue();
  }

  /**
   * Возвращает заголовок сессии или значение по умолчанию.
   *
   * @param request HTTP-запрос.
   * @return заголовок сессии либо значение по умолчанию.
   */
  private static String headerSessionOrDefault(ServerHttpRequest request) {
    return Objects.requireNonNullElse(
        request.getHeaders().getFirst(AuditHttpConstants.X_SESSION_ID),
        AuditJwtConstants.NO_SESSION
    );
  }

  /**
   * Инициализирует идентификатор узла из окружения или локальной машины.
   *
   * @return идентификатор узла из окружения либо локальной машины.
   */
  private static String initializeNodeId() {
    final String pod = System.getenv(AuditJwtConstants.HOSTNAME);
    final String namespace = System.getenv(AuditJwtConstants.NAMESPACE);
    return Objects.nonNull(pod) && Objects.nonNull(namespace)
        ? namespace + AuditTextConstants.UNDERSCORE + pod
        : Try.of(() -> {
          final InetAddress address = InetAddress.getLocalHost();
          final String ip = address.getHostAddress();
          final String fqdn = address.getCanonicalHostName();
          return ip + AuditTextConstants.UNDERSCORE
              + fqdn.replace(AuditTextConstants.CHAR_UNDERSCORE, AuditTextConstants.CHAR_DASH);
        })
        .onFailure(exception -> log.warn(AuditLogMessages.UNABLE_TO_RESOLVE_NODE_ID, exception))
        .getOrElse(AuditJwtConstants.UNKNOWN_NODE_ID);
  }
}
