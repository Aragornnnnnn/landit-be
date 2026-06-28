# 초기 BE 셋팅 컨텍스트 노트

## 2026-06-28

- 저장소는 `LICENSE`만 있는 거의 빈 Git 저장소로 확인했다.
- 빌드 도구는 명시되지 않았으므로 Spring Boot 초기 프로젝트의 일반적인 선택지인 Gradle Groovy DSL을 사용한다.
- 기본 패키지는 `com.landit.landitbe`로 정한다. `artifactId`의 하이픈을 Java package에 직접 쓸 수 없기 때문이다.
- Spring Initializr metadata 기준 기본 Boot 4 버전은 `4.1.0.RELEASE`이지만, `springdoc-openapi` dependency id가 `[3.5.0.RELEASE,4.1.0.M1)`까지만 지원되어 Spring Boot는 `4.0.7`로 고정한다.
- Maven metadata 기준 Spring Boot 4 BOM 실제 배포 버전은 `.RELEASE` 접미사 없이 `4.0.7`, `4.1.0` 형식이다.
- Spring Scheduler는 별도 starter가 아니라 Spring Framework의 scheduling 기능이므로 애플리케이션 진입점에 `@EnableScheduling`을 붙여 초기 활성화한다.
- Logback은 Spring Boot starter logging에 포함되는 기본 로깅 구현이라 별도 의존성을 추가하지 않는다.
- Sentry Spring Boot starter `8.16.0`은 Boot 4.0.7에서 제거된 `org.springframework.boot.web.client.RestClientCustomizer`를 참조해 컨텍스트 부팅에 실패한다.
- Boot 4 요구를 유지하기 위해 Sentry는 `io.sentry:sentry-logback:8.16.0`과 `logback-spring.xml` appender로 연동한다.
- Initializr가 Boot 4.0.7 조합에서 springdoc-openapi `3.0.2`를 생성했으므로, 별도 springdoc 버전 override는 추가하지 않는다.
- H2는 사용 목적이 테스트이므로 runtime dependency가 아니라 `testRuntimeOnly`로 제한한다.
- `./gradlew test`는 Sentry를 Logback appender로 바꾼 뒤 통과했다.
- Initializr가 만든 `HELP.md`는 `.gitignore` 대상이어서 커밋되지 않는 생성 문서로 남지 않도록 제거했다.
- 초기 설정은 하나의 논리 단위로 커밋한다.

## 2026-06-28 DB 연결 설정

- 사용자가 이번 작업은 이슈 번호 없이 진행하라고 명시해 AGENTS.md의 이슈 번호 요구는 예외로 처리한다.
- repo에는 SSM Parameter Store를 직접 조회하는 코드나 의존성이 없다.
- 기존 `application.yml`은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` env var를 읽지만 로컬 기본값을 포함하고 있었다.
- SSM 값은 애플리케이션에서 직접 읽지 않고 배포/IaC 단계에서 env var로 주입하는 방향을 유지한다.
- `DB_URL`에는 이미 `sslmode=require`와 `prepareThreshold=0`이 포함된 값을 주입받는 전제이므로 애플리케이션에서 JDBC URL을 재조립하지 않는다.
- secret 값은 repo, 로그, 테스트 출력, 문서에 남기지 않는다.
- `application-local.yml`, `application-develop.yml`, `application-prod.yml`은 모두 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` placeholder만 읽도록 분리한다.
- `.env.example`은 실제 로딩 파일이 아니라 로컬 환경변수 설정을 위한 secret 없는 예시로 둔다.
- `./gradlew test`로 profile 설정 placeholder 검증과 기존 컨텍스트 부팅을 확인했다.
