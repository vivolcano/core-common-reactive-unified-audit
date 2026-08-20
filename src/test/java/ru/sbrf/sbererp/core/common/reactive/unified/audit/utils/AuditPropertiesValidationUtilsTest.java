package ru.sbrf.sbererp.core.common.reactive.unified.audit.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.exception.UnifiedAuditException;
import ru.sbrf.sbererp.core.common.reactive.unified.audit.holder.ParamHolder;
import ru.sbrf.ufs.platform.audit.model.CriticalityEnum;

final class AuditPropertiesValidationUtilsTest {

  @Test
  void blankNameIsRejected() {
    assertThatThrownBy(() -> AuditPropertiesValidationUtils.validate(" ", "description"))
        .isInstanceOf(UnifiedAuditException.class);
  }

  @Test
  void emptyParamsMapIsRejectedWhenProvided() {
    assertThatThrownBy(() -> AuditPropertiesValidationUtils.validate(
        "event",
        "description",
        CriticalityEnum.UNCRITICAL,
        true,
        Map.of("request", List.<ParamHolder>of())
    )).isInstanceOf(UnifiedAuditException.class);
  }

  @Test
  void nullControllerClassIsRejected() {
    assertThatThrownBy(() -> AuditPropertiesValidationUtils.validate(
        null,
        Map.of("create", List.of("event"))
    )).isInstanceOf(UnifiedAuditException.class);
  }
}
