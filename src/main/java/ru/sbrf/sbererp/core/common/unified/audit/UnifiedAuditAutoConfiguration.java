package ru.sbrf.sbererp.core.common.unified.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditClientProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditReactiveProperties;

/**
 * Класс авто-конфигурации для реактивного модуля аудита.
 * <p>
 * Авто-конфигурация осуществляется при помощи файла
 * {@code org.springframework.boot.autoconfigure.AutoConfiguration.imports} в ресурсах и включается
 * только для WebFlux-приложения.
 */
@AutoConfiguration(afterName = "org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ComponentScan("ru.sbrf.sbererp.core.common.unified.audit")
@EnableConfigurationProperties({
    AuditClientProperties.class,
    AuditEventsProperties.class,
    AuditReactiveProperties.class
})
public class UnifiedAuditAutoConfiguration {
}
