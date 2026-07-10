# LAN-112 작업 메모

- 소셜 로그인 요청의 사용자 이름 필드는 도메인과 응답 DTO의 용어에 맞춰 `nickname`으로 정했다.
- Apple은 ID Token에 사용자 이름을 항상 제공하지 않으므로, `provider=APPLE`이고 요청 `nickname`이 있으면 그 값을 우선 사용한다.
- 요청 `nickname`은 nullable이며, Apple 외 제공자의 현재 ID Token 기반 이름 처리에는 영향을 주지 않는다.
- `socialLoginUsesRequestNicknameForApple` 테스트가 구현 전 `Id Token Name` 응답으로 실패하는 것을 확인한 후 구현했다.
- Apple nickname 누락 처리는 `socialLoginCreatesGuestForAppleWithoutRequestNickname`와 `socialLoginKeepsExistingAppleNicknameWhenRequestNicknameIsMissing`가, Google 무시는 `socialLoginIgnoresRequestNicknameForGoogle`가 검증한다.
- 신규 Apple 사용자의 요청 `nickname`이 null 또는 blank이면 `Guest`를 저장한다.
- 기존 Apple 사용자의 요청 `nickname`이 null 또는 blank이면 nickname을 갱신하지 않는다.
