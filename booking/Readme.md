# Сервис бронирования

## Требования

- JDK 25
- Gradle 9.8.2
- Доступный hotels-service для полноценного сценария создания бронирования
  (по умолчанию `http://localhost:8081`)

База данных встроенная: H2 в in-memory режиме. PostgreSQL и отдельный контейнер БД не нужны.

## Переменные окружения

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| `SERVER_PORT` | `8080` | Порт приложения |
| `DB_URL` | `jdbc:h2:mem:booking;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | JDBC URL H2 |
| `DB_USERNAME` | `sa` | Пользователь H2 |
| `DB_PASSWORD` | пусто | Пароль H2 |
| `DEMO_SEED_ENABLED` | `true` | Заполнять ли БД демо-данными при старте |
| `JWT_SECRET` | dev-значение из `application.yaml` | Секрет HMAC-SHA256, не короче 32 байт |
| `JWT_EXPIRATION_SECONDS` | `3600` | Время жизни JWT в секундах |
| `JWT_ISSUER` | `booking-service` | Issuer JWT |
| `HOTELS_BASE_URL` | `http://localhost:8081` | Базовый URL hotels-service |
| `HOTELS_CONNECT_TIMEOUT` | `2s` | Таймаут соединения с hotels-service |
| `HOTELS_READ_TIMEOUT` | `5s` | Таймаут чтения ответа |
| `HOTELS_MAX_ATTEMPTS` | `3` | Максимальное число попыток вызова |
| `HOTELS_INITIAL_BACKOFF` | `200ms` | Начальная задержка между повторами |
| `HOTELS_BACKOFF_MULTIPLIER` | `2.0` | Множитель экспоненциального backoff |

## База данных H2

Приложение использует in-memory H2. Схема создаётся Flyway при старте из
`src/main/resources/db/migration`, отдельно накатывать миграции не нужно.

H2 Console доступна по адресу:

```text
http://localhost:8080/h2-console
```

Параметры подключения:

| Параметр | Значение |
|---|---|
| JDBC URL | `jdbc:h2:mem:booking` |
| User Name | `sa` |
| Password | пусто |

## Демо-данные

При старте приложения выполняется `DemoDataSeeder` и создаёт пользователей и бронирования
для проверки API. Сидинг идемпотентен: если пользователи уже есть, данные повторно не создаются.
Отключить сидинг можно через `DEMO_SEED_ENABLED=false`.

Пользователи:

| Логин | Пароль | Роль |
|---|---|---|
| `admin` | `admin12345` | ADMIN |
| `user` | `user12345` | USER |

Бронирования:

| ID | Владелец | roomId | Период | Статус |
|---|---|---|---|---|
| 1 | `user` | 101 | сегодня +1 … +3 | CONFIRMED |
| 2 | `user` | 102 | сегодня +5 … +7 | PENDING |
| 3 | `user` | 103 | сегодня -5 … -3 | CANCELLED |
| 4 | `admin` | 201 | сегодня +10 … +12 | CONFIRMED |

ID детерминированы для чистой in-memory БД. Фактические ID и данные также пишутся в лог при старте.

## Запуск приложения

```bash
gradle bootRun
```

С явными параметрами:

```bash
JWT_SECRET=mySecretKeyForJWTShouldBeLongEnoughAndSecureInProduction \
HOTELS_BASE_URL=http://localhost:8081 \
gradle bootRun
```

Если используется SDKMAN и команда `gradle` не найдена:

```bash
source ~/.sdkman/bin/sdkman-init.sh
```

### Запуск в Docker

Сначала соберите jar, затем запустите контейнер:

```bash
gradle build
docker compose up --build
```

## Сборка

```bash
gradle build
```

Во время сборки:
- генерируются DTO из `src/main/resources/openapi.yml`;
- запускаются тесты;
- собирается исполняемый jar в `build/libs/booking-0.0.1-SNAPSHOT.jar`.

## Тесты

Все тесты:

```bash
gradle test
```

Тесты используют профиль `test` и отдельную in-memory H2, внешняя БД не нужна.
Демо-сидинг в тестах отключён через `app.demo.seed-enabled=false`.

Отдельные тесты:

```bash
gradle test --tests 'com.example.booking.service.BookingServiceTest'
gradle test --tests 'com.example.booking.AuthFlowIntegrationTest'
gradle test --tests 'com.example.booking.BookingApplicationTests'
```

HTML-отчёт: `build/reports/tests/test/index.html`.

## Генерация DTO из OpenAPI

```bash
gradle openApiGenerate
```

- исходный контракт: `src/main/resources/openapi.yml`;
- сгенерированные классы: `build/generated/openapi/src/main/java/com/example/booking/model`.

Классы запросов и ответов (`*Dto`) генерируются автоматически, вручную их не редактируем.
Задача `openApiGenerate` также выполняется перед `compileJava`.

## Клиент hotels-service

Взаимодействие с hotels-service реализовано через Spring Cloud OpenFeign:

- декларативный клиент: `client/HotelsFeignClient.java`;
- обёртка с маппингом ошибок: `client/FeignHotelAvailabilityClient.java`;
- конфигурация Feign: `client/HotelsFeignConfiguration.java`;
- таймауты и повторы задаются переменными `HOTELS_*`;
- ответы 5xx и 429 помечаются как retryable, повторы выполняются с экспоненциальным backoff.

## Быстрая проверка API

Авторизация демо-пользователя:

```bash
curl -s -X POST http://localhost:8080/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user12345"}'
```

Авторизация администратора:

```bash
curl -s -X POST http://localhost:8080/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin12345"}'
```

Получить токен и список бронирований:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user12345"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')

curl -s http://localhost:8080/bookings \
  -H "Authorization: Bearer $TOKEN"
```

Получить бронирование по id:

```bash
curl -s http://localhost:8080/booking/1 \
  -H "Authorization: Bearer $TOKEN"
```

Отменить бронирование:

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE http://localhost:8080/booking/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Request-Id: demo-cancel-1'
```

Регистрация нового пользователя:

```bash
curl -s -X POST http://localhost:8080/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"john","password":"secret123"}'
```

Создание бронирования (требуется доступный hotels-service и существующий номер):

```bash
curl -s -X POST http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Request-Id: demo-request-1' \
  -d '{"roomId":101,"autoSelect":false,"startDate":"2026-11-20","endDate":"2026-11-22"}'
```
