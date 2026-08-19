package ru.sbrf.sbererp.audit.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.sbt.audit.core.model.v2.event.Event;
import com.sbt.audit.core.model.v2.event.EventParam;
import com.sbt.audit.core.model.v2.metamodel.EventMetaInfo;
import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.sbrf.sbererp.audit.it.controller.AuditItemsController;
import ru.sbrf.sbererp.audit.it.support.RecordingAuditService;
import ru.sbrf.sbererp.core.common.unified.audit.properties.AuditEventsProperties;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.EventHolder;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditJwtConstants;

@SpringBootTest(classes = UnifiedAuditIntegrationApplication.class)
class UnifiedAuditIntegrationTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private RecordingAuditService recordingAuditService;

  @Autowired
  private AuditEventsProperties auditEventsProperties;

  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
        .configureClient()
        .responseTimeout(Duration.ofSeconds(10))
        .build();
    recordingAuditService.clearEvents();
  }

  @Test
  void yamlModelBindsToTestControllerAndRegistersMetamodel() {
    assertThat(auditEventsProperties.classEventsHolders())
        .singleElement()
        .extracting(holder -> holder.controllerClass())
        .isEqualTo(AuditItemsController.class);
    assertThat(auditEventsProperties.metamodelEvents())
        .extracting(EventHolder::name)
        .contains(
            "ItemCreated",
            "ItemCreatedVip",
            "ItemCreateFailed",
            "ItemCreatedLarge",
            "ItemPinged",
            "ItemFetched",
            "ItemFetchedSpecial",
            "ItemsListed"
        );
    assertThat(recordingAuditService.metamodels()).isNotEmpty();
    assertThat(recordingAuditService.metamodels().getFirst().getEventMetaInfos())
        .extracting(EventMetaInfo::getName)
        .contains("ItemCreated", "ItemFetchedSpecial", "ItemsListed");
  }

  @Test
  void createExtractsBodiesHeadersClaimsAndMasksSecret() {
    webTestClient.post()
        .uri("/api/items")
        .header("request-id", "req-1")
        .header("X-Client-App", "test-suite")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"name":"widget","secret":"hidden-value"}
            """)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().valueEquals("X-Item-Id", "42")
        .expectBody()
        .jsonPath("$.id").isEqualTo("42")
        .jsonPath("$.name").isEqualTo("widget");

    Event event = requireEvent("ItemCreated");
    assertThat(event.isSuccess()).isTrue();
    assertThat(event.getUserLogin()).isEqualTo("audit-user");
    assertThat(event.getUserName()).isEqualTo("Ivan Ivanovich Petrov");
    assertThat(event.getRequestId()).isEqualTo("req-1");
    assertThat(event.getSession()).isEqualTo("jwt_claim_jti:jwt-id");
    assertThat(event.getUserNode()).isEqualTo("jwt_claim_sid:node-sid");
    assertThat(param(event, "itemName")).isEqualTo("widget");
    assertThat(param(event, "itemId")).isEqualTo("42");
    assertThat(param(event, "httpStatus")).isEqualTo("200");
    assertThat(param(event, "clientApp")).isEqualTo("test-suite");
    assertThat(param(event, "itemResponseHeader")).isEqualTo("42");
    assertThat(param(event, "login")).isEqualTo("audit-user");
    assertThat(param(event, "payload")).contains("widget");
    assertThat(param(event, "payload")).doesNotContain("hidden-value");
  }

  @Test
  void createWithVipHeaderSelectsConditionEvent() {
    webTestClient.post()
        .uri("/api/items")
        .header("X-Client-App", "vip-suite")
        .header("X-Audit-Tag", "vip")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"name":"vip-item","secret":"top-secret"}
            """)
        .exchange()
        .expectStatus().isOk();

    Event event = requireEvent("ItemCreatedVip");
    assertThat(param(event, "auditTag")).isEqualTo("vip");
    assertThat(param(event, "itemName")).isEqualTo("vip-item");
    assertThat(recordingAuditService.findByName("ItemCreated")).isEmpty();
  }

  @Test
  void failSendsUnsuccessfulEventByHttpStatus() {
    webTestClient.post()
        .uri("/api/items/fail")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"name":"bad-item"}
            """)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.id").isEqualTo("0");

    Event event = requireEvent("ItemCreateFailed");
    assertThat(event.isSuccess()).isFalse();
    assertThat(param(event, "itemName")).isEqualTo("bad-item");
    assertThat(param(event, "httpStatus")).isEqualTo("400");
  }

  @Test
  void getByIdExtractsPathVariableWithoutRequestBody() {
    webTestClient.get()
        .uri("/api/items/7")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo("7");

    Event event = requireEvent("ItemFetched");
    assertThat(param(event, "itemId")).isEqualTo("7");
    assertThat(param(event, "itemName")).isEqualTo("item-7");
    assertThat(param(event, "httpStatus")).isEqualTo("200");
  }

  @Test
  void getByIdSpecialMatchesPathVariableCondition() {
    webTestClient.get()
        .uri("/api/items/special")
        .exchange()
        .expectStatus().isOk();

    Event event = requireEvent("ItemFetchedSpecial");
    assertThat(param(event, "itemId")).isEqualTo("special");
    assertThat(recordingAuditService.findByName("ItemFetched")).isEmpty();
  }

  @Test
  void listExtractsQueryParamOnGet() {
    webTestClient.get()
        .uri("/api/items?q=alpha")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[0].name").isEqualTo("alpha");

    Event event = requireEvent("ItemsListed");
    assertThat(param(event, "query")).isEqualTo("alpha");
    assertThat(param(event, "httpStatus")).isEqualTo("200");
  }

  @Test
  void excludedActuatorPathIsNotAudited() {
    webTestClient.get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("UP");

    assertThat(recordingAuditService.events()).isEmpty();
  }

  @Test
  void methodWithoutYamlMappingIsNotAudited() {
    webTestClient.get()
        .uri("/api/items/meta/status")
        .exchange()
        .expectStatus().isOk();

    assertThat(recordingAuditService.events()).isEmpty();
  }

  @Test
  void unknownPathIsNotAudited() {
    webTestClient.get()
        .uri("/no-such")
        .exchange()
        .expectStatus().isNotFound();

    assertThat(recordingAuditService.events()).isEmpty();
  }

  @Test
  void oversizedRequestBodyDoesNotBreakHttpAndOmitsBodyParams() {
    String oversizedName = "n".repeat(80);
    webTestClient.post()
        .uri("/api/items/large")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"name\":\"" + oversizedName + "\"}")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.name").isEqualTo(oversizedName);

    Event event = requireEvent("ItemCreatedLarge");
    assertThat(param(event, "httpStatus")).isEqualTo("200");
    assertThat(param(event, "itemName")).isNull();
  }

  @Test
  void unreadRequestBodyStillSendsPingEvent() {
    webTestClient.post()
        .uri("/api/items/ping")
        .header("X-Client-App", "ping-client")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {"ignored":true}
            """)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("ok");

    Event event = requireEvent("ItemPinged");
    assertThat(param(event, "clientApp")).isEqualTo("ping-client");
    assertThat(param(event, "pingStatus")).isEqualTo("ok");
    assertThat(param(event, "httpStatus")).isEqualTo("200");
  }

  @Test
  void anonymousHeadersUsePlaceholdersWhenClaimsFilterStillPresent() {
    webTestClient.get()
        .uri("/api/items/1")
        .exchange()
        .expectStatus().isOk();

    Event event = requireEvent("ItemFetched");
    assertThat(event.getUserLogin()).isEqualTo("audit-user");
    assertThat(event.getRequestId()).isEqualTo(AuditJwtConstants.NO_REQUEST_ID);
  }

  private Event requireEvent(String name) {
    return recordingAuditService.findByName(name)
        .orElseThrow(() -> new AssertionError("Expected audit event " + name
            + " but was " + recordingAuditService.events().stream().map(Event::getName).toList()));
  }

  private static String param(Event event, String name) {
    if (Objects.isNull(event.getParams())) {
      return null;
    }
    return event.getParams().stream()
        .filter(param -> Objects.equals(name, param.getName()))
        .map(EventParam::getValue)
        .findFirst()
        .orElse(null);
  }
}
