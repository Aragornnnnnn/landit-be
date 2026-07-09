# LAN-104 CORS 설정 체크리스트

## 구현

- [x] `origin/develop` 기준 `feat/LAN-104` 브랜치와 작업트리 상태 확인.
- [x] 현재 SecurityFilterChain과 환경변수 설정 방식 확인.
- [x] 인증 필요 API preflight CORS 테스트를 먼저 추가하고 실패 확인.
- [x] CORS origin 설정 프로퍼티와 환경변수 바인딩 추가.
- [x] Spring Security에 CORS 설정 적용.
- [x] 로컬 환경변수 예시 갱신.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## 리뷰 반영

- [x] `OPTIONS /** permitAll` 필요성 재검토.
- [x] Spring Security CORS 필터가 preflight를 처리하는지 테스트로 확인.
- [x] method, header, credential 정책을 코드 기본값으로 이동.
- [x] SSM/env 관리 대상을 `LANDIT_CORS_ALLOWED_ORIGINS`로 축소.
- [x] `./gradlew test` 실행.
- [x] 기존 CORS 커밋에 리뷰 반영분 포함.

## 코드 구조 점검

- [x] Ponytail 기준으로 과설계 가능성 확인.
- [x] CORS 설정 구조를 origin 외부 설정과 코드 기본 정책으로 단순화.
- [x] 작업 기록 위치를 `docs/tasks/LAN-104/`로 이동.
- [x] `AGENTS.md` 작업 기록 규칙 갱신.
- [x] 구조 점검 후 `git diff --check` 실행.
- [x] 구조 점검 후 `./gradlew test` 실행.
- [x] 기존 CORS 커밋에 구조 점검 반영분 포함.
