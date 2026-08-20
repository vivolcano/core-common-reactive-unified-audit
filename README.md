# Реактивный модуль единого аудита

Spring Boot-модуль аудита HTTP-запросов WebFlux-контроллеров. События описываются в YAML и
отправляются в АС Единый Аудит через блокирующий клиент `audit-client-core2`.

Это не аннотация на методе и не отдельный sidecar: библиотека подключается зависимостью, поднимает
автоконфигурацию и после обработки запроса сама выбирает событие по FQCN контроллера и имени
Java-метода. Контроллер не вызывает клиент аудита вручную.

Модуль — WebFlux-порт servlet-версии единого аудита. Контракт YAML (`audit.client`, `audit.model`)
тот же: потребитель меняет артефакт и стек на WebFlux, существующий `application.yml` продолжает
работать.

> Важно: модуль рассчитан **строго на reactive WebFlux stack**:
> - Spring WebFlux
> - Netty (`ServerHttpRequest` / `ServerHttpResponse`)
> - `WebFilter`
>
> Автоконфигурация включается только при reactive web-приложении
> (`@ConditionalOnWebApplication(type = REACTIVE)`). Spring MVC, Servlet API, `OncePerRequestFilter`
> и `HandlerInterceptor` не поддерживаются — для них остаётся WEB-версия.

Клиент АС Единый Аудит по-прежнему блокирующий. Модуль вызывает `AuditService.audit` /
`AuditService.register` только на `boundedElastic` с префиксом потоков `unified-audit`, чтобы не
блокировать event loop Netty.

## Содержание

- [Что делает и чего не делает](#что-делает-и-чего-не-делает)
- [Как устроен конвейер](#как-устроен-конвейер)
- [Как работает запрос](#как-работает-запрос)
- [Совместимость с WEB-версией](#совместимость-с-web-версией)
- [Подключение](#подключение)
- [Быстрый старт](#быстрый-старт)
- [Конфигурация](#конфигурация)
- [Источники параметров](#источники-параметров)
- [Выбор события](#выбор-события)
- [Условия](#условия)
- [Шапка события](#шапка-события)
- [Маскирование](#маскирование)
- [Тела запроса и ответа](#тела-запроса-и-ответа)
- [Исключённые пути](#исключённые-пути)
- [Логирование](#логирование)
- [Ограничения](#ограничения)
- [Рекомендации](#рекомендации)
- [Сборка](#сборка)

## Что делает и чего не делает

### Делает

- После HTTP-цепочки находит `HandlerMethod`, сопоставляет его с `audit.model` и отправляет одно
  событие в АС Единый Аудит.
- На старте приложения регистрирует метамодель событий (`ApplicationReadyEvent`).
- Извлекает параметры из path/query/`@RequestBody`, заголовков, JWT-claims, статуса и тела ответа.
- Выбирает событие по `conditions` или по флагу `success` и HTTP-статусу (успех — `200`–`308`).
- Маскирует указанные JSON-поля, если в YAML задан `masks`.
- Аудирует и успешный ответ, и ошибку цепочки.
- Для GET не читает тело запроса (как WEB-версия). Если контроллер тело не подписался, фильтр
  дочитывает его после цепочки, чтобы YAML `request` / поле JSON всё равно сработали.

### Не делает

- **Не требует** аннотаций вроде `@Audit` на контроллере. Привязка только через YAML:
  `controller-class` + имя метода.
- **Не является** клиентом Kafka/АС сами по себе: сеть делает `audit-client-core2`. Этот модуль
  только собирает событие и вызывает блокирующий `AuditService` на elastic-планировщике.
- **Не аудирует** пути без `HandlerMethod` (статика, 404 без контроллера) и методы, которых нет в
  `audit.model`.
- **Не ломает HTTP**, если аудит не смог извлечь поле или клиент АС вернул ошибку: сбой отправки
  логируется и глотается.
- **Не поддерживает** Spring MVC. Для servlet-стека остаётся WEB-модуль.
- **Не подменяет** `audit.client` / `audit.model`: ключи, имена событий, операторы условий и шапка
  JWT те же, что в WEB-версии.

## Как устроен конвейер

В запросе три слоя. Их нельзя путать: фильтр готовит тела, резолвер выбирает YAML-событие, клиент
отправляет его в АС.

Bitbucket не рендерит `mermaid` без отдельного плагина, поэтому схема ниже — обычный текст.

```
HTTP-запрос
    │
    ▼
AuditWebFilter
    │  не GET  → CapturingServerHttpRequest  (копия тела в лимите аудита)
    │  всегда  → CapturingServerHttpResponse
    │
    ▼
WebFlux-цепочка (контроллер)
    │
    ▼
после цепочки (успех или ошибка)
    │  непрочитанное тело запроса дочитывается
    │  тела кладутся в атрибуты обмена
    ▼
AuditEventResolver
    │  HandlerMethod + audit.model
    │  JWT claims из ReactiveSecurityContext
    │  conditions / success
    ▼
AuditClientService  →  boundedElastic (unified-audit-*)
    │
    ▼
AuditService.audit  (блокирующий клиент АС Единый Аудит)
```

На старте отдельно:

```
ApplicationReadyEvent
    → MetaModelConverter (audit.model + audit.client.meta-model)
    → AuditService.register   на том же elastic-планировщике
```

## Как работает запрос

1. Фильтр смотрит путь. Совпадение с `audit.reactive.exclude-path-patterns` — цепочка без аудита и
   без копирования тел.
2. Для не-GET запрос оборачивается декоратором: первый подписчик (codec / `@RequestBody`) получает
   живой поток, копия идёт в кэш аудита, пока не превышен `max-body-size`.
3. Ответ всегда оборачивается: HTTP-запись не обрывается лимитом аудита.
4. После контроллера, если тело запроса никто не читал, фильтр дочитывает его сам.
5. Резолвер ищет `HandlerMethod`. Нет метода или нет YAML-маппинга — тихий пропуск.
6. Среди событий метода сначала ищутся те, у кого `conditions` совпали. Иначе берётся событие без
   условий с `success`, равным «HTTP 200–308».
7. Параметры извлекаются экстракторами, пустые значения в событие не кладутся.
8. Событие конвертируется в модель SBT и отправляется на `unified-audit-*` потоке.

## Совместимость с WEB-версией

Потребитель, который уже настроил servlet-модуль, **оставляет YAML как есть** и меняет зависимость
на этот артефакт. Префиксы `audit.client` и `audit.model` не переименовывались.

| Ключ | WEB | WebFlux | Комментарий |
| --- | --- | --- | --- |
| `audit.client.config` | да | да | Карта свойств блокирующего `audit-client-core2` |
| `audit.client.meta-model.version` | да | да | Обязательное |
| `audit.client.meta-model.module` | да | да | Обязательное |
| `audit.client.meta-model.subsystem` | да | да | Обязательное |
| `audit.client.meta-model.source-system` | да | да | Обязательное |
| `audit.model.class-events-holders` | да | да | Список контроллеров |
| `controller-class` | да | да | FQCN контроллера |
| `events.<имяМетода>` | да | да | Имя Java-метода, не URL |
| `name` / `description` / `mode` / `success` | да | да | `mode`: `CRITICAL` \| `UNCRITICAL` |
| `params.request` | да | да | Path/query/`@RequestBody` / поле JSON |
| `params.request-header` | да | да | |
| `params.claims` | да | да | JWT `Authentication.details` как `Map` |
| `params.response-code` | да | да | |
| `params.response-header` | да | да | |
| `params.response-body` | да | да | Поле JSON или всё тело |
| `params.path-variable` | да | да | URI-шаблон, без сигнатуры метода |
| `params.*.name` / `description` / `key` / `masks` | да | да | |
| `conditions` + `field` / `operator` / `values` | да | да | Те же операторы |
| Шапка: `sub`, `given_name`, `patronymic`, `family_name`, `sid`, `jti` | да | да | |
| Заголовки `request-id`, `X-Correlation-ID`, `X-Session-ID`, cookie `JSESSIONID` | да | да | |
| Успех HTTP | `200`–`308` | `200`–`308` | Как в WEB-версии |
| GET без тела запроса | да | да | |
| `audit.reactive.max-body-size` | нет | да | Только WebFlux, см. ниже |
| `audit.reactive.exclude-path-patterns` | нет | да | Только WebFlux, см. ниже |

Что **не** нужно менять в YAML при переходе:

- имена событий и параметров;
- FQCN контроллеров и имена методов;
- ключи источников (`request`, `request-header`, `claims`, …);
- операторы `EQUALS`, `IN`, `MATCHES` и остальные.

Что меняется в приложении, а не в модели аудита:

- зависимость на `core-common-reactive-unified-audit`;
- стек сервиса — WebFlux (`spring.main.web-application-type=reactive`);
- клиент АС вызывается с elastic-планировщика, а не с servlet-потока.

Необязательный блок `audit.reactive` на поведение WEB-конфига не влияет, пока его нет. Значения по
умолчанию отличаются от servlet-фильтров `/*` без лимита тела:

- лимит кэша тел — **1 MB**; превышение не ломает HTTP, в событие тело просто не попадает;
- из аудита по умолчанию исключены actuator/swagger/`favicon.ico`.

Чтобы максимально повторить WEB («аудировать все пути, тела без искусственного лимита»), задайте:

```yaml
audit:
  reactive:
    max-body-size: 10MB          # или больше, по профилю сервиса
    exclude-path-patterns: []    # пустой список отключает exclude
```

## Подключение

### Maven

```xml
<dependency>
    <groupId>ru.sbrf.sbererp.core</groupId>
    <artifactId>core-common-reactive-unified-audit</artifactId>
    <version>${core-common-reactive-unified-audit.version}</version>
</dependency>
```

Клиент АС — тот же, что у WEB-версии:

```xml
<dependency>
    <groupId>ru.sbt.pvm</groupId>
    <artifactId>audit-client-core2</artifactId>
    <version>${audit.version}</version>
</dependency>
```

Автоконфигурация подхватывается через
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
`@Enable…` в приложении не нужен: достаточно зависимости, WebFlux и YAML.

Имена параметров методов контроллера должны быть доступны рефлексии (`-parameters`). У Spring Boot
parent это обычно уже включено; если parent нет:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

Без `-parameters` YAML `params.request` сопоставляется с `arg0`/`arg1`, а не с `orderId`.

## Быстрый старт

1. Заменить servlet-модуль аудита на эту зависимость (или добавить её в новый WebFlux-сервис).
2. Оставить `audit.client` и `audit.model` как в WEB-версии.
3. Убедиться, что приложение reactive и контроллеры — WebFlux (`Mono`/`Flux`/`@RestController`).
4. JWT-claims по-прежнему кладутся в `Authentication.details` как `Map<String, Object>`.

Минимальный YAML (те же ключи, что в WEB):

```yaml
audit:
  client:
    config: {}
    meta-model:
      version: "1.0"
      module: erp-module
      subsystem: core
      source-system: ERP
  model:
    class-events-holders:
      - controller-class: com.example.api.OrderController
        events:
          create:
            - name: OrderCreated
              description: Создание заказа
              mode: UNCRITICAL
              success: true
              params:
                request:
                  - name: orderId
                    description: Идентификатор заказа
                response-code:
                  - name: httpStatus
                    description: Код ответа
```

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PostMapping
    public Mono<OrderResponse> create(@RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }
}
```

Имя метода в YAML — `create`, не URL и не HTTP-метод. Полный пример —
[`example/application.yml`](example/application.yml).

## Конфигурация

### `audit.client`

Заголовок метамодели и карта свойств клиента SBT. Все четыре поля `meta-model` обязательны.

| Ключ | Тип | Обязательный | Смысл |
| --- | --- | --- | --- |
| `config` | `Map<String, String>` | нет, `{}` | Свойства `PropertiesAuditConfigBuilder` |
| `meta-model.version` | string | да | Версия метамодели |
| `meta-model.module` | string | да | Модуль |
| `meta-model.subsystem` | string | да | Подсистема события |
| `meta-model.source-system` | string | да | Система-источник |

Содержимое `config` задаёт сам `audit-client-core2` (брокер, таймауты и т.д.). Этот модуль передаёт
карту в билдер, как WEB-версия, и не интерпретирует ключи.

### `audit.model`

Список контроллеров. У каждого:

- `controller-class` — FQCN;
- `events` — карта **имя Java-метода → список событий**.

У события:

| Поле | Тип | Обязательное |
| --- | --- | --- |
| `name` | string | да |
| `description` | string | да |
| `mode` | `CRITICAL` \| `UNCRITICAL` | да |
| `success` | boolean | да |
| `params` | карта источник → список параметров | нет, может быть пустой |
| `conditions` | карта источник → список условий | нет |

Параметр:

| Поле | Смысл |
| --- | --- |
| `name` | Имя в метамодели и в событии |
| `description` | Описание в метамодели |
| `key` | Ключ извлечения, если не совпадает с `name` (имя JSON-поля, заголовка, path-variable) |
| `masks` | JSON-поля, которые нужно вырезать |

Условие:

| Поле | Смысл |
| --- | --- |
| `field` | Имя поля / заголовка / переменной пути |
| `operator` | Оператор из таблицы ниже |
| `values` | Ожидаемые значения |

### `audit.reactive`

Только WebFlux. В WEB-версии блока нет — при миграции можно не добавлять.

| Ключ | По умолчанию | Смысл |
| --- | --- | --- |
| `max-body-size` | `1MB` | Лимит **кэша аудита**, не лимит HTTP |
| `exclude-path-patterns` | actuator, swagger, `/v3/api-docs/**`, `/webjars/**`, `/favicon.ico` | Пути без фильтра аудита |

`null` у `exclude-path-patterns` включает дефолты. **Пустой список** `[]` отключает исключения —
аудируются все пути, как `/*` в WEB-версии.

## Источники параметров

Ключи карты `params` / `conditions` — те же dash-имена, что в WEB:

| YAML-ключ | Откуда значение |
| --- | --- |
| `request` | Параметр метода (`@PathVariable`, `@RequestParam`, `@RequestHeader`, `@RequestBody`, `Pageable`). Если имя из YAML не совпало с параметром метода, но на методе есть `@RequestBody` — поле JSON тела |
| `request-header` | Заголовок запроса (`key` или `name`) |
| `claims` | JWT claim из `Authentication.details` |
| `path-variable` | `HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE`, без сигнатуры метода |
| `response-code` | HTTP-статус (`null` трактуется как `200`) |
| `response-header` | Заголовок ответа |
| `response-body` | Поле JSON ответа; если поля нет — всё тело (с `masks`, если заданы) |

Для `request` с целым телом `key` должен совпасть с именем параметра `@RequestBody`. Остальные
записи в том же списке считаются полями JSON.

## Выбор события

Для метода может быть несколько YAML-событий.

1. Сначала события **с `conditions`**: берётся первое, у которого все условия истинны (AND).
2. Если ни одно условие не совпало — событие **без условий**, у которого `success` совпал со
   статусом ответа (`200`–`308` → `true`, иначе `false`).
3. Если подходящего нет — аудит пропускается, HTTP не меняется.

Так на одном `create` живут `OrderCreated` (`success: true`), `OrderCreateFailed` (`success: false`)
и, например, `OrderCreatedVip` с условием на заголовок.

## Условия

Операторы — те же, что в WEB-версии.

Сравнение значений:

- `EQUALS`, `NOT_EQUALS`
- `IN`, `NOT_IN`
- `MATCHES` (ровно одно regexp-значение)
- `GREATER_THAN`, `LESS_THAN`, `GREATER_OR_EQUAL`, `LESS_OR_EQUAL` (числа как `double`, иначе строки)

Пустота:

- `IS_NULL`, `IS_NOT_NULL`
- `STRING_IS_EMPTY`, `STRING_IS_NOT_EMPTY`
- `STRING_IS_BLANK`, `STRING_IS_NOT_BLANK`

Строки:

- `STRING_CONTAINS` — все фрагменты из `values`
- `STRING_CONTAINS_ANY`
- `STRING_NOT_CONTAINS`
- `STRING_STARTS_WITH`, `STRING_ENDS_WITH`

JSON-массивы (строка вида `[...]`):

- `COLL_IS_EMPTY`, `COLL_IS_NOT_EMPTY`
- `COLL_SIZE_EQUALS`, `COLL_SIZE_GREATER_THAN`, `COLL_SIZE_LESS_THAN`
- `COLL_SIZE_GREATER_OR_EQUAL`, `COLL_SIZE_LESS_OR_EQUAL`

```yaml
conditions:
  request-header:
    - field: X-Audit-Tag
      operator: EQUALS
      values: ["vip"]
  path-variable:
    - field: id
      operator: EQUALS
      values: ["special"]
```

## Шапка события

Заполняется автоматически, YAML для неё не нужен. Ключи JWT те же, что в WEB-версии; источник —
`ReactiveSecurityContextHolder`, `details` аутентификации как `Map`.

| Поле события | Источник | Заглушка |
| --- | --- | --- |
| `userLogin` | claim `sub` | `NO-USER` |
| `userName` | `given_name`, `patronymic`, `family_name` | `NO-USER-NAME` |
| `userNode` | claim `sid` | `NO-CLAIM-SID` |
| `session` | claim `jti`, иначе `X-Correlation-ID`, cookie `JSESSIONID`, `X-Session-ID` | `NO-SESSION` |
| `requestId` | заголовок `request-id` | `NO_REQUEST_ID` |
| `nodeId` | `NAMESPACE` + `HOSTNAME`, иначе IP и hostname | `UNKNOWN-NODE-ID` |

Форматы `jti` / `sid` те же: `jwt_claim_jti:%s`, `jwt_claim_sid:%s`.

## Маскирование

`masks` вырезает поля из JSON, который уходит в параметр события (не из HTTP-ответа клиенту).

- Имя поля: `secret`
- Вложенность через точку: `card.pan`
- JSON Pointer: `/item/secret`

Работает и для целого тела (`REQUEST_BODY`), и для извлечённого объекта/массива.

## Тела запроса и ответа

Фильтр копирует тела **для аудита**. HTTP-поток от этого не обрезается:

- контроллер всегда получает исходное тело;
- клиент всегда получает исходный ответ;
- если размер больше `max-body-size`, кэш аудита пустой, соответствующие YAML-параметры в событие
  не попадают.

MIME для разбора JSON: `application/json`, `application/problem+json`, `text/plain`.

GET тело запроса не кэширует. POST/PUT/PATCH без `@RequestBody` всё равно могут отдать тело в аудит:
фильтр дочитывает непрочитанный поток после цепочки.

Повторный `getBody()` после первого полного прочтения (в лимите) отдаёт replay из кэша — это нужно
кодекам WebFlux, не потребителю YAML.

## Исключённые пути

По умолчанию не аудируются служебные URL. Это **новое** относительно WEB (`/*`). Список:

```
/actuator/**
/swagger-ui.html
/swagger-ui/**
/v3/api-docs
/v3/api-docs/**
/webjars/**
/favicon.ico
```

Чтобы аудировать и их (как WEB), задайте `exclude-path-patterns: []`.

## Логирование

Логгер: `ru.sbrf.sbererp.core.common.reactive.unified.audit`.

| Уровень | Что пишется |
| --- | --- |
| INFO | Имя события, id отправки, версия/модуль метамодели, число событий. Без тел и JWT |
| DEBUG | JSON события и метамодели, кэш тел, разбор JSON, условия |
| WARN | Превышение лимита тела, сбой маскирования/парсинга JSON, нет подходящего события |
| ERROR | Сбой `AuditService.audit` / `register` (HTTP при этом не меняется) |

Для разбора конкретного запроса:

```yaml
logging:
  level:
    ru.sbrf.sbererp.core.common.reactive.unified.audit: DEBUG
```

Не держите DEBUG на проме: в лог попадут тела запросов и метамодель.

## Ограничения

- Только WebFlux. Servlet-фильтры WEB-версии сюда не переносятся.
- `AuditService` блокирующий: вызывать его с event loop нельзя; модуль уже оборачивает вызов.
- YAML привязан к **имени Java-метода**. Переименование метода без правки YAML отключает аудит.
- Несколько событий с условиями: побеждает первое совпавшее в порядке YAML.
- Пустое извлечённое значение в `params` отбрасывается и в АС не уходит.
- Дефолтный exclude и лимит тела — отличия от WEB; их снимают настройками `audit.reactive`.

## Рекомендации

- Копируйте `audit.model` из WEB-сервиса один в один, не переименовывайте ключи.
- Для миграции явно задайте `exclude-path-patterns` и `max-body-size`, если нужно прежнее поведение.
- Держите `-parameters` у компилятора сервиса.
- Не кладите секреты в событие без `masks`.
- Не описывайте в YAML методы, которых нет на контроллере: биндер просто их не увидит.
- Проверяйте привязку на старте по INFO `Binding audit extractors for controller bean ...`.

## Сборка

```bash
mvn test
```

Интеграционные сценарии и эталонный YAML — `src/test/resources/application.yml` и
`ru.sbrf.sbererp.audit.it.UnifiedAuditIntegrationTest`.
