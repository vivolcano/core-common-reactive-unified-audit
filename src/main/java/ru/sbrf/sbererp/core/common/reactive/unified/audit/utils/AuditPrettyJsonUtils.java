package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import io.vavr.control.Try;
import lombok.experimental.UtilityClass;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pretty-print Jackson для DEBUG-логов метамодели и события.
 *
 * <p>Вызывать только из ветки {@code log.isDebugEnabled()}.
 */
@UtilityClass
public final class AuditPrettyJsonUtils {

  /** Jackson-маппер с indented output для DEBUG-логов. */
  private static final JsonMapper MAPPER = JsonMapper.builder()
      .configure(SerializationFeature.INDENT_OUTPUT, true)
      .build();

  /**
   * @param object      любой объект, сериализуемый Jackson
   * @param failMessage текст при {@link JacksonException}
   * @return indented JSON либо {@code failMessage}
   */
  public static String getFormatString(Object object, String failMessage) {
    return Try.of(() -> MAPPER.writeValueAsString(object)).getOrElse(failMessage);
  }
}
