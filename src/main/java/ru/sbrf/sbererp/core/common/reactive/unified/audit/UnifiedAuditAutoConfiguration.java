package ru.sbrf.sbererp.core.common.reactive.unified.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditClientProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.properties.AuditReactiveProperties;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.utils.AuditConfigurationFieldNames;

/**
 * Spring Boot auto-config модуля для {@link ConditionalOnWebApplication.Type#REACTIVE}.
 *
 * <p>Сканирует {@code ru.sbrf.sbererp.core.common.reactive.unified.audit} и биндит
 * {@link AuditClientProperties}, {@link AuditEventsProperties}, {@link AuditReactiveProperties}.
 */
@AutoConfiguration(afterName = AuditConfigurationFieldNames.WEBFLUX_AUTO_CONFIGURATION)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ComponentScan(AuditConfigurationFieldNames.BASE_PACKAGE)
@EnableConfigurationProperties({
    AuditClientProperties.class,
    AuditEventsProperties.class,
    AuditReactiveProperties.class
})
public final class UnifiedAuditAutoConfiguration {
}
