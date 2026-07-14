# LAN-124 원격 AI 클라이언트 Jackson 3 전환

## 목표

원격 AI 클라이언트가 Spring Boot 4의 Jackson 3 `JsonMapper`를 주입받아 AI 서버 JSON 요청과 응답을 처리하도록 전환한다.

## 변경 범위

- `RemoteAiConversationClient`의 `ObjectMapper`와 `JsonNode`를 `tools.jackson.databind`의 Jackson 3 타입으로 전환한다.
- 생성자에서 Spring Boot가 자동 구성한 `JsonMapper`를 주입받는다.
- Jackson 2 전용 `JacksonConfiguration`을 제거한다.
- `@JsonIgnoreProperties`는 Jackson 3 호환 annotation인 기존 `com.fasterxml.jackson.annotation` import를 유지한다.
- 원격 AI 단위 테스트를 Jackson 3 `JsonMapper`로 실행하고, 알 수 없는 응답 필드를 무시하는 역직렬화 계약을 확인한다.

## 검증 기록

- 변경 전 `LanditBeApplicationTests`에서 Jackson 2 `ObjectMapper` Bean이 남아 실패하는 것을 확인했다.
- 변경 전 `RemoteAiConversationClientTest`에서 Jackson 3 `JsonMapper` 생성자가 없어 실패하는 것을 확인했다.
- 변경 후 원격 모드 ApplicationContext에서 `JsonMapper`와 `RemoteAiConversationClient`가 함께 기동되고 Jackson 2 `ObjectMapper` Bean이 없는 것을 확인했다.
- 원격 AI 메시지 피드백 요청의 JSON 직렬화와 알 수 없는 응답 필드를 포함한 JSON 역직렬화 계약을 확인했다.
- `./gradlew test --no-daemon`이 통과했다.
