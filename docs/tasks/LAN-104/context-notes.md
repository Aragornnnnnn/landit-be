# LAN-104 컨텍스트 노트

## 2026-07-08 CORS 설정

- 사용자가 Notion 이슈 번호 `LAN-104`에 해당하는 작업 브랜치 생성을 요청해 `origin/develop` 기준 `feat/LAN-104`에서 작업한다.
- 현재 `AuthSecurityConfig`에는 `cors(...)` 설정이 없고, `DELETE /api/v1/auth/me`처럼 인증이 필요한 API가 있다.
- 기존 런타임 설정은 SSM을 애플리케이션에서 직접 조회하지 않고 환경변수로 주입받아 `application.yml`에서 읽는 방식이다.
- CORS 허용 origin은 SSM `/landit/{environment}/LANDIT_CORS_ALLOWED_ORIGINS`에 있으며, 애플리케이션에서는 `LANDIT_CORS_ALLOWED_ORIGINS` 환경변수로 읽는다.
- method, header, credential 정책은 환경별로 바꿀 필요가 낮으므로 SSM이 아니라 프로젝트 코드 기본값으로 관리한다.
- `CorsConfigurationIntegrationTests.preflightForAuthenticatedApiUsesConfiguredCorsPolicy`는 구현 전 `Access-Control-Allow-Origin` 헤더가 없어 실패했고, `AuthSecurityConfig` CORS 연결 후 통과했다.
- 리뷰 반영으로 `OPTIONS /** permitAll`은 제거한다. `http.cors(...)`가 활성화되어 있으면 유효한 preflight는 인증 인가 규칙 전에 CORS 필터에서 처리된다.
- CORS 설정 중 SSM/env로 관리하는 값은 `LANDIT_CORS_ALLOWED_ORIGINS` 하나로 제한한다.
- CORS 기본 method는 `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`이고, 기본 header는 `Authorization`, `Content-Type`, `Accept`, `Origin`이며, credential은 `true`로 둔다.
- method/header 외부 설정이 비어 있으면 기존 구현은 `Invalid CORS request`로 403을 반환했고, 코드 기본값을 적용한 뒤 preflight 테스트가 통과했다.
- 최종 검증으로 `git diff --check`와 `./gradlew test`를 실행했고 둘 다 통과했다.
- 리뷰 반영 후에도 `git diff --check`와 `./gradlew test`를 다시 실행했고 둘 다 통과했다.

## 2026-07-08 코드 구조 점검

- 작업별 기록은 앞으로 `docs/tasks/{ISSUE_NUMBER}/` 아래에 둔다.
- 루트 `checklist.md`와 `context-notes.md`에 있던 LAN-104 기록은 `docs/tasks/LAN-104/`로 옮긴다.
- Ponytail 기준으로 확인한 과설계는 `CorsProperties`가 origin 외 method/header/credential까지 외부 설정처럼 들고 있던 점이다.
- method/header/credential은 코드 기본 정책이므로 `CorsProperties`에서 제거하고 `AuthSecurityConfig` 상수로 관리한다.
- 구조 점검 후 `git diff --check`, `./gradlew test --tests 'com.landit.landitbe.auth.CorsConfigurationIntegrationTests'`, `./gradlew test`를 실행했고 모두 통과했다.
