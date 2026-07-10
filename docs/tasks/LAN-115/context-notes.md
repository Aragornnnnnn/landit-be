# LAN-115 작업 메모

- Spring Boot 4의 `spring-boot-starter-webmvc`는 Jackson 3의 `tools.jackson` 계열을 자동 구성한다.
- `RemoteAiConversationClient`는 Jackson 2의 `com.fasterxml.jackson.databind.ObjectMapper`를 주입받으므로, 원격 AI 모드에서 해당 타입 Bean이 없어 ApplicationContext 기동이 실패한다.
- ECS 배포 검증은 PRIMARY deployment의 `failedTasks`가 숫자가 아닌 경우 0으로 정규화하고, 실패 태스크가 확인되면 즉시 실패해야 한다.
- `LanditBeApplicationTests`를 `landit.ai.client-mode=remote`로 기동해 수정 전 `NoSuchBeanDefinitionException`을 재현했고, `JacksonConfiguration`의 Jackson 2 Bean 등록 후 통과를 확인했다.
- develop 워크플로우는 `failedTasks`가 1 이상이면 replacement 태스크 상태를 기다리지 않고, 서비스 상태와 최근 ECS 이벤트를 출력한 뒤 즉시 실패한다.
- `ScenarioSessionMessageContextRow`의 마지막 두 필드는 `String`이었지만, JPQL이 전달하는 `ScenarioLanguageVariant.targetLocale`, `baseLocale`의 실제 Java 타입은 `Locale` enum이다. DTO 생성자 파라미터를 `Locale`로 맞췄다.
- `ScenarioSessionMessageQueryRepositoryIntegrationTests`는 데이터가 없는 조회도 실행해 JPQL constructor expression을 검증한다. 수정 전 `Missing constructor for type 'ScenarioSessionMessageContextRow'`로 실패했고 수정 후 통과했다.
- `origin/develop` rebase 후 전체 테스트의 기존 `ScenarioSessionApiIntegrationTests` 5건이 fake AI 응답 및 인증 기대값 불일치로 실패한다. 이번 DTO·회귀 테스트 변경 파일과 겹치지 않는다.
- develop 배포는 Docker 빌드 전에 `./gradlew test --no-daemon`을 실행한다. ECS 검증은 `update-service`가 반환한 PRIMARY deployment ID와 생성 시각만 추적하며 10초 간격, 최대 5분으로 제한한다.
- Bash mock 검증은 정상 안정화, nullable `failedTasks`, deployment 실패 태스크, essential container 종료, non-zero exit code, 이전 deployment 태스크 무시, 5분 타임아웃을 다룬다.
