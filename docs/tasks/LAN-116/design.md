# LAN-116 앱 버전 업데이트 확인 API 설계

## 목표

클라이언트의 플랫폼과 현재 빌드 번호를 받아 활성 앱 버전 정책과 비교하고 `FORCE`, `SOFT`, `NONE` 중 하나를 반환한다. 조회 API만 구현하며, 정책 등록·수정 API와 초기 정책 데이터 삽입은 이번 범위에서 제외한다.

## 확정 범위

- `GET /api/v1/app-versions/check`를 인증 없이 제공한다.
- 요청값은 `platform=IOS|ANDROID`, `buildNumber>=1`로 제한한다.
- 플랫폼별 활성 정책 한 건을 조회한다.
- 최신 빌드와 최소 지원 빌드를 기준으로 업데이트 유형과 사유를 계산한다.
- 활성 정책이 없으면 `500 APP_VERSION_POLICY_NOT_CONFIGURED`를 반환한다.
- 기존 `ApiResponse`와 OpenAPI 문서 형식을 따른다.
- 기존 `V4__apply_dbml_schema.sql`은 수정하지 않는다.

## 제외 범위

- 버전 정책 등록·수정·활성 전환 관리자 API.
- FE 값이 확정되지 않은 iOS·Android 초기 정책 데이터.
- 테스트 DB인 H2에서 PostgreSQL 부분 유니크 인덱스와 동일한 제약을 별도 구조로 재현하는 작업.

초기 정책 데이터는 FE 값이 확정된 후 당시의 다음 Flyway 번호로 별도 마이그레이션을 추가한다. 그전에는 정책이 없는 플랫폼 요청이 의도대로 `APP_VERSION_POLICY_NOT_CONFIGURED`를 반환한다.

## 애플리케이션 구조

```text
GET /api/v1/app-versions/check
        ↓
AppVersionController
        ↓
AppVersionQueryService
        ↓
AppVersionRepository
        ↓
app_version 활성 정책 한 건
```

- Controller는 쿼리 파라미터 검증, Query Service 호출, 공통 응답 변환을 담당한다.
- Query Service는 활성 정책 누락 처리와 업데이트 정책 계산을 담당한다.
- Repository는 기존 `AppVersion` 엔티티를 사용해 플랫폼별 활성 정책을 조회한다.
- 단순 조회 흐름이므로 별도 UseCase, Port, Adapter 인터페이스는 추가하지 않는다.

## 요청 검증과 공개 접근

- `platform`은 기존 `AppPlatform` enum으로 바인딩한다.
- `buildNumber`는 Bean Validation의 `@Min(1)`로 검증한다.
- 필수 파라미터 누락, 잘못된 enum 값, 1 미만 빌드 번호는 모두 `400 VALIDATION_FAILED`로 변환한다.
- 잘못된 enum 변환은 공통 예외 처리기에 타입 불일치 예외를 추가해 처리한다.
- 보안 설정에 해당 GET 경로를 공개 경로로 명시하고, 인증 헤더가 없는 통합 테스트로 확인한다.

## 업데이트 정책

| 조건 | updateType | reason |
| --- | --- | --- |
| `buildNumber < minimumSupportedBuildNumber` | `FORCE` | `forceUpdateReason` |
| `minimumSupportedBuildNumber <= buildNumber < latestBuildNumber` | `SOFT` | `softUpdateReason` |
| `buildNumber >= latestBuildNumber` | `NONE` | `null` |

응답에는 활성 정책의 `versionName`, `buildNumber`, `minimumSupportedBuildNumber`, `releasedAt`을 각각 `latestVersionName`, `latestBuildNumber`, `minimumSupportedBuildNumber`, `releasedAt`으로 반환한다.

## 오류 처리

- 활성 정책이 없으면 `ApiException`과 새 오류 코드 `APP_VERSION_POLICY_NOT_CONFIGURED`를 사용한다.
- 오류 코드는 HTTP 500과 `앱 버전 정책이 올바르게 설정되지 않았습니다.` 메시지를 갖는다.
- 기존 공통 예외 처리기는 5xx `ApiException`을 Sentry에 기록한다.
- PostgreSQL의 유니크 인덱스가 플랫폼별 활성 정책 중복을 차단하므로 임의로 첫 번째 정책을 선택하지 않는다.

## 데이터베이스 제약

현재 기반 브랜치에 V14와 V15가 있으므로 다음 번호를 사용한다.

1. 공통 V16 마이그레이션에서 기존 `chk_app_version_build`를 교체한다.
   - `build_number >= 1`.
   - `minimum_supported_build_number >= 0`.
   - `minimum_supported_build_number <= build_number`.
2. PostgreSQL 전용 V17 마이그레이션에서 `platform`에 `WHERE active = true` 조건을 둔 부분 유니크 인덱스를 추가한다.

V16은 H2와 PostgreSQL에 공통 적용한다. V17은 운영 DB인 PostgreSQL에서만 적용한다. H2에 생성 컬럼이나 트리거를 추가해 PostgreSQL 제약을 흉내 내지 않는다.

## 테스트와 검증

- API 통합 테스트에서 iOS와 Android 정책을 독립적으로 조회한다.
- 최소 지원 빌드 미만, 최소 지원 빌드 이상이면서 최신 빌드 미만, 최신 빌드 이상의 경계를 검증한다.
- 각 업데이트 유형의 응답 필드와 `reason`을 검증한다.
- 잘못된 플랫폼, 필수값 누락, 1 미만 빌드 번호의 `400 VALIDATION_FAILED`를 검증한다.
- 활성 정책이 없는 플랫폼의 `500 APP_VERSION_POLICY_NOT_CONFIGURED`를 검증한다.
- 인증 헤더 없이 호출해 공개 접근을 검증한다.
- H2 스키마 테스트에서 V16 적용과 숫자 제약 위반 거부를 검증한다.
- PostgreSQL 연결 환경이 있으면 `migrateDatabase`로 V16과 V17을 실제 적용한다. 환경이 없으면 미검증 사유를 결과에 남긴다.
- 전체 `./gradlew test`와 `git diff --check feat/LAN-94...HEAD`를 실행한다.

## 완료 조건 조정

원 명세의 초기 iOS·Android 데이터 삽입 항목은 FE 값 미확정으로 이번 완료 조건에서 제외한다. API, 오류 계약, OpenAPI, DB 제약, H2 마이그레이션, 전체 테스트가 통과하면 LAN-116의 구현 범위를 완료한 것으로 본다. PostgreSQL 실제 적용은 연결 가능한 검증 환경 유무를 결과에 명시한다.
