package ru.sbrf.sbererp.audit.it.controller;

import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Тестовый контроллер: успех, ошибка, GET без тела, непрочитанное тело, превышение лимита.
 */
@RestController
@RequestMapping("/api/items")
public class AuditItemsController {

  /**
   * Тело создания элемента.
   *
   * @param name   имя элемента
   * @param secret чувствительное поле для проверки маскирования
   */
  public record ItemRequest(String name, String secret) {
  }

  /**
   * Ответ с идентификатором элемента.
   *
   * @param id   идентификатор
   * @param name имя
   */
  public record ItemResponse(String id, String name) {
  }

  /**
   * Успешное создание: читает JSON, пишет заголовок ответа.
   *
   * @param request  тело запроса
   * @param exchange текущий обмен
   * @return созданный элемент
   */
  @PostMapping
  public Mono<ItemResponse> create(@RequestBody ItemRequest request, ServerWebExchange exchange) {
    exchange.getResponse().getHeaders().add("X-Item-Id", "42");
    return Mono.just(new ItemResponse("42", request.name()));
  }

  /**
   * HTTP 400 без исключения цепочки — проверяет YAML {@code success: false}.
   *
   * @param request тело запроса
   * @return ответ с кодом 400
   */
  @PostMapping("/fail")
  public Mono<ResponseEntity<ItemResponse>> fail(@RequestBody ItemRequest request) {
    return Mono.just(ResponseEntity.badRequest().body(new ItemResponse("0", request.name())));
  }

  /**
   * Создание с потенциально большим телом: HTTP не должен ломаться из-за лимита аудита.
   *
   * @param request тело запроса
   * @return созданный элемент
   */
  @PostMapping("/large")
  public Mono<ItemResponse> createLarge(@RequestBody ItemRequest request) {
    return Mono.just(new ItemResponse("1", request.name()));
  }

  /**
   * POST без {@code @RequestBody}: контроллер тело не читает, фильтр дочитывает его для аудита.
   *
   * @return статус
   */
  @PostMapping("/ping")
  public Mono<Map<String, String>> ping() {
    return Mono.just(Map.of("status", "ok"));
  }

  /**
   * GET по идентификатору: path-variable и условие {@code id=special}.
   *
   * @param id идентификатор из URI
   * @return элемент
   */
  @GetMapping("/{id}")
  public Mono<ItemResponse> getById(@PathVariable("id") String id) {
    return Mono.just(new ItemResponse(id, "item-" + id));
  }

  /**
   * GET-список без тела запроса, с опциональным query-параметром.
   *
   * @param query поисковая строка
   * @return список элементов
   */
  @GetMapping
  public Flux<ItemResponse> list(@RequestParam(name = "q", required = false) String query) {
    return Flux.just(new ItemResponse("1", Objects.requireNonNullElse(query, "all")));
  }

  /**
   * Метод без YAML-маппинга: событие аудита отправляться не должно.
   *
   * @return статус
   */
  @GetMapping("/meta/status")
  public Mono<Map<String, String>> status() {
    return Mono.just(Map.of("status", "ok"));
  }
}
