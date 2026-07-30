# LAN-220 관리자 앱 버전 정책 관리 구현 계획

**Goal:** 기존 Landit 소셜 로그인 기반으로 관리자만 앱 버전 정책을 관리하고, 변경 이력을 감사 기록에 남긴다.

## 범위와 결정

- 관리자 API는 `/api/v1/admin/**`로 제한하고 `admin_account` 허용 목록에 없는 인증 사용자는 `403 FORBIDDEN`으로 차단한다.
- 관리자 계정 관리, `GET /api/v1/admin/me`, 감사 기록 조회, 앱 버전 정책 삭제는 구현하지 않는다.
- iOS와 Android 앱 버전 정책의 목록 조회, 등록, 수정, 플랫폼별 활성 정책 전환만 제공한다.
- 플랫폼별 활성 정책은 하나만 유지한다.
- 기존 `GET /api/v1/app-versions/check`는 현재 활성 정책을 계속 반환한다.
- 관리자 쓰기 작업은 관리자 ID, 작업 종류, 대상, 변경 전후 값, 작업 시각을 `admin_audit_log`에 기록한다.
- 인증 정보는 감사 기록, 응답, 로그에 기록하지 않는다.
- 푸시 발송, 사용자 검색, 날짜별 시나리오 일정 관리 기능은 이번 범위에서 제외한다.

## API

| Method | Path | 역할 |
| --- | --- | --- |
| `GET` | `/api/v1/admin/app-versions` | 앱 버전 정책 목록 조회 |
| `POST` | `/api/v1/admin/app-versions` | 앱 버전 정책 등록 |
| `PATCH` | `/api/v1/admin/app-versions/{appVersionId}` | 버전명, 빌드 번호, 최소 지원 빌드 번호, 업데이트 사유, 릴리스 노트, 출시 시각 수정 |
| `POST` | `/api/v1/admin/app-versions/{appVersionId}/activate` | 같은 플랫폼의 활성 정책을 대상 정책으로 전환 |

## 구현 순서와 검증

1. 관리자 허용 목록·감사 기록·관리자 인가 필터를 유지한다.
   - 검증. 일반 사용자는 관리자 API에서 `403 FORBIDDEN`을 받고, 관리자 쓰기 작업은 감사 기록에 저장된다.
2. 앱 버전 관리자 API와 활성 정책 전환을 유지한다.
   - 검증. iOS와 Android별 목록·등록·수정·활성 전환이 가능하고, 플랫폼별 활성 정책은 한 건이다.
3. 기존 공개 앱 버전 확인 API 호환성을 검증한다.
   - 검증. 활성 정책 변경 후 `GET /api/v1/app-versions/check` 응답이 변경된 정책을 반환한다.
4. 푸시·사용자 검색·시나리오 일정 코드를 제거하고 OpenAPI 범위를 확인한다.
   - 검증. 관리자 OpenAPI에는 앱 버전 경로만 남고 제외 기능의 관리자 경로는 없다.

## 검증 명령

```bash
./gradlew test \
  --tests '*AdminAuthorizationIntegrationTests' \
  --tests '*AdminAuditServiceIntegrationTests' \
  --tests '*AdminAppVersionApiIntegrationTests' \
  --tests 'com.landit.landitbe.feature.app.AppVersionApiIntegrationTests' \
  --tests 'com.landit.landitbe.DatabaseSchemaIntegrationTests'
./gradlew check
git diff --check
```

## 확인 결과

- [x] 관리자 권한과 일반 사용자 `403`을 확인했다.
- [x] 앱 버전 정책 목록·등록·수정·활성 전환을 확인했다.
- [x] 플랫폼별 활성 정책 한 건과 공개 확인 API 반영을 확인했다.
- [x] 관리자 쓰기 감사 기록과 민감 정보 미노출을 확인했다.
- [x] 제외 기능이 관리자 API와 변경분에 포함되지 않았는지 확인했다.

검증 결과. 대상 통합 테스트와 `./gradlew check`를 통과했고, `git diff --check`도 오류가 없었다.
