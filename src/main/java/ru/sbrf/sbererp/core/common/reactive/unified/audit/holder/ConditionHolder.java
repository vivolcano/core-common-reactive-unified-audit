package ru.sbrf.sbererp.core.common.reactive.unified.audit.holder;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.Name;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.Extractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.enums.ConditionOperator;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditConfigurationFieldNames;

/**
 * YAML-условие события. {@code field} биндится в {@code name}; экстрактор ставит биндер.
 *
 * @param name      имя поля из YAML {@code field}
 * @param operator  оператор сравнения {@link ConditionOperator}
 * @param values    ожидаемые значения; {@code null} → пустой список
 * @param extractor стратегия чтения обмена
 * @param key       ключ извлечения; по умолчанию {@code name}
 */
public record ConditionHolder(
    String name,
    ConditionOperator operator,
    List<String> values,
    AtomicReference<Extractor> extractor,
    AtomicReference<String> key
) implements Holder {

  /**
   * @param name     имя поля условия
   * @param operator оператор сравнения
   * @param values   ожидаемые значения; {@code null} заменяется пустым списком
   */
  @ConstructorBinding
  public ConditionHolder(
      @Name(AuditConfigurationFieldNames.FIELD) String name,
      ConditionOperator operator,
      List<String> values) {
    this(
        name,
        operator,
        ObjectUtils.isEmpty(values) ? List.of() : List.copyOf(values),
        new AtomicReference<>(),
        new AtomicReference<>()
    );
  }

  /**
   * Нормализует ссылки биндера, если канонический конструктор вызван напрямую.
   */
  public ConditionHolder {
    extractor = Objects.requireNonNullElseGet(extractor, AtomicReference::new);
    key = Objects.requireNonNullElseGet(key, AtomicReference::new);
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
}
