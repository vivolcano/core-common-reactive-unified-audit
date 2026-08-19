package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import static ru.sbrf.sbererp.core.common.unified.audit.properties.util.ValidateUtil.validate;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import ru.sbrf.sbererp.core.common.unified.audit.extractor.Extractor;

/**
 * Класс, представляющий единичный параметр в событии аудита.
 */
@Getter
@EqualsAndHashCode(exclude = {"extractor", "key"})
public class ParamHolder implements Holder {

  /**
   * Имя параметра.
   */
  private final String name;

  /**
   * Описание параметра.
   */
  private final String description;

  /**
   * Список масок для маскирования полей параметра-объекта.
   */
  private final List<String> masks;

  /**
   * Экстрактор, устанавливается при определении способа извлечения значения параметра.
   */
  @Setter
  private Extractor extractor;

  /**
   * Ключ, необязательный параметр. Устанавливается при несоответствии имени параметра и имени поля для извлечения значения
   * параметра.
   */
  @Setter
  private String key;

  /**
   * Конструктор класса.
   *
   * @param name        имя параметра
   * @param description описание параметра
   * @param masks       список масок
   */
  public ParamHolder(String name, String description, String key, List<String> masks) {
    validate(name, description);
    this.name = name;
    this.description = description;
    this.masks = masks;
    this.key = key;
  }
}
