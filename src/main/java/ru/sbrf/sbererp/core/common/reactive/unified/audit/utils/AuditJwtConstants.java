package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * Константы JWT-claims, идентификаторов узла и значения-заглушки шапки события аудита.
 */
@UtilityClass
public final class AuditJwtConstants {

  /** Формат идентификатора сессии из claim {@code jti}. */
  public static final String JWT_CLAIM_JTI_PREFIX = "jwt_claim_jti:%s";

  /** Формат идентификатора узла пользователя из claim {@code sid}. */
  public static final String JWT_CLAIM_SID_PREFIX = "jwt_claim_sid:%s";

  /** Имя claim субъекта. */
  public static final String SUB = "sub";

  /** Имя claim идентификатора токена. */
  public static final String JTI = "jti";

  /** Имя claim идентификатора сессии узла. */
  public static final String SID_CLAIM_NAME = "sid";

  /** Имя claim имени пользователя. */
  public static final String FIRST_NAME = "given_name";

  /** Имя claim отчества. */
  public static final String MIDDLE_NAME = "patronymic";

  /** Имя claim фамилии. */
  public static final String LAST_NAME = "family_name";

  /** Переменная окружения имени пода. */
  public static final String HOSTNAME = "HOSTNAME";

  /** Переменная окружения namespace. */
  public static final String NAMESPACE = "NAMESPACE";

  /** Значение Node ID, если узел определить не удалось. */
  public static final String UNKNOWN_NODE_ID = "UNKNOWN-NODE-ID";

  /** Значение логина, если claim отсутствует. */
  public static final String NO_USER = "NO-USER";

  /** Значение ФИО, если claims отсутствуют. */
  public static final String NO_USER_NAME = "NO-USER-NAME";

  /** Значение сессии, если источник не найден. */
  public static final String NO_SESSION = "NO-SESSION";

  /** Значение request id, если заголовок отсутствует. */
  public static final String NO_REQUEST_ID = "NO_REQUEST_ID";

  /** Значение sid, если claim отсутствует. */
  public static final String NO_CLAIM_SID = "NO-CLAIM-SID";
}
