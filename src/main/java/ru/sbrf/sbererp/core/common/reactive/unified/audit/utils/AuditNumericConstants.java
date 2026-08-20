package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import lombok.experimental.UtilityClass;

/**
 * Числовые литералы модуля аудита: индексы, размеры, режимы сравнения, порядки Spring.
 */
@UtilityClass
public final class AuditNumericConstants {

  /** Ноль: пустая длина, начальный индекс, равенство. */
  public static final int ZERO = 0;

  /** Единица: один элемент, сдвиг на следующий индекс, размер одного значения. */
  public static final int ONE = 1;

  /** Два: режим «больше или равно» для размера коллекции. */
  public static final int TWO = 2;

  /** Минус один: несравнимо / невалидный размер / режим «меньше». */
  public static final int MINUS_ONE = -1;

  /** Минус два: режим «меньше или равно» для размера коллекции. */
  public static final int MINUS_TWO = -2;

  /**
   * Смещение {@link ru.sbrf.sbererp.core.common.reactive.unified.audit.filter.AuditWebFilter}
   * относительно {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE}.
   */
  public static final int FILTER_ORDER_OFFSET = 50;

  /** Лимит кэша тел аудита по умолчанию, в мегабайтах. */
  public static final int DEFAULT_MAX_BODY_SIZE_MEGABYTES = 1;

  /** Порядок регистрации метамодели на {@link org.springframework.boot.context.event.ApplicationReadyEvent}. */
  public static final int METAMODEL_REGISTER_ORDER = 1;
}
