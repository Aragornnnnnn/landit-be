# LAN-116 앱 버전 업데이트 확인 API 구현 계획

## 목표

`GET /api/v1/app-versions/check`에서 플랫폼별 활성 정책을 조회하고 현재 빌드 번호에 따라 `FORCE`, `SOFT`, `NONE`을 반환한다.

## 확정 사항

- 인증 없이 호출하며 `platform=IOS|ANDROID`, `buildNumber>=1`만 허용한다.
- 활성 정책이 없으면 `500 APP_VERSION_POLICY_NOT_CONFIGURED`를 반환한다.
- 초기 iOS·Android 정책 데이터는 이번 작업에서 넣지 않는다.
- 기존 `V4__apply_dbml_schema.sql`은 수정하지 않는다.
- 새 의존성이나 별도 UseCase, Port, Adapter는 추가하지 않는다.

## 작업 순서

### 1. DB 제약

관련 파일은 `V16__enforce_app_version_build_constraints.sql`, `V17__enforce_single_active_app_version.sql`, `DatabaseSchemaIntegrationTests.java`다.

- [ ] 실패 테스트를 먼저 추가한다.
- [ ] 공통 V16에서 `build_number >= 1`, `minimum_supported_build_number >= 0`, `minimum_supported_build_number <= build_number`를 강제한다.
- [ ] PostgreSQL 전용 V17에서 `active = true`인 정책을 플랫폼별 한 건으로 제한한다.
- [ ] H2 제약 적용과 PostgreSQL 전용 SQL 내용을 검증한다.

### 2. 조회 API

새 파일은 `AppVersionController`, `AppVersionCheckResponse`, `AppVersionQueryService`, `AppVersionRepository`, `AppVersionApiIntegrationTests`다. 기존 `AppVersion`과 `ErrorCode`도 수정한다.

- [ ] API 통합 테스트를 먼저 작성해 404 실패를 확인한다.
- [ ] 플랫폼별 활성 정책을 조회한다.
- [ ] 현재 빌드가 최소 지원 빌드보다 낮으면 `FORCE`와 `forceUpdateReason`을 반환한다.
- [ ] 현재 빌드가 최신 빌드보다 낮으면 `SOFT`와 `softUpdateReason`을 반환한다.
- [ ] 나머지는 `NONE`과 `reason=null`을 반환한다.
- [ ] 정책이 없으면 `APP_VERSION_POLICY_NOT_CONFIGURED`를 발생시킨다.

### 3. 검증, 보안, OpenAPI

`AppVersionController`, `GlobalExceptionHandler`, `AuthSecurityConfig`, `AppVersionApiIntegrationTests`를 수정한다.

- [ ] Bean Validation으로 `buildNumber>=1`을 검증한다.
- [ ] 잘못된 플랫폼과 필수값 누락을 `400 VALIDATION_FAILED`로 처리한다.
- [ ] 보안 설정에 공개 GET 경로를 명시한다.
- [ ] 요청 파라미터와 200·400·500 응답을 OpenAPI에 문서화한다.
- [ ] iOS·Android 독립 조회, `FORCE`·`SOFT`·`NONE` 경계, 정책 누락, 무인증 호출, OpenAPI 노출을 통합 테스트로 검증한다.

### 4. 전체 검증

```bash
./gradlew test --tests com.landit.landitbe.DatabaseSchemaIntegrationTests
./gradlew test --tests com.landit.landitbe.app.AppVersionApiIntegrationTests
./gradlew test
git diff --check feat/LAN-94...HEAD
```

PostgreSQL 환경이 있으면 시크릿을 출력하지 않고 `./gradlew migrateDatabase`로 V16과 V17을 실제 적용한다. 환경이 없으면 미검증 사유를 최종 결과에 명시한다.

## 커밋 단위

1. `feat: 앱 버전 정책 DB 제약 추가`.
2. `feat: 앱 버전 업데이트 확인 API 추가`.
3. `test: 앱 버전 확인 API 계약 검증`.

## 후속 작업

FE가 iOS와 Android 초기 정책 값을 확정하면 당시의 다음 Flyway 번호로 데이터 삽입 마이그레이션을 추가한다.
