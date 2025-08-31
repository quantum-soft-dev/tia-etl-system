# Документация сервиса File Scanner

## 📋 Общее описание

File Scanner Service - это микросервис для автоматического сканирования директорий и обнаружения файлов в рамках ETL-платформы TIA (Telecommunications Intelligence Analytics) для правительства Либерии. Сервис предназначен для обработки телекоммуникационных данных от операторов Orange и MTN в форматах ASN.1 и CSV для налогового контроля и мониторинга качества услуг.

### Основные функции
- Автоматическое сканирование директорий по расписанию
- Обнаружение и валидация файлов по заданным паттернам
- Управление очередью файлов для обработки через Redis
- Отслеживание статуса обработки файлов
- Сбор метрик и мониторинг производительности
- REST API для управления задачами сканирования

## 🏗️ Архитектура

### Используемые технологии

- **Язык программирования**: Kotlin 2.0.21
- **Фреймворк**: Spring Boot 3.3.5
- **База данных**: PostgreSQL с Liquibase для миграций
- **Кэширование и очереди**: Redis
- **Планировщик задач**: Quartz Scheduler
- **Метрики**: Micrometer с Prometheus
- **API документация**: OpenAPI/Swagger 2.3.0
- **Тестирование**: JUnit 5, MockK, Testcontainers
- **Контейнеризация**: Docker с Testcontainers для интеграционных тестов

### Структура проекта

```
services/file-scanner/
├── src/main/kotlin/com/quantumsoft/tia/scanner/
│   ├── components/           # Основные компоненты бизнес-логики
│   │   ├── DirectoryScanner  # Сканирование директорий
│   │   ├── FileValidator     # Валидация файлов
│   │   └── QueueManager      # Управление очередью Redis
│   ├── config/               # Конфигурация приложения
│   │   ├── OpenApiConfig     # Swagger/OpenAPI настройки
│   │   ├── QuartzConfig      # Конфигурация планировщика
│   │   └── StartupInitializer # Инициализация при запуске
│   ├── controllers/          # REST API контроллеры
│   │   ├── actuator/         # Actuator endpoints
│   │   ├── files/            # Управление файлами
│   │   ├── health/           # Health check
│   │   ├── jobs/             # Управление задачами
│   │   └── metrics/          # Метрики и статистика
│   ├── dto/                  # Data Transfer Objects
│   ├── entities/             # JPA сущности
│   ├── metrics/              # Сбор метрик
│   ├── models/               # Доменные модели
│   ├── repositories/         # JPA репозитории
│   ├── scheduler/            # Планировщик задач
│   └── services/             # Бизнес-сервисы
└── src/main/resources/
    ├── application.yml       # Основная конфигурация
    └── db/changelog/         # Liquibase миграции
```

## 🔌 API Endpoints

### Управление задачами сканирования

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/api/v1/scanner/jobs` | Получить список задач сканирования |
| GET | `/api/v1/scanner/jobs/{jobId}` | Получить детали задачи |
| POST | `/api/v1/scanner/jobs` | Создать новую задачу |
| PUT | `/api/v1/scanner/jobs/{jobId}` | Обновить задачу |
| DELETE | `/api/v1/scanner/jobs/{jobId}` | Удалить задачу |

### Выполнение задач

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/api/v1/scanner/jobs/{jobId}/trigger` | Запустить задачу немедленно |
| GET | `/api/v1/scanner/jobs/{jobId}/executions` | История выполнений задачи |
| GET | `/api/v1/scanner/jobs/{jobId}/statistics` | Статистика задачи |

### Управление файлами

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/api/v1/scanner/files` | Поиск и фильтрация файлов |
| GET | `/api/v1/scanner/files/{fileId}` | Детали файла |
| POST | `/api/v1/scanner/files/{fileId}/retry` | Повторить обработку файла |
| DELETE | `/api/v1/scanner/files/{fileId}` | Удалить запись о файле |
| GET | `/api/v1/scanner/files/statistics` | Статистика по файлам |
| POST | `/api/v1/scanner/files/cleanup` | Очистка старых файлов |

### Метрики и мониторинг

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/api/v1/scanner/health` | Проверка здоровья сервиса |
| GET | `/api/v1/scanner/metrics` | Базовые метрики |
| GET | `/api/v1/scanner/metrics/summary` | Сводка метрик |
| GET | `/api/v1/scanner/metrics/queue` | Метрики очереди |
| GET | `/api/v1/scanner/metrics/performance` | Метрики производительности |
| POST | `/api/v1/scanner/metrics/reset` | Сброс метрик |

### Actuator Endpoints

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/file-scanner/actuator/health` | Spring Boot health check |
| GET | `/file-scanner/actuator/info` | Информация о приложении |
| GET | `/file-scanner/actuator/metrics` | Детальные метрики |
| GET | `/file-scanner/actuator/prometheus` | Метрики для Prometheus |

## 📦 Основные компоненты

### DirectoryScanner

**Назначение**: Рекурсивное сканирование директорий для обнаружения файлов по заданным паттернам.

**Ключевые методы**:
- `scan(config: ScanConfiguration): ScanResult` - выполняет сканирование директории

**Функциональность**:
- Поддержка glob-паттернов для фильтрации файлов
- Рекурсивное сканирование с ограничением глубины
- Фильтрация по размеру файла
- Вычисление SHA-256 хэша файлов
- Обработка символических ссылок (опционально)
- Таймаут сканирования
- Сбор метрик производительности

### QueueManager

**Назначение**: Управление распределенной очередью файлов через Redis.

**Ключевые методы**:
- `queueFile(request: QueueRequest): QueueResult` - добавление файла в очередь
- `batchQueue(requests: List<QueueRequest>): BatchQueueResult` - пакетное добавление
- `moveToDeadLetter(queueId: String, reason: String): Boolean` - перемещение в DLQ
- `getQueueStatistics(): QueueStatistics` - статистика очереди
- `cleanupExpiredLocks(): Int` - очистка просроченных блокировок

**Функциональность**:
- Распределенные блокировки для предотвращения дубликатов
- Приоритезация файлов (HIGH, NORMAL, LOW)
- Dead Letter Queue для проблемных файлов
- Retry механизм с подсчетом попыток
- Статистика и метрики очереди
- Автоматическая очистка устаревших блокировок

### JobScheduler

**Назначение**: Планирование и выполнение задач сканирования через Quartz.

**Ключевые методы**:
- `scheduleJob(scanJob: ScanJob)` - планирование задачи
- `triggerJobNow(jobId: UUID)` - немедленный запуск
- `pauseJob(jobId: UUID)` - приостановка задачи
- `resumeJob(jobId: UUID)` - возобновление задачи
- `isJobRunning(jobId: UUID): Boolean` - проверка статуса

**Функциональность**:
- Поддержка CRON и фиксированных интервалов
- Кластерная поддержка через Quartz JDBC JobStore
- Обработка пропущенных запусков (misfire handling)
- Автоматический перезапуск задач при старте сервиса

## 📦 Сервисы

### ScanJobService

**Назначение**: Управление жизненным циклом задач сканирования.

**Основные методы**:
```kotlin
- findJobs(active: Boolean?, parserId: String?, pageable: Pageable): Page<ScanJobDto>
- findById(id: UUID): ScanJobDetailDto?
- createJob(request: CreateScanJobRequest): ScanJobDto
- updateJob(id: UUID, request: UpdateScanJobRequest): ScanJobDto?
- deleteJob(id: UUID): Boolean
- triggerScan(jobId: UUID, force: Boolean): ScanExecutionDto?
- getExecutions(jobId: UUID, pageable: Pageable): Page<ScanExecutionDto>
- getJobStatistics(jobId: UUID): JobStatisticsDto?
```

**Зависимости**:
- ScanJobRepository - персистентность задач
- ScanJobExecutionRepository - история выполнений
- ScannedFileRepository - управление файлами
- JobScheduler - интеграция с планировщиком

### FileStatusService

**Назначение**: Управление статусом и жизненным циклом отсканированных файлов.

**Основные методы**:
```kotlin
- queryFiles(filter: FileStatusFilter, pageable: Pageable): Page<FileStatusDto>
- getFile(fileId: UUID): FileStatusDto?
- retryFile(fileId: UUID): FileStatusDto?
- deleteFile(fileId: UUID): Boolean
- getStatistics(jobId: UUID?): FileStatisticsDto
- cleanup(request: CleanupRequest): CleanupResultDto
```

## 🗄️ Модели данных

### Основные сущности

#### ScanJob
```kotlin
data class ScanJob(
    val id: UUID,
    val name: String,                    // Уникальное имя задачи
    val description: String?,             // Описание
    val sourceDirectory: String,         // Директория для сканирования
    val filePattern: String,             // Паттерн файлов (glob)
    val scanIntervalType: ScanIntervalType, // CRON или FIXED
    val scanIntervalValue: String,       // Значение интервала
    val maxFileSizeMb: Int,              // Макс. размер файла
    val recursiveScan: Boolean,          // Рекурсивное сканирование
    val maxDepth: Int,                   // Макс. глубина рекурсии
    val priority: Int,                   // Приоритет (0-10)
    val parserId: String,                // ID парсера для обработки
    val isActive: Boolean,               // Активность задачи
    val createdAt: Instant,
    val updatedAt: Instant
)
```

#### ScannedFileEntity
```kotlin
data class ScannedFileEntity(
    val id: UUID,
    val scanJob: ScanJob,                // Связанная задача
    val filePath: String,                // Полный путь к файлу
    val fileName: String,                // Имя файла
    val fileSizeBytes: Long,             // Размер в байтах
    val fileHash: String,                // SHA-256 хэш
    val fileModifiedAt: Instant,         // Дата модификации файла
    val status: FileStatus,              // Статус обработки
    val queueId: String?,                // ID в очереди
    val parserId: String?,               // ID используемого парсера
    val discoveredAt: Instant,           // Время обнаружения
    val processedAt: Instant?,          // Время обработки
    val errorMessage: String?           // Сообщение об ошибке
)
```

#### ScanJobExecution
```kotlin
data class ScanJobExecution(
    val id: UUID,
    val scanJob: ScanJob,                // Связанная задача
    val instanceId: String,              // ID инстанса сканера
    val status: ExecutionStatus,         // RUNNING, COMPLETED, FAILED
    val startedAt: Instant,              // Время начала
    val completedAt: Instant?,          // Время завершения
    val filesDiscovered: Int,           // Найдено файлов
    val filesQueued: Int,               // Добавлено в очередь
    val filesSkipped: Int,              // Пропущено файлов
    val durationMs: Long?,              // Длительность в мс
    val errorMessage: String?           // Сообщение об ошибке
)
```

### Перечисления (Enums)

```kotlin
enum class FileStatus {
    DISCOVERED,    // Файл обнаружен
    QUEUED,       // В очереди на обработку
    PROCESSING,   // Обрабатывается
    COMPLETED,    // Успешно обработан
    FAILED,       // Ошибка обработки
    SKIPPED       // Пропущен
}

enum class ExecutionStatus {
    RUNNING,      // Выполняется
    COMPLETED,    // Успешно завершено
    FAILED        // Завершено с ошибкой
}

enum class ScanIntervalType {
    CRON,         // Cron-выражение
    FIXED         // Фиксированный интервал
}

enum class Priority {
    HIGH,         // Высокий приоритет
    NORMAL,       // Обычный приоритет
    LOW           // Низкий приоритет
}
```

## 🔄 Диаграммы Flow

### Процесс сканирования директории

```
1. JobScheduler запускает ScannerJob по расписанию
   ↓
2. ScannerJob создает ScanJobExecution со статусом RUNNING
   ↓
3. DirectoryScanner выполняет сканирование директории
   ├─ Проверка существования директории
   ├─ Рекурсивный обход с учетом maxDepth
   ├─ Фильтрация по glob-паттерну
   ├─ Проверка размера файла
   └─ Вычисление SHA-256 хэша
   ↓
4. Для каждого найденного файла:
   ├─ Проверка дубликатов в БД по hash
   ├─ QueueManager добавляет в Redis очередь
   ├─ Установка распределенной блокировки
   └─ Сохранение ScannedFileEntity в БД
   ↓
5. Обновление ScanJobExecution со статусом COMPLETED
   ↓
6. MetricsCollector записывает метрики выполнения
```

### Процесс обработки API запроса создания задачи

```
1. POST /api/v1/scanner/jobs
   ↓
2. JobsController.createJob()
   ├─ Валидация CreateScanJobRequest
   └─ Проверка уникальности имени
   ↓
3. ScanJobService.createJob()
   ├─ Создание ScanJob entity
   ├─ Сохранение в PostgreSQL
   └─ Возврат ScanJobDto
   ↓
4. JobScheduler.scheduleJob()
   ├─ Создание Quartz JobDetail
   ├─ Создание Trigger (CRON/FIXED)
   └─ Регистрация в Quartz Scheduler
   ↓
5. HTTP 201 Created с ScanJobDto
```

### Процесс управления очередью

```
1. QueueManager.queueFile()
   ↓
2. Проверка распределенной блокировки
   ├─ Ключ: lock:{fileHash}
   ├─ TTL: 5 минут
   └─ Instance ID как значение
   ↓
3. Если блокировка получена:
   ├─ Сериализация QueueMessage в JSON
   ├─ Добавление в Redis List по приоритету
   │   ├─ queue:high
   │   ├─ queue:normal
   │   └─ queue:low
   ├─ Обновление счетчиков статистики
   └─ Запись метрик
   ↓
4. Возврат QueueResult с queueId
```

## 🔗 Зависимости между компонентами

### Граф зависимостей

```
FileScannerApplication
    ├─ @EnableScheduling → Quartz активация
    ├─ @EnableAsync → Асинхронная обработка
    └─ Spring Boot автоконфигурация
        ├─ JPA/Hibernate
        ├─ Redis
        ├─ Liquibase
        └─ Micrometer

Controllers Layer
    ├─ JobsController → ScanJobService
    ├─ FilesController → FileStatusService
    ├─ MetricsController → MetricsCollector
    └─ HealthController → Redis, PostgreSQL checks

Service Layer
    ├─ ScanJobService
    │   ├─ ScanJobRepository
    │   ├─ ScanJobExecutionRepository
    │   ├─ ScannedFileRepository
    │   └─ JobScheduler
    └─ FileStatusService
        ├─ ScannedFileRepository
        └─ QueueManager

Component Layer
    ├─ DirectoryScanner
    │   └─ MetricsCollector
    ├─ QueueManager
    │   ├─ RedisTemplate
    │   ├─ ObjectMapper
    │   └─ MetricsCollector
    └─ FileValidator

Scheduler Layer
    ├─ JobScheduler
    │   ├─ Quartz Scheduler
    │   └─ ScanJobRepository
    └─ ScannerJob (QuartzJobBean)
        ├─ DirectoryScanner
        ├─ QueueManager
        └─ Repositories

Repository Layer (JPA)
    ├─ ScanJobRepository
    ├─ ScanJobExecutionRepository
    └─ ScannedFileRepository
```

## 🔧 Конфигурация

### Основные параметры (application.yml)

```yaml
scanner:
  instance-id: ${HOSTNAME:scanner-1}  # ID инстанса
  scan:
    default-timeout: 5m               # Таймаут сканирования
    max-concurrent-scans: 3           # Макс. параллельных сканов
    file-size-limit: 1024MB           # Лимит размера файла
  queue:
    batch-size: 100                   # Размер батча для очереди
    lock-timeout: 5m                  # Таймаут блокировки
    cleanup-interval: 1h              # Интервал очистки
  metrics:
    enabled: true                     # Включение метрик
    collection-interval: 30s          # Интервал сбора

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tia_etl
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  
  data.redis:
    host: localhost
    port: 6379
    lettuce.pool:
      max-active: 10
      max-idle: 5
  
  quartz:
    job-store-type: jdbc              # Персистентность в БД
    properties:
      org.quartz:
        scheduler:
          instanceId: AUTO
          instanceName: FileScannerScheduler
        jobStore:
          isClustered: true            # Кластерный режим
        threadPool:
          threadCount: 10
```

## 🧪 Тестирование

### Структура тестов

```
src/test/kotlin/
├── components/
│   ├── DirectoryScannerTest      # Unit тесты сканера
│   ├── QueueManagerTest           # Unit тесты очереди
│   └── FileValidatorTest          # Unit тесты валидатора
├── controllers/
│   ├── ScanJobControllerTest      # MockMvc тесты API
│   ├── FileStatusControllerTest   # MockMvc тесты API
│   └── MetricsControllerTest      # MockMvc тесты API
├── services/
│   ├── ScanJobServiceTest         # Бизнес-логика тесты
│   └── FileStatusServiceTest      # Бизнес-логика тесты
├── integration/
│   ├── BaseIntegrationTest        # Базовый класс
│   ├── RedisQueueIntegrationTest  # Redis интеграция
│   └── ScanJobRepositoryIntegrationTest # JPA тесты
├── e2e/
│   ├── BaseE2ETest                # Testcontainers база
│   ├── JobControllerE2ETest       # E2E тесты задач
│   ├── FileOperationsE2ETest      # E2E тесты файлов
│   └── MetricsE2ETest             # E2E тесты метрик
└── config/
    ├── TestConfiguration          # Тестовая конфигурация
    └── TestQuartzConfig           # Mock Quartz
```

### Тестовые профили

- **unit** - только unit тесты (по умолчанию)
- **integration** - интеграционные тесты с Testcontainers
- **e2e** - end-to-end тесты
- **all** - все тесты

### Покрытие тестами

- Unit тесты: ~80% покрытие бизнес-логики
- Интеграционные тесты: Redis, PostgreSQL через Testcontainers
- E2E тесты: критические сценарии использования
- Архитектурные тесты: ArchUnit для проверки зависимостей

## 🚀 Deployment и эксплуатация

### Системные требования

- **JVM**: Java 21+
- **Память**: минимум 2GB heap
- **CPU**: 2+ ядра рекомендуется
- **PostgreSQL**: версия 14+
- **Redis**: версия 6+

### Переменные окружения

```bash
# База данных
DB_HOST=postgresql-server
DB_PORT=5432
DB_NAME=tia_etl
DB_USER=tia_user
DB_PASSWORD=secure_password

# Redis
REDIS_HOST=redis-server
REDIS_PORT=6379

# Приложение
SCANNER_INSTANCE_ID=scanner-prod-1
SCANNER_MAX_CONCURRENT_SCANS=5
SCANNER_FILE_SIZE_LIMIT=2048MB

# Метрики
METRICS_ENABLED=true
METRICS_COLLECTION_INTERVAL=30s
```

### Health Checks

Сервис предоставляет health endpoints для мониторинга:

- `/api/v1/scanner/health` - кастомный health check
  - Проверка PostgreSQL соединения
  - Проверка Redis соединения
  - Проверка QueueManager статуса
  
- `/file-scanner/actuator/health` - Spring Boot health
  - Детальная информация о компонентах
  - Liveness и Readiness probes

### Метрики Prometheus

Основные метрики:
- `scanner_files_scanned_total` - общее количество отсканированных файлов
- `scanner_files_queued_total` - файлов добавлено в очередь
- `scanner_errors_total` - общее количество ошибок
- `scanner_scan_duration_seconds` - длительность сканирования
- `scanner_queue_depth` - текущая глубина очереди
- `scanner_processing_duration_seconds` - время обработки файлов

### Логирование

Структурированное логирование с уровнями:
- **DEBUG**: детальная информация о сканировании
- **INFO**: основные операции и статусы
- **WARN**: предупреждения и retry операции
- **ERROR**: критические ошибки

Ротация логов:
- Максимальный размер файла: 10MB
- История: 30 дней
- Путь: `/logs/file-scanner.log`

## 🔒 Безопасность

### Аутентификация и авторизация

- Интеграция с Keycloak (планируется)
- Роли: Admin, Operator, Viewer
- JWT токены для API доступа

### Защита данных

- SHA-256 хэширование файлов для дедупликации
- Распределенные блокировки для предотвращения race conditions
- Валидация входных данных на всех уровнях
- SQL injection защита через JPA/Hibernate

### Аудит

- Логирование всех операций создания/изменения/удаления
- Отслеживание пользователей через createdBy/updatedBy
- История выполнения задач в БД

## 💡 Рекомендации и улучшения

### Обнаруженные области для улучшения

1. **Масштабируемость**
   - Реализовать партиционирование таблиц для больших объемов
   - Добавить sharding для Redis при росте нагрузки
   - Оптимизировать batch операции для больших директорий

2. **Отказоустойчивость**
   - Добавить circuit breaker для внешних зависимостей
   - Реализовать graceful shutdown с завершением текущих задач
   - Улучшить retry механизм с exponential backoff

3. **Мониторинг**
   - Добавить трассировку через OpenTelemetry
   - Реализовать алерты для критических метрик
   - Добавить дашборды Grafana

4. **Производительность**
   - Реализовать параллельное сканирование поддиректорий
   - Добавить кэширование для частых запросов
   - Оптимизировать вычисление хэшей для больших файлов

5. **Безопасность**
   - Полная интеграция с Keycloak
   - Шифрование sensitive данных в БД
   - Rate limiting для API endpoints

### Паттерны проектирования

Используемые паттерны:
- **Repository Pattern** - абстракция доступа к данным
- **Service Layer** - бизнес-логика отделена от контроллеров
- **DTO Pattern** - разделение domain и transfer объектов
- **Dependency Injection** - через Spring IoC container
- **Strategy Pattern** - для различных типов интервалов (CRON/FIXED)
- **Observer Pattern** - через Spring Events (планируется)

## 📝 Заключение

File Scanner Service представляет собой хорошо структурированный микросервис с четким разделением ответственности между слоями. Архитектура следует принципам SOLID и паттернам Spring Boot. Сервис готов к production использованию с поддержкой кластеризации, мониторинга и масштабирования.

Основные сильные стороны:
- Модульная архитектура с четким разделением слоев
- Comprehensive тестовое покрытие
- Поддержка распределенной обработки
- Детальные метрики и мониторинг
- Хорошая документация API через OpenAPI

Рекомендуется дальнейшее развитие в направлении улучшения отказоустойчивости, добавления полноценной системы аутентификации и оптимизации для работы с большими объемами данных.
