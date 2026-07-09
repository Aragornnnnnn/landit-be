# 초기 BE 설정 체크리스트

- [x] 저장소 상태 확인.
- [x] Spring Boot 4 지원 버전과 dependency id 확인.
- [x] Gradle 기반 Spring Boot 프로젝트 생성.
- [x] 요청된 BE 라이브러리 의존성 반영.
- [x] Java 21 설정 반영.
- [x] Scheduler 활성화.
- [x] PostgreSQL 기본 설정과 H2 테스트 설정 분리.
- [x] Flyway 초기 마이그레이션 추가.
- [x] 애플리케이션 컨텍스트 테스트 구성.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## DB 연결 설정

- [x] 기존 DB/profile/시크릿 설정 방식 조사.
- [x] SSM 직접 조회 패턴 부재 확인.
- [x] 환경변수 기반 DB 설정 유지 계획 수립.
- [x] local/develop/prod profile 설정 분리.
- [x] 시크릿 없는 `.env.example` 추가.
- [x] 설정 검증 테스트 추가.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## 시간대 설정

- [x] 기존 timezone 설정 부재 확인.
- [x] JVM 기본 timezone을 `Asia/Seoul`로 설정.
- [x] Jackson timezone을 `Asia/Seoul`로 설정.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## LAN-43 dev 배포 워크플로우

- [x] 저장소 구조, build tool, Dockerfile, 기존 워크플로우, git 상태 확인.
- [x] 기존 Dockerfile과 워크플로우 부재 확인.
- [x] 최소 Dockerfile 추가.
- [x] dev 배포용 GitHub Actions 워크플로우 추가.
- [x] dev 배포 워크플로우를 수동 실행으로 제한.
- [x] dev 배포 워크플로우를 `develop` GitHub Environment 변수 기반으로 변경.
- [x] 워크플로우 YAML 문법 확인.
- [x] `./gradlew test` 실행.
- [x] `git diff`와 `git status --short` 확인.
- [x] 논리 단위 커밋 생성.

## LAN-43 prod 배포 워크플로우

- [x] 현재 AWS ECS/ECR 리소스 이름 확인.
- [x] prod 배포용 GitHub Actions 워크플로우 추가.
- [x] prod 배포 워크플로우를 수동 실행으로 제한.
- [x] prod 배포 워크플로우를 `main` 브랜치에서만 진행하도록 제한.
- [x] prod 배포 워크플로우를 `prod` GitHub Environment 변수 기반으로 변경.
- [x] 워크플로우 YAML 문법 확인.
- [x] `./gradlew test` 실행.
- [x] `git diff`와 `git status --short` 확인.
- [x] 논리 단위 커밋 생성.

## LAN-66 Agent 개발용 아키텍처 문서화

- [x] `feat/LAN-66` 브랜치에서 작업.
- [x] 기존 문서 구조와 아키텍처 초안 확인.
- [x] `AGENTS.md`에 에이전트가 따라야 할 아키텍처 규칙 추가.
- [x] 상세 백엔드 아키텍처 문서 추가.
- [x] `README.md`에서 상세 문서로 연결.
- [x] 문서 링크와 diff 검토.
- [x] 논리 단위 커밋 생성.

## LAN-66 문서 점검 후속 수정

- [x] `HEALTH_CHECK_URL` 변수명 불일치 수정.
- [x] Worker 구현과 배포 소유 경계 명시.
- [x] `README.md` 문서 링크 보강.
- [x] 완료된 실행 계획 문서의 스냅샷 성격 명시.
- [x] 문서 링크, YAML 문법, diff 검증.
- [x] 논리 단위 커밋 생성.

## LAN-58 공통 응답과 예외 처리 체계

- [x] `feat/LAN-58` 브랜치와 작업트리 상태 확인.
- [x] SayNow BE의 공통 응답/예외 처리 구현 확인.
- [x] Landit BE의 현재 패키지와 테스트 구조 확인.
- [x] 공통 응답 테스트를 먼저 추가하고 실패 확인.
- [x] 공통 예외 처리 테스트를 먼저 추가하고 실패 확인.
- [x] Landit 패키지에 공통 응답 객체 추가.
- [x] Landit 패키지에 공통 예외 객체와 핸들러 추가.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## LAN-58 Sentry 예외 전송 정책

- [x] 현재 브랜치와 작업트리 상태 확인.
- [x] 기존 Landit Sentry 설정과 예외 핸들러 확인.
- [x] Sentry reporter 전송 정책 테스트를 먼저 추가하고 실패 확인.
- [x] Sentry reporter 경계와 기본 구현 추가.
- [x] `GlobalExceptionHandler`가 5xx만 Sentry에 전송하도록 변경.
- [x] 사용하지 않는 `SENTRY_TRACES_SAMPLE_RATE` 설정 제거.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## LAN-55 OIDC 소셜 로그인

- [x] `feat/LAN-55` 브랜치와 작업트리 상태 확인.
- [x] SayNow BE 소셜 로그인, OIDC, nonce 검증 구현 확인.
- [x] Landit BE의 현재 인증/DB/설정 구조 확인.
- [x] 소셜 로그인 통합 테스트를 먼저 추가하고 실패 확인.
- [x] nonce 누락과 불일치 거부 테스트를 먼저 추가하고 실패 확인.
- [x] 사용자와 refresh token 테이블 마이그레이션 추가.
- [x] 인증 도메인, DTO, 저장소 추가.
- [x] OIDC verifier와 nonce 검증 추가.
- [x] access token과 refresh token 발급 추가.
- [x] 소셜 로그인 API 추가.
- [x] SecurityFilterChain 설정 추가.
- [x] 인증 환경변수 예시와 profile 설정 추가.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.
- [x] Apple audience 환경변수와 provider 지원 범위 반영.
- [x] Apple 소셜 로그인 통합 테스트를 먼저 추가하고 실패 확인.
- [x] Apple OIDC issuer, JWKS, audience 설정 추가.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.
- [x] `RemoteOidcTokenVerifier`를 SayNow의 provider settings 방식으로 정리.
- [x] 리팩터링 후 소셜 로그인 통합 테스트 실행.
- [x] 리팩터링 후 `./gradlew test` 실행.
- [x] 코드 리뷰 스킬로 변경 diff 검토.
- [x] 논리 단위 커밋 생성.

## LAN-55 DBML Entity와 Flyway 전환

- [x] 수정된 DBML 기준 구현 계획 작성.
- [x] DBML 스키마 회귀 테스트를 먼저 추가하고 실패 확인.
- [x] 기존 `users/refresh_tokens` 구조를 `user_profile/oauth_identity/refresh_token`으로 전환하는 Flyway 마이그레이션 추가.
- [x] 인증 도메인 Entity와 Repository를 DBML 구조로 리팩터링.
- [x] 나머지 DBML 테이블 Entity와 enum 추가.
- [x] JSONB 컬럼을 `JsonNode`와 Hibernate JSON 타입으로 매핑.
- [x] DB check constraint와 partial unique index 반영.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 의미 단위 커밋 생성.
- [x] refresh token 회전 통합 테스트를 먼저 추가하고 실패 확인.
- [x] logout refresh token 폐기 통합 테스트를 먼저 추가하고 실패 확인.
- [x] withdraw와 access token 인증 필터 통합 테스트를 먼저 추가하고 실패 확인.
- [x] SayNow 방식의 refresh/logout/withdraw 서비스 흐름 추가.
- [x] access token 파싱과 Spring Security filter/principal 추가.
- [x] 탈퇴 사용자 soft delete와 refresh token 일괄 폐기 반영.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 코드 리뷰 스킬로 변경 diff 검토.
- [x] 논리 단위 커밋 생성.

## LAN-66 NPS 응답 Entity 수정

- [x] `feat/LAN-66` 브랜치에서 작업.
- [x] 최신 `origin/main` 코드 반영.
- [x] NPS 응답 스키마 회귀 테스트를 먼저 추가하고 실패 확인.
- [x] `session_nps_response`를 세션 비종속 `nps_response`로 변경.
- [x] 중복 제출을 허용하도록 unique 제약 제거.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.
- [x] 이미 적용된 `V4__apply_dbml_schema.sql` 원복.
- [x] NPS 테이블 교체를 새 `V6` migration으로 분리.
- [x] Flyway checksum 재발 방지 테스트 추가.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## 2026-07-07 ECS 배포 검증 fail-fast 개선

- [x] `origin/develop` 기준 워크플로우 확인.
- [x] develop/prod `Verify ECS service` 단계에 bounded wait와 ECS 이벤트 출력 추가.
- [x] YAML 문법과 diff 검증.
- [x] 논리 단위 커밋 생성.

## 2026-07-07 ERD 기준 Entity 동기화

- [x] 사용자 지시에 따라 이슈 번호 없이 `origin/develop` 기준 브랜치 생성.
- [x] ERD와 현재 Entity, Flyway migration 차이 재확인.
- [x] ERD 기준 컬럼 회귀 테스트를 먼저 추가하고 실패 확인.
- [x] 기존 적용 migration은 유지하고 새 migration으로 schema 변경.
- [x] `scenario`, `writing_expression`, `user_writing_expression_completion` Entity 수정.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## 2026-07-07 Swagger 한글 설명 인코딩 수정

- [x] live `/v3/api-docs` 한글 설명 깨짐 재현.
- [x] `@Schema` 한글 설명 원문과 build 설정 확인.
- [x] JavaCompile 인코딩을 UTF-8로 고정.
- [x] `./gradlew test` 실행.
- [x] origin/develop push.
- [x] live `/v3/api-docs` 재확인.

## 2026-07-07 Scenario completion_criteria 제거

- [x] `origin/develop` 최신 상태 반영.
- [x] `completion_criteria` 사용처 확인.
- [x] schema 회귀 테스트를 먼저 추가하고 실패 확인.
- [x] `Scenario` Entity에서 `completionCriteria` 제거.
- [x] 새 Flyway migration으로 `scenario.completion_criteria` 제거.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## 2026-07-07 OpenAPI JSON charset 명시

- [x] 브라우저에서 `/v3/api-docs` 원문 JSON 한글 깨짐 재현.
- [x] live 응답 바이트가 UTF-8이고 `Content-Type`에 charset이 없음을 확인.
- [x] OpenAPI docs charset 회귀 테스트를 먼저 추가하고 실패 확인.
- [x] `/v3/api-docs` 응답에 UTF-8 charset 명시.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.
- [ ] origin/develop push.

## 2026-07-07 CONTRIBUTING 분리

- [x] 사용자가 이슈 번호 없이 `origin/develop` 직접 작업을 요청한 예외 확인.
- [x] 현재 `AGENTS.md`, `README.md`, 기존 협업 규칙 위치 확인.
- [x] 사람용 협업 규칙을 `CONTRIBUTING.md`로 분리.
- [x] `AGENTS.md`를 에이전트 실행 규칙 중심으로 정리.
- [x] `README.md` 문서 링크 갱신.
- [x] 문서 diff 검토.
- [x] 논리 단위 커밋 생성.

## 2026-07-07 V8 migration 배포 실패 수정

- [x] GitHub Actions job 로그와 ECS task 로그 확인.
- [x] `V8__drop_scenario_completion_criteria.sql`를 이미 컬럼이 없는 DB에서도 통과하도록 수정.
- [x] ECS 배포 검증이 failed task를 timeout 전에 실패 처리하도록 수정.
- [x] 관련 테스트와 YAML 검증 실행.
- [x] 논리 단위 커밋 생성.

## 2026-07-07 Flyway와 ECS 배포 분리

- [x] 현재 Flyway 실행 경로와 develop/prod profile 확인.
- [x] 별도 Flyway migration 실행 task 추가.
- [x] deploy workflow에서 ECS 배포 전 migration 실행.
- [x] develop/prod 앱 런타임 Flyway 비활성화.
- [x] 관련 테스트와 YAML 검증 실행.
- [x] 논리 단위 커밋 생성.

## 2026-07-07 LAN-59 PR 충돌 정리

- [x] `feat/LAN-59`를 `origin/develop` 기준으로 rebase.
- [x] 중복 Flyway workflow 커밋 제거.
- [x] 표현 조회 API diff만 남았는지 확인.
- [x] 관련 검증 실행.
- [x] PR 브랜치 push 전 상태 확인.

## 2026-07-07 LAN-79 PR 충돌 정리

- [x] `feat/LAN-79`를 현재 `origin/feat/LAN-59` 기준으로 rebase.
- [x] LAN-79 전용 diff만 남았는지 확인.
- [x] 배포 workflow 파일 diff가 제거됐는지 확인.
- [x] 관련 검증 실행.
- [x] PR 브랜치 push 전 상태 확인.
