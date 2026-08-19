package ru.sbrf.sbererp.audit.it.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Имитация actuator: путь должен попадать под {@code audit.reactive.exclude-path-patterns}.
 */
@RestController
public class AuditHealthController {

  /**
   * @return статус здоровья
   */
  @GetMapping("/actuator/health")
  public Mono<Map<String, String>> health() {
    return Mono.just(Map.of("status", "UP"));
  }
}
