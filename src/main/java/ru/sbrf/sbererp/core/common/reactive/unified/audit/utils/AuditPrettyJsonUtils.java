package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

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

  /** Jackson-маппер с indented output для DEBUG-логов. */
  private static final JsonMapper MAPPER = JsonMapper.builder()
      .configure(SerializationFeature.INDENT_OUTPUT, true)
      .build();

  /**
   * Сериализует объект в indented JSON для DEBUG-лога.
   *
   * @param object      любой объект, сериализуемый Jackson; {@code null} сериализуется как {@code null}.
   * @param failMessage текст, который возвращается при {@link JacksonException}.
   * @return indented JSON или {@code failMessage}, если сериализация не удалась.
   */
  public static String getFormatString(Object object, String failMessage) {
    try {
      return MAPPER.writeValueAsString(object);
    } catch (JacksonException e) {
      return failMessage;
    }
  }
}
