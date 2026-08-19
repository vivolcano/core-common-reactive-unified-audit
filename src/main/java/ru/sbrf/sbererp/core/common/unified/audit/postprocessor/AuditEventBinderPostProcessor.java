package ru.sbrf.sbererp.core.common.unified.audit.postprocessor;

import static ru.sbrf.sbererp.core.common.unified.audit.util.LogMessage.POSTPROCESSOR_MESSAGE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import ru.sbrf.sbererp.core.common.unified.audit.binder.AuditParameterBinder;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ClassEventsHolder;

/**
 * Класс-постпроцессор бинов Spring, предназначенный для поиска и обработки классов-контроллеров,
 * которые должны участвовать в логировании событий аудита.
 * <p>
 * Перед инициализацией бина проверяет, является ли класс контроллером с настроенными событиями
 * аудита. В случае совпадения вызывает метод
 * {@link AuditParameterBinder#configureAuditParameters(ClassEventsHolder, Class)} d}, чтобы связать
 * параметры событий с соответствующими методами контроллера.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventBinderPostProcessor implements BeanPostProcessor {

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
   * @param bean     бин Spring, который находится в процессе инициализации
   * @param beanName имя бина
   * @return возвращается тот же бин после обработки
   * @throws BeansException при возникновении ошибки обработки бина
   */
  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    Class<?> clazz = bean.getClass();
    for (ClassEventsHolder classEventsHolder : properties.getClassEventsHolders()) {
      if (clazz.isAssignableFrom(classEventsHolder.getControllerClass())) {
        AuditParameterBinder.configureAuditParameters(classEventsHolder, clazz);
        log.info(POSTPROCESSOR_MESSAGE, beanName);
        return bean;
      }
    }
    return bean;
  }
}
