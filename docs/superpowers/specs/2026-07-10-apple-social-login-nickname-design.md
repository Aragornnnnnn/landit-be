# Apple 소셜 로그인 nickname 처리 설계

## 목표

Apple 소셜 로그인에서 ID Token에 사용자 이름이 없을 때, 클라이언트가 전달한 nullable `nickname`으로 사용자 프로필을 생성하거나 갱신한다.

## 변경 범위

- `SocialLoginRequest`에 nullable `nickname`을 추가한다.
- `AuthService`는 Apple 요청의 비어 있지 않은 `nickname`을 OIDC 검증 결과의 이름보다 우선 사용한다.
- Apple 요청에 `nickname`이 없으면 기존 OIDC 검증 결과를 사용한다.
- Apple 외 제공자의 이름 처리와 API 동작은 변경하지 않는다.

## 데이터 흐름

1. 클라이언트가 `provider`, `idToken`, `nonce`, 선택적 `nickname`을 보낸다.
2. 서버가 기존 방식으로 ID Token을 검증한다.
3. 제공자가 Apple이고 요청 `nickname`이 비어 있지 않으면 해당 값을 사용자 프로필 이름으로 사용한다.
4. 신규 사용자 생성과 기존 사용자 프로필 갱신은 확정된 이름을 사용한다.

## 검증

- Apple 요청의 `nickname`이 ID Token 이름보다 우선 적용되는지 통합 테스트로 검증한다.
- Apple 요청에서 `nickname`이 없을 때 기존 동작을 유지하는지 검증한다.
- Google 로그인은 요청 `nickname`과 무관하게 ID Token 이름을 사용하는지 회귀 테스트로 검증한다.
