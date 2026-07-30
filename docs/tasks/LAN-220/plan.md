# LAN-220 관리자 앱 버전 정책 관리 구현 계획

**Goal:** 플랫폼별 단일 앱 버전 정책을 관리하고, 앱 버전명으로 업데이트 필요 수준을 판단한다.

## 범위와 결정

- 기존 Landit 소셜 로그인과 `admin_account` 허용 목록으로 `/api/v1/admin/**` 접근을 제한한다.
- iOS와 Android는 각각 `app_version` 레코드 한 건만 유지한다.
- 기존 `app_version` 테이블을 유지하며 `active`는 현재 정책 레코드에 `true`로 유지한다.
- 버전 정책 등록과 활성 정책 전환 API는 제공하지 않는다.
- 관리자는 플랫폼별 정책 목록을 조회하고, 플랫폼을 기준으로 정책을 수정한다.
- `GET /api/v1/app-versions/check`는 `platform`, `versionName`, `buildNumber`을 받는다.
- 업데이트 판단에는 `versionName`만 사용한다. `buildNumber`는 요청 계약에 유지하지만 이번 범위에서 관측 정보로 기록하지 않는다.
- 버전명은 `Major.Minor.Patch` 숫자 형식으로 검증하고, 숫자 세 항목을 순서대로 비교한다.
- 현재 버전이 최소 지원 버전 미만이면 `FORCE`, 최신 버전 미만이면 `SOFT`, 그 외에는 `NONE`을 반환한다.
- `minimum_supported_build_number`를 `minimum_supported_version_name`으로 교체한다.
- 관리자 수정 감사 기록에는 변경 전후 정책 값을 저장한다.
- 푸시 발송, 사용자 검색, 날짜별 시나리오 관리, 관리자 계정 관리, 감사 기록 조회, MDC·Sentry 추적은 이번 범위에서 제외한다.

## API

| Method | Path | 역할 |
| --- | --- | --- |
| `GET` | `/api/v1/admin/app-versions` | iOS·Android 단일 정책 목록 조회 |
| `PATCH` | `/api/v1/admin/app-versions/{platform}` | 플랫폼 정책의 버전명, 최소 지원 버전명, 빌드 번호, 안내 문구, 릴리스 노트, 출시 시각 수정 |
| `GET` | `/api/v1/app-versions/check?platform={platform}&versionName={versionName}&buildNumber={buildNumber}` | 현재 앱 버전 기준 업데이트 필요 수준 조회 |

## 구현 순서와 검증

1. 단일 플랫폼 정책을 위한 DB 마이그레이션을 추가한다.
   - 검증. 기존 비활성 레코드를 제거하고, 플랫폼 중복을 막으며, 기존 최소 지원 빌드에 대응하는 버전명을 보존한다. 대응 버전이 없으면 migration을 중단한다.
2. 버전명 검증과 비교를 구현하고 공개 확인 API 계약을 변경한다.
   - 검증. `1.0.0`, `1.1.0`, `1.3.0`, `1.3.1` 요청이 각각 정책에 맞는 `FORCE`, `SOFT`, `NONE`, `NONE`을 반환한다.
3. 관리자 API를 목록·플랫폼 수정만 남기고 감사 기록을 갱신한다.
   - 검증. 등록·활성화 경로가 OpenAPI에서 사라지고, 관리자 수정이 공개 확인 API에 즉시 반영된다.

## 검증 명령

```bash
./gradlew test \
  --tests 'com.landit.landitbe.feature.app.AppVersionApiIntegrationTests' \
  --tests 'com.landit.landitbe.feature.app.AdminAppVersionApiIntegrationTests' \
  --tests 'com.landit.landitbe.DatabaseSchemaIntegrationTests'
./gradlew check
git diff --check
```

## 확인 결과

- [x] V32 migration으로 비활성 이력을 제거하고, 기존 최소 지원 기준을 보존해 플랫폼별 단일 정책과 최소 지원 버전명으로 전환했다.
- [x] 공개 확인 API에서 버전명 기준 `FORCE`, `SOFT`, `NONE` 판단과 숫자 비교를 검증했다.
- [x] 관리자 API에서 목록 조회와 플랫폼별 수정만 노출되고, 수정 감사 기록이 남는 것을 검증했다.
- [x] `./gradlew check`와 `git diff --check`를 통과했다.
