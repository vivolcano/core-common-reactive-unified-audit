package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.adapter.EventAdapter;

final class AuditPrettyJsonUtilsTest {

  @Test
  void getFormatStringPrettyPrintsObject() {
    EventAdapter event = EventAdapter.builder()
        .eventName("ItemCreated")
        .isSuccess(true)
        .build();

    String json = AuditPrettyJsonUtils.getFormatString(event, "failed");

    assertThat(json).contains("ItemCreated");
    assertThat(json).contains("\n");
  }
}
