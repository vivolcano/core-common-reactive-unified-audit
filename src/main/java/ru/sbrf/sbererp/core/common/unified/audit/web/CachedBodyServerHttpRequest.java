package ru.sbrf.sbererp.core.common.unified.audit.web;

import java.util.Objects;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import ru.sbrf.sbererp.core.common.unified.audit.utils.AuditExchangeAttributeNames;

/**
 * {@link ServerHttpRequest} с заранее прочитанным телом, доступным для повторного чтения.
 * <p>
 * В WebFlux тело запроса — одноразовый {@link Flux}{@code <DataBuffer>}. Фильтр читает его один раз,
 * кладёт байты в эту обёртку и отдаёт контроллеру новый поток из копии.
 */
public final class CachedBodyServerHttpRequest extends ServerHttpRequestDecorator {

  private final byte[] cachedBody;

  /**
   * Создаёт обёртку с кэшированным телом.
   *
   * @param delegate   исходный запрос.
   * @param cachedBody байты тела; {@code null} заменяется пустым массивом.
   */
  public CachedBodyServerHttpRequest(ServerHttpRequest delegate, byte[] cachedBody) {
    super(delegate);
    this.cachedBody = Objects.isNull(cachedBody)
        ? AuditExchangeAttributeNames.EMPTY_BODY
        : cachedBody;
  }

  /**
   * Повторно отдаёт кэшированное тело как поток {@link DataBuffer}.
   *
   * @return тело запроса
   */
  @Override
  public Flux<DataBuffer> getBody() {
    if (cachedBody.length == 0) {
      return Flux.empty();
    }
    return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(cachedBody));
  }
}
