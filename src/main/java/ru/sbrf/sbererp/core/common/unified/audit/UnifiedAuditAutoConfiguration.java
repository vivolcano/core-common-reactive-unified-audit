package ru.sbrf.sbererp.core.common.unified.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditClientProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditReactiveProperties;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditConfigurationFieldNames;

/**
 * Spring Boot auto-config модуля: только {@link ConditionalOnWebApplication.Type#REACTIVE}.
 * <p>
 * Регистрируется через {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 * Сканирует {@code ru.sbrf.sbererp.core.common.unified.audit} и биндит properties.
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
