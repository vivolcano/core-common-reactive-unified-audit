package ru.sbrf.sbererp.core.common.reactive.unified.audit.postprocessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.binder.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.Extractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ClassEventsHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;

/**
 * На старте привязывает {@link Extractor} к YAML-событиям контроллера.
 * <p>
 * Для бина, чей класс есть в {@code audit.model.class-events-holders}, вызывает
 * {@link AuditParameterBinder#configureAuditParameters(ClassEventsHolder, Class)}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class AuditEventBinderPostProcessor implements BeanPostProcessor {

  /**
   * Экземпляр конфигурационного класса, содержащий модель событий аудита, загружаемые из yaml-файла
   * по префиксу "audit.model".
   */
  private final AuditEventsProperties properties;

  /**
   * Метод, вызываемый Spring перед инициализацией бина.
   * <p>
   * Проверяет, относится ли класс текущего бина к одному из контроллеров, описанных в конфигурации.
   * Если да — вызывает метод сбора источников событий для этого класса.
   *
   * @param bean     бин Spring, который находится в процессе инициализации.
   * @param beanName имя бина.
   * @return возвращается тот же бин после обработки.
   * @throws BeansException при возникновении ошибки обработки бина.
   */
  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    Class<?> clazz = bean.getClass();
    for (ClassEventsHolder classEventsHolder : properties.classEventsHolders()) {
      if (clazz.isAssignableFrom(classEventsHolder.controllerClass())) {
        AuditParameterBinder.configureAuditParameters(classEventsHolder, clazz);
        log.info(AuditLogMessages.BINDING_EXTRACTORS, beanName, clazz.getName());
        return bean;
      }
    }
    return bean;
  }
}
