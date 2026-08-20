package ru.sbrf.sbererp.core.common.reactive.unified.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.PathContainer;

final class AuditReactivePropertiesTest {

  @Test
  void nullExcludePatternsUseActuatorAndSwaggerDefaults() {
    AuditReactiveProperties properties = new AuditReactiveProperties(null, null);

    assertThat(properties.excludePathPatterns()).contains(
        "/actuator/**",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    );
    assertThat(AuditReactiveProperties.matchesAny(
        PathContainer.parsePath("/actuator/health"),
        properties.compiledExcludePathPatterns()
    )).isTrue();
    assertThat(AuditReactiveProperties.matchesAny(
        PathContainer.parsePath("/api/users"),
        properties.compiledExcludePathPatterns()
    )).isFalse();
  }

  @Test
  void maxBodyBytesUsesConfiguredDataSize() {
    AuditReactiveProperties properties = new AuditReactiveProperties(
        org.springframework.util.unit.DataSize.ofBytes(64),
        java.util.List.of()
    );

    assertThat(properties.maxBodyBytes()).isEqualTo(64);
  }

  @Test
  void emptyExcludePatternsDisableExclusions() {
    AuditReactiveProperties properties = new AuditReactiveProperties(null, java.util.List.of());

    assertThat(properties.excludePathPatterns()).isEmpty();
    assertThat(AuditReactiveProperties.matchesAny(
        PathContainer.parsePath("/actuator/health"),
        properties.compiledExcludePathPatterns()
    )).isFalse();
  }
}
