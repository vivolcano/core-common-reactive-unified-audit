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
import ru.sbrf.sbererp.core.common.unified.audit.util.Constants;
import ru.sbrf.sbererp.core.common.unified.audit.util.LogMessage;
import ru.sbrf.sbererp.core.common.unified.audit.util.ObjectToFormatStringUtil;
import ru.sbrf.sbererp.core.common.unified.audit.util.enums.ApplicationGeneralError;

/**
 * Класс, отвечающий за регистрацию метамодели событий аудита в АС Единый Аудит.
 * <p>
 * После запуска приложения собирает метаданные событий с помощью {@link MetaModelConverter},
 * формирует объект {@link Metamodel} и отправляет его в АС Единый Аудит через {@link AuditService}.
 * Регистрация выполняется один раз при старте приложения на {@code boundedElastic}.
 */
@Slf4j
@Component
public class MetaModelSender {

  private final AuditService auditService;
  private final MetaModelConverter metaModelConverter;
  private final Scheduler auditScheduler;

  /**
   * Создаёт отправителя метамодели.
   *
   * @param auditService       блокирующий клиент аудита
   * @param metaModelConverter конвертер метамодели
   * @param auditScheduler     elastic-планировщик
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
  @Order(1)
  @EventListener(ApplicationReadyEvent.class)
  public void register() {
    Metamodel metamodel = metaModelConverter.create();
    log.debug(ObjectToFormatStringUtil.getFormatString(
        metamodel, Constants.ERROR_CONVERT_METAMODEL_TO_STRING));
    log.info(
        LogMessage.METAMODEL_REGISTERING_START,
        metamodel.getMetamodelVersion(),
        metamodel.getModule(),
        metamodel.getEventMetaInfos().stream()
            .map(EventMetaInfo::getName)
            .collect(Collectors.joining(Constants.COMMA))
    );
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
   * @param hash хеш зарегистрированной метамодели
   */
  private void logRegisterSuccess(String hash) {
    log.info(LogMessage.METAMODEL_REGISTERING_SUCCESS, hash);
  }

  /**
   * Логирует ошибку регистрации метамодели.
   *
   * @param throwable ошибка клиента
   */
  private void logRegisterError(Throwable throwable) {
    log.error(LogMessage.METAMODEL_REGISTERING_FAIL, throwable.getMessage(), throwable);
    log.error(ApplicationGeneralError.FAILED_SEND_AUDIT_META_MODEL.getMessage());
  }

  /**
   * Поглощает ошибку регистрации, чтобы старт приложения не падал из-за аудита.
   *
   * @param throwable ошибка регистрации
   * @return пустой сигнал
   */
  private Mono<String> swallowRegisterError(Throwable throwable) {
    return Mono.empty();
  }
}
