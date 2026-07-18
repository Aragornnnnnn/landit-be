# LAN-176 CodeRabbit 리뷰 설계

## 목표

`landit-be`, `landit-ai`, `landit-fe`의 PR을 저장소별 책임에 맞게 한국어로 자동 리뷰한다. 포맷이나 취향보다 실제 결함, 서비스 간 계약 위반, 보안 문제, 회귀 가능성을 우선한다.

## 공통 정책

- 자동 리뷰 대상 base 브랜치는 `develop`, `main`, `release/*`, `feat/*`다.
- draft PR은 자동 리뷰하지 않는다.
- 리뷰 프로필은 `chill`로 두고 변경 요청 자동 차단은 사용하지 않는다.
- 저장소의 `AGENTS.md`를 공통 코드 지침으로 사용하고, `.coderabbit.yaml`에는 CI가 잡기 어려운 의미 규칙만 둔다.
- 세 저장소는 공개 저장소이므로 CodeRabbit Open Source 무료 플랜을 전제로 한다.

## 저장소별 리뷰 초점

### landit-be

- API 인증·인가·소유권, 요청 검증, 오류와 HTTP 상태, 공개 계약과 OpenAPI 호환성.
- 트랜잭션 안의 외부 호출, 멱등성, 중복 요청과 동시성, 상태 전이 원자성.
- `landit-ai` 요청·응답 DTO, enum, nullability, 실패 매핑 호환성.
- Flyway 이력 불변성, JPA 스키마 정합성, PostgreSQL 운영 안전성.

### landit-ai

- `landit-be`와 Pydantic 요청·응답 계약, 오류와 HTTP 상태 호환성.
- LLM JSON 파싱·검증·복구 경계, 점수와 피드백의 일관성.
- 캐시·중복·동시성·멱등성, prompt·사용자 발화·API key 유출 방지.
- 실제 LLM 네트워크 호출 없이 실패 경계와 품질 회귀를 검증하는 테스트.

### landit-fe

- `landit-be` API 경로·메서드·DTO·nullability·오류·인증 갱신 계약.
- Next.js 서버·클라이언트 경계, 접근성, 로딩·오류 상태.
- WebView 탐색·메시지 검증, OAuth 흐름, 앱 식별자와 OTA 호환성.
- 웹과 모바일이 공유하는 bridge 메시지의 직렬화·역호환성.

## 저장소 연결

- `landit-fe`는 `landit-be`를 참조한다.
- `landit-be`는 `landit-fe`와 `landit-ai`를 참조한다.
- `landit-ai`는 `landit-be`를 참조한다.

FE가 AI를 직접 호출하지 않으므로 직접 연결하지 않는다. 이 구조로 실제 런타임 의존 방향만 반영하고 불필요한 리뷰 컨텍스트를 줄인다.

## 검증

- 세 YAML 파일을 파싱하고 공통 base 브랜치 정규식과 연결 저장소를 확인한다.
- 각 저장소의 diff와 `git diff --check`를 확인한다.
- BE는 `./gradlew check`, AI는 unittest, FE는 포맷·lint·typecheck·test·build를 실행한다.
