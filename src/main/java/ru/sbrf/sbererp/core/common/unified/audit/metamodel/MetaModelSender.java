package ru.sbrf.sbererp.core.common.unified.audit.metamodel;

import com.sbt.audit.core.model.v2.metamodel.EventMetaInfo;
import com.sbt.audit.core.model.v2.metamodel.Metamodel;
import com.sbt.audit.core.service.AuditService;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import ru.sbrf.sbererp.core.common.unified.audit.config.AuditSchedulerConfig;
import ru.sbrf.sbererp.core.common.unified.audit.converter.MetaModelConverter;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditTextConstants;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditLogMessages;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditNumericConstants;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditPrettyJsonUtils;

/**
 * Регистрирует метамодель в АС Единый Аудит на {@link ApplicationReadyEvent}.
 * <p>
 * {@link AuditService#register} вызывается на {@code unifiedAuditElasticScheduler}.
 * Ошибка регистрации логируется и не валит старт приложения.
 */
@Slf4j
@Component
public final class MetaModelSender {

  private final AuditService auditService;
  private final MetaModelConverter metaModelConverter;
  private final Scheduler auditScheduler;

  /**
   * Создаёт отправителя метамодели.
   *
   * @param auditService       блокирующий клиент аудита.
   * @param metaModelConverter конвертер метамодели.
   * @param auditScheduler     elastic-планировщик.
   */
  public MetaModelSender(
      AuditService auditService,
      MetaModelConverter metaModelConverter,
      @Qualifier(AuditSchedulerConfig.ELASTIC_SCHEDULER) Scheduler auditScheduler) {
    this.auditService = auditService;
    this.metaModelConverter = metaModelConverter;
    this.auditScheduler = auditScheduler;
  }

  /**
   * Метод, вызываемый при старте приложения.
   * <p>
   * Создает метамодель, регистрирует её в системе аудита и логирует хэш метамодели. Если
   * регистрация не удалась — логирует ошибку.
   */
  @Order(AuditNumericConstants.METAMODEL_REGISTER_ORDER)
  @EventListener(ApplicationReadyEvent.class)
  public void register() {
    Metamodel metamodel = metaModelConverter.create();
    int eventCount = metamodel.getEventMetaInfos() == null
        ? AuditNumericConstants.ZERO
        : metamodel.getEventMetaInfos().size();
    log.info(
        AuditLogMessages.METAMODEL_REGISTERING,
        metamodel.getMetamodelVersion(),
        metamodel.getModule(),
        eventCount
    );
    if (log.isDebugEnabled()) {
      String eventNames = metamodel.getEventMetaInfos() == null
          ? AuditTextConstants.EMPTY_STRING
          : metamodel.getEventMetaInfos().stream()
              .map(EventMetaInfo::getName)
              .collect(Collectors.joining(AuditTextConstants.COMMA));
      log.debug(AuditLogMessages.METAMODEL_EVENT_NAMES, eventNames);
      log.debug(
          AuditLogMessages.AUDIT_METAMODEL_PAYLOAD,
          AuditPrettyJsonUtils.getFormatString(
              metamodel, AuditLogMessages.FAILED_TO_SERIALIZE_METAMODEL)
      );
    }
    Mono.fromCallable(() -> auditService.register(metamodel))
        .subscribeOn(auditScheduler)
        .doOnNext(this::logRegisterSuccess)
        .doOnError(this::logRegisterError)
        .onErrorResume(this::swallowRegisterError)
        .block();
  }

  /**
   * Логирует успешную регистрацию метамодели.
   *
   * @param hash хеш зарегистрированной метамодели.
   */
  private void logRegisterSuccess(String hash) {
    log.info(AuditLogMessages.METAMODEL_REGISTERED, hash);
  }

  /**
   * Логирует ошибку регистрации метамодели.
   *
   * @param throwable ошибка клиента.
   */
  private void logRegisterError(Throwable throwable) {
    log.error(AuditLogMessages.METAMODEL_REGISTER_FAILED, throwable);
  }

  /**
   * Поглощает ошибку регистрации, чтобы старт приложения не падал из-за аудита.
   *
   * @param throwable ошибка регистрации.
   * @return пустой сигнал
   */
  private Mono<String> swallowRegisterError(Throwable throwable) {
    return Mono.empty();
  }
}
