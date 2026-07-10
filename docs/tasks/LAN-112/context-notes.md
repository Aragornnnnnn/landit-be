# LAN-112 작업 메모

- 소셜 로그인 요청의 사용자 이름 필드는 도메인과 응답 DTO의 용어에 맞춰 `nickname`으로 정했다.
- Apple은 ID Token에 사용자 이름을 항상 제공하지 않으므로, `provider=APPLE`이고 요청 `nickname`이 있으면 그 값을 우선 사용한다.
- 요청 `nickname`은 nullable이며, Apple 외 제공자의 현재 ID Token 기반 이름 처리에는 영향을 주지 않는다.
