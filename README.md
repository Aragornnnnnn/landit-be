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

애플리케이션은 DB 연결 정보를 직접 조립하지 않고 환경변수에서 그대로 읽습니다. 배포 환경에서는 IaC가 AWS SSM Parameter Store 값을 환경변수로 주입합니다.

SSM Parameter Store 경로는 다음 이름을 사용합니다. 실제 값은 repo, 로그, 테스트 출력, 문서에 남기지 않습니다.

- `/landit/develop/DB_URL`
- `/landit/develop/DB_USERNAME`
- `/landit/develop/DB_PASSWORD`
- `/landit/prod/DB_URL`
- `/landit/prod/DB_USERNAME`
- `/landit/prod/DB_PASSWORD`

로컬 실행 시에는 `.env.example`을 참고해 secret 없는 placeholder를 실제 로컬 환경변수로 설정합니다. 현재 애플리케이션은 `.env` 파일을 직접 로딩하지 않습니다.

```bash
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:postgresql://<host>:<port>/<database>?sslmode=require&prepareThreshold=0
DB_USERNAME=<db-username>
DB_PASSWORD=<db-password>
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
