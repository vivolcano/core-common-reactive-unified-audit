package ru.sbrf.sbererp.core.common.reactive.unified.audit.postprocessor;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.binder.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.extractor.Extractor;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ClassEventsHolder;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditLogMessages;

/**
 * {@link BeanPostProcessor}, который на старте привязывает {@link Extractor} к YAML-событиям контроллера.
 *
 * @see AuditParameterBinder#configureAuditParameters(ClassEventsHolder, Class)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class AuditEventBinderPostProcessor implements BeanPostProcessor {

  private final AuditEventsProperties properties;

  /**
   * Если класс бина есть в {@code audit.model.class-events-holders}, привязывает экстракторы.
   *
   * @param bean     инициализируемый бин
   * @param beanName имя бина
   * @return тот же бин
   */
  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    final Class<?> clazz = bean.getClass();
    Option.ofOptional(
            properties.classEventsHolders().stream()
                .filter(classEventsHolder -> clazz.isAssignableFrom(classEventsHolder.controllerClass()))
                .findFirst()
        )
        .forEach(classEventsHolder -> {
          AuditParameterBinder.configureAuditParameters(classEventsHolder, clazz);
          log.info(AuditLogMessages.BINDING_EXTRACTORS, beanName, clazz.getName());
        });
    return bean;
  }
}
