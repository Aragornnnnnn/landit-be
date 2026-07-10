# LAN-115 작업 메모

- Spring Boot 4의 `spring-boot-starter-webmvc`는 Jackson 3의 `tools.jackson` 계열을 자동 구성한다.
- `RemoteAiConversationClient`는 Jackson 2의 `com.fasterxml.jackson.databind.ObjectMapper`를 주입받으므로, 원격 AI 모드에서 해당 타입 Bean이 없어 ApplicationContext 기동이 실패한다.
- ECS 배포 검증은 PRIMARY deployment의 `failedTasks`가 숫자가 아닌 경우 0으로 정규화하고, 실패 태스크가 확인되면 즉시 실패해야 한다.
- `LanditBeApplicationTests`를 `landit.ai.client-mode=remote`로 기동해 수정 전 `NoSuchBeanDefinitionException`을 재현했고, `JacksonConfiguration`의 Jackson 2 Bean 등록 후 통과를 확인했다.
- develop 워크플로우는 `failedTasks`가 1 이상이면 replacement 태스크 상태를 기다리지 않고, 서비스 상태와 최근 ECS 이벤트를 출력한 뒤 즉시 실패한다.
