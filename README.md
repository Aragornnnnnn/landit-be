# landit-be

Landit 백엔드 서버입니다. Java 21과 Spring Boot 4 기반으로 REST API, JPA, Flyway, Security, Actuator, OpenAPI 문서화를 사용합니다.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring WebMVC
- Spring Data JPA
- Spring Security
- Flyway
- PostgreSQL
- springdoc-openapi
- Sentry, Logback
- JUnit 5, H2

## Requirements

- Java 21
- PostgreSQL

## Environment

로컬 실행 시 필요한 주요 환경변수입니다.

```bash
DB_URL=jdbc:postgresql://localhost:5432/landit
DB_USERNAME=landit
DB_PASSWORD=landit
SENTRY_DSN=
SENTRY_ENVIRONMENT=local
SENTRY_TRACES_SAMPLE_RATE=0.0
```

## Run

```bash
./gradlew bootRun
```

## Test

```bash
./gradlew test
```

## API Docs

애플리케이션 실행 후 Swagger UI에서 API 문서를 확인합니다.

```text
http://localhost:8080/swagger-ui/index.html
```

## Health Check

```text
http://localhost:8080/actuator/health
```
