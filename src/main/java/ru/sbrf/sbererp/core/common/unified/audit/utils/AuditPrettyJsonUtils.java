package ru.sbrf.sbererp.core.common.unified.audit.utils;

import lombok.experimental.UtilityClass;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pretty-print Jackson для DEBUG-логов метамодели и события.
 * <p>
 * Вызывать только из ветки {@code log.isDebugEnabled()}, чтобы payload не попал в INFO/WARN/ERROR.
 */
@UtilityClass
public final class AuditPrettyJsonUtils {

  private static final JsonMapper MAPPER = JsonMapper.builder()
      .configure(SerializationFeature.INDENT_OUTPUT, true)
      .build();

  /**
   * @param object      any Jackson-serializable object; {@code null} serializes as {@code null}.
   * @param failMessage text returned on {@link JacksonException}.
   * @return indented JSON, or {@code failMessage} if serialization fails
   */
  public static String getFormatString(Object object, String failMessage) {
    try {
      return MAPPER.writeValueAsString(object);
    } catch (JacksonException e) {
      return failMessage;
    }
  }
}
