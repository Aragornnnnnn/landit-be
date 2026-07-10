# LAN-115 ECS 기동 및 배포 검증 설계

## 목표

원격 AI 클라이언트가 활성화된 Spring ApplicationContext를 정상 기동하고, develop ECS 배포 검증이 새 태스크의 기동 실패를 즉시 보고하도록 한다.

## 변경 범위

- Jackson 2 `ObjectMapper` Bean을 명시적으로 등록해 `RemoteAiConversationClient` 생성자 주입을 만족한다.
- `landit.ai.client-mode=remote` 기동 테스트가 해당 Bean과 원격 클라이언트 생성을 검증한다.
- `deploy-dev.yml`은 `failedTasks`가 `None`, `null`, 빈 값 또는 비숫자일 때 0으로 처리한다.
- PRIMARY deployment가 실패했거나 실패 태스크만 남은 경우 서비스 상태와 최근 ECS 이벤트를 출력하고 즉시 실패한다.

## 검증

- 원격 AI 모드 `ApplicationContext` 테스트가 수정 전에는 `ObjectMapper` Bean 누락으로 실패하고 수정 후 통과한다.
- 전체 Gradle 테스트를 실행한다.
- 워크플로우 diff로 `failedTasks` 정규화와 즉시 실패 경로가 상태·이벤트 출력을 포함하는지 확인한다.
