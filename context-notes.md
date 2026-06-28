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
