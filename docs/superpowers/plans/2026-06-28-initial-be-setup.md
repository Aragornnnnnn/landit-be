# Initial BE Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Java 21 and Spring Boot 4 기반의 Landit BE 초기 서버 골격을 만든다.

**Architecture:** 빈 저장소에 Gradle 기반 Spring Boot 애플리케이션을 생성한다. 운영 DB는 PostgreSQL, 테스트 DB는 H2를 사용하고, Flyway, JPA, Security, Validation, Actuator, OpenAPI, Sentry, Logback 기반 로깅을 초기 의존성으로 연결한다.

**Tech Stack:** Java 21, Spring Boot 4.0.7, Gradle Groovy DSL, Spring WebMVC, Spring Data JPA, Spring Validation, Spring Security, Spring Scheduling, Flyway, PostgreSQL, springdoc-openapi, Actuator, Sentry, Logback, JUnit 5, H2, Lombok.

## Global Constraints

- 언어는 Java 21이다.
- 프레임워크는 Spring Boot 4 계열이다.
- springdoc-openapi Initializr 지원 범위 때문에 Spring Boot는 `4.0.7`로 고정한다.
- 빌드 도구는 새 저장소 초기값으로 Gradle Groovy DSL을 사용한다.
- 기본 패키지는 `com.landit.landitbe`로 둔다.
- 새 소스 파일의 첫 줄은 파일 역할을 설명하는 한국어 한 줄 주석이어야 한다.
- 설정은 PostgreSQL 운영 연결과 H2 테스트 연결이 분리되어야 한다.

---

### Task 1: Bootstrap Gradle Spring Boot Project

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `src/main/java/com/landit/landitbe/LanditBeApplication.java`
- Create: `src/test/java/com/landit/landitbe/LanditBeApplicationTests.java`
- Create: `src/main/resources/logback-spring.xml`

**Interfaces:**
- Consumes: Spring Initializr metadata for Boot `4.0.7`.
- Produces: runnable Spring Boot application class `LanditBeApplication`.

- [x] **Step 1: Generate the project skeleton**

Run:

```bash
curl -fsSL 'https://start.spring.io/starter.zip?type=gradle-project&language=java&bootVersion=4.0.7&baseDir=landit-be-bootstrap&groupId=com.landit&artifactId=landit-be&name=landit-be&description=Landit%20backend%20server&packageName=com.landit.landitbe&packaging=jar&javaVersion=21&dependencies=web,data-jpa,validation,security,flyway,postgresql,actuator,lombok,h2,springdoc-openapi' -o /tmp/landit-be-bootstrap.zip
```

Expected: `/tmp/landit-be-bootstrap.zip` is created.

- [x] **Step 2: Copy generated files into the repository**

Run:

```bash
unzip -q /tmp/landit-be-bootstrap.zip -d /tmp/landit-be-bootstrap
rsync -a /tmp/landit-be-bootstrap/landit-be-bootstrap/ /Users/sangmin8817/Soma/landit-be/
```

Expected: Gradle files and `src` tree exist in the repository.

- [x] **Step 3: Add Sentry Logback dependency**

Modify `build.gradle` dependencies:

```groovy
implementation 'io.sentry:sentry-logback:8.16.0'
```

Expected: Sentry is connected through Logback without enabling a Boot 4 incompatible Sentry auto-configuration.

### Task 2: Add Minimal Runtime Configuration

**Files:**
- Create: `src/main/resources/application.yml`
- Create: `src/test/resources/application-test.yml`
- Create: `src/main/resources/db/migration/V1__init.sql`

**Interfaces:**
- Consumes: Boot auto-configuration for DataSource, Flyway, JPA, Actuator, and Logback.
- Produces: default local profile settings and test profile settings.

- [x] **Step 1: Configure local application defaults**

Create `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: landit-be
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/landit}
    username: ${DB_USERNAME:landit}
    password: ${DB_PASSWORD:landit}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info

sentry:
  dsn: ${SENTRY_DSN:}
  environment: ${SENTRY_ENVIRONMENT:local}
  traces-sample-rate: ${SENTRY_TRACES_SAMPLE_RATE:0.0}
```

Expected: application starts with PostgreSQL environment variables when a DB is available.

- [x] **Step 2: Configure test profile**

Create `src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:landit;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

sentry:
  dsn:
```

Expected: tests run against H2 without requiring PostgreSQL.

- [x] **Step 3: Add initial migration**

Create `src/main/resources/db/migration/V1__init.sql`:

```sql
-- 초기 스키마 기준선을 생성하는 Flyway 마이그레이션
```

Expected: Flyway has a valid first migration even before domain tables are designed.

### Task 3: Enable Scheduling and Health Verification

**Files:**
- Modify: `src/main/java/com/landit/landitbe/LanditBeApplication.java`
- Modify: `src/test/java/com/landit/landitbe/LanditBeApplicationTests.java`

**Interfaces:**
- Consumes: Spring scheduling infrastructure.
- Produces: application context that loads with WebMVC, Security, JPA, Flyway, Actuator, Sentry, and Scheduling.

- [x] **Step 1: Enable scheduling**

Modify `LanditBeApplication.java`:

```java
// Landit 백엔드 애플리케이션의 진입점을 정의한다.
package com.landit.landitbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LanditBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LanditBeApplication.class, args);
    }
}
```

Expected: scheduler infrastructure is available for later daily quest and review jobs.

- [x] **Step 2: Load application context with test profile**

Modify `LanditBeApplicationTests.java`:

```java
// Landit 백엔드 애플리케이션 컨텍스트 부팅을 검증한다.
package com.landit.landitbe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class LanditBeApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

Expected: context loads using H2 and Flyway.

### Task 4: Verify and Commit

**Files:**
- Modify: `checklist.md`
- Modify: `context-notes.md`
- Read: Gradle test output

**Interfaces:**
- Consumes: generated Gradle wrapper.
- Produces: verified baseline commit.

- [x] **Step 1: Run tests**

Run:

```bash
./gradlew test
```

Expected: build passes.

- [x] **Step 2: Commit the initial setup**

Run:

```bash
git add .
git commit -m "chore: Spring Boot 초기 설정"
```

Expected: one commit containing the initial BE setup.

## Self-Review

- Spec coverage: Java 21, Spring Boot 4, WebMVC, JPA, Validation, Security, Scheduler, Flyway, PostgreSQL, springdoc-openapi, Actuator, Sentry, Logback, JUnit 5, H2, Lombok are represented.
- Placeholder scan: no `TBD`, `TODO`, or open-ended implementation steps are intentionally left.
- Type consistency: application class and test class paths use the same `com.landit.landitbe` package.
