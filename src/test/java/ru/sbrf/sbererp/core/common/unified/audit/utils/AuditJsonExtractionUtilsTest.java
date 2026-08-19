package ru.sbrf.sbererp.core.common.unified.audit.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import ru.sbrf.sbererp.core.common.unified.audit.properties.holder.ParamHolder;

final class AuditJsonExtractionUtilsTest {

  @Test
  void extractFieldValueReturnsNestedJsonField() {
    ParamHolder holder = new ParamHolder("itemName", "name", "name", null);

    String value = AuditJsonExtractionUtils.extractFieldValue(
        "{\"item\":{\"name\":\"widget\"}}",
        holder
    );

    assertThat(value).isEqualTo("widget");
  }

  @Test
  void extractFieldValueMasksConfiguredFields() {
    ParamHolder holder = new ParamHolder("item", "payload", "item", List.of("secret"));

    String value = AuditJsonExtractionUtils.extractFieldValue(
        "{\"item\":{\"name\":\"widget\",\"secret\":\"hidden\"}}",
        holder
    );

    assertThat(value).contains("widget");
    assertThat(value).doesNotContain("hidden");
  }

  @Test
  void extractFieldValueReturnsNullWhenFieldMissing() {
    ParamHolder holder = new ParamHolder("missing", "missing", "missing", null);

    String value = AuditJsonExtractionUtils.extractFieldValue("{\"name\":\"widget\"}", holder);

    assertThat(value).isNull();
  }

  @Test
  void preparationStringMasksWholeBody() {
    ParamHolder holder = new ParamHolder("payload", "body", null, List.of("secret"));

    String value = AuditJsonExtractionUtils.preparationString(
        "{\"name\":\"widget\",\"secret\":\"hidden\"}",
        holder
    );

    assertThat(value).contains("widget");
    assertThat(value).doesNotContain("hidden");
  }

  @Test
  void getPageableParamsFormatsQuery() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api")
        .queryParam("page", "2")
        .queryParam("size", "20")
        .queryParam("sort", "name,asc")
        .build();

    assertThat(AuditJsonExtractionUtils.getPageableParams(request))
        .isEqualTo("{page=2, size=20, sort=[name,asc]}");
  }
}
