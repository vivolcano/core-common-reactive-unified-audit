package ru.sbrf.sbererp.core.common.unified.audit.properties.holder;

import static ru.sbrf.sbererp.core.common.unified.audit.utils.AuditPropertiesValidationUtils.validate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.web.server.ServerWebExchange;
import ru.sbrf.sbererp.core.common.unified.audit.extractor.Extractor;

/**
 * YAML-параметр события. {@link #extractor} и {@link #key} мутируются биндером.
 *
 * @param name        имя в метамодели.
 * @param description описание в метамодели.
 * @param masks       JSON-пути для вырезания полей; может быть {@code null}.
 * @param key         ключ извлечения (заголовок/поле); задаётся YAML или биндером.
 * @param extractor   стратегия чтения {@link ServerWebExchange}.
 */
public record ParamHolder(
    String name,
    String description,
    List<String> masks,
    AtomicReference<String> key,
    AtomicReference<Extractor> extractor
) implements Holder {

  /**
   * Конструктор привязки YAML.
   *
   * @param name        имя параметра.
   * @param description описание параметра.
   * @param key         ключ извлечения, если отличается от имени.
   * @param masks       список масок.
   */
  @ConstructorBinding
  public ParamHolder(String name, String description, String key, List<String> masks) {
    this(name, description, masks, new AtomicReference<>(key), new AtomicReference<>());
  }

  /**
   * Проверяет обязательные поля YAML.
   */
  public ParamHolder {
    validate(name, description);
    key = Objects.requireNonNullElseGet(key, AtomicReference::new);
    extractor = Objects.requireNonNullElseGet(extractor, AtomicReference::new);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Extractor getExtractor() {
    return extractor.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExtractor(Extractor extractor) {
    this.extractor.set(extractor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getKey() {
    return key.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setKey(String key) {
    this.key.set(key);
  }

  /**
   * Сравнивает параметры по YAML-полям, без рантайм-привязки экстрактора.
   *
   * @param object другой объект.
   * @return {@code true}, если имя, описание и маски совпадают
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof ParamHolder that)) {
      return false;
    }
    return Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(masks, that.masks);
  }

  /**
   * Хеш по YAML-полям параметра.
   *
   * @return хеш имени, описания и масок
   */
  @Override
  public int hashCode() {
    return Objects.hash(name, description, masks);
  }
}
