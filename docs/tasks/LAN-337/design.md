# LAN-337 관리자 인증·응답 계약 보완 설계

## 목표

관리자 화면이 로그인 응답만으로 사용자 권한을 판정하고, 관리자 API의 OpenAPI 생성 타입이 실제 응답 계약과 일치하도록 수정한다. 앱 버전 정책에는 마지막 수정 시각과 수정자 표시 정보를 추가한다.

## 범위

- `POST /api/v1/auth/social-login`의 `data.user`에 `role`, `status`를 추가한다.
- `GET /api/v1/auth/me`는 추가하지 않는다.
- `AdminUserListResponse.Item`을 고유한 `AdminUserListItem` 스키마로 분리한다.
- 로그인 및 이번 관리자 화면에서 사용하는 응답 DTO의 required·nullable 계약을 명시한다.
- `app_version`에 마지막 수정 시각과 수정자 ID를 저장하고 관리자 응답에는 수정자 닉네임을 반환한다.
- 사용자 목록 컬럼 결정과 편지 `title`·`preview` 길이 제한은 범위에서 제외한다.

## API 계약

### 로그인 응답

기존 `data.user` 필드를 유지하고 다음 필드를 추가한다.

```json
{
  "userId": 1,
  "nickname": "관리자",
  "email": "admin@example.com",
  "provider": "GOOGLE",
  "newUser": false,
  "role": "ADMIN",
  "status": "ACTIVE"
}
```

`role`은 `USER` 또는 `ADMIN`, `status`는 `ACTIVE`, `WITHDRAWN`, `BANNED` enum 값이다. 로그인은 활성 프로필만 성공하므로 응답값을 하드코딩하지 않고 프로필에서 읽는다.

### 관리자 앱 버전 응답

기존 응답에 다음 필드를 추가한다.

```json
{
  "updatedAt": "2026-08-20T16:30:00",
  "updatedBy": "김준서"
}
```

`updatedAt`은 항상 존재한다. `updatedBy`는 과거 수정자 정보가 없을 수 있으므로 JSON 키는 유지하되 값은 `null`일 수 있다. DB에는 닉네임이 아니라 `user_profile.id`를 저장하고 응답 변환 시 현재 닉네임을 조회한다.

## 저장 설계

`app_version`에 다음 컬럼을 추가한다.

- `updated_at TIMESTAMP(6) NOT NULL`
- `updated_by_user_profile_id BIGINT NULL`
- `updated_by_user_profile_id`에서 `user_profile(id)`로 연결되는 외래 키

기존 데이터는 가장 최근 `APP_VERSION_UPDATED` 감사 로그가 있으면 해당 로그의 생성 시각과 관리자 ID를 사용한다. 감사 로그가 없으면 `updated_at`은 기존 `created_at`, `updated_by_user_profile_id`는 `NULL`로 백필한다. `released_at`은 수정 시각 백필에 사용하지 않는다.

앱 버전 Entity는 현재 수정 메서드에서 `updatedAt`과 수정자 ID를 함께 갱신해 PATCH 응답에도 새 시각을 즉시 반환한다. 기존 `admin_audit_log`는 변경 이력으로 계속 저장한다.

## OpenAPI 설계

- `AdminUserListItem`을 고유 스키마 이름으로 등록하고 사용자 목록 `items`가 해당 `$ref`를 가리키도록 한다.
- 로그인 응답과 관리자 사용자 목록·상세, 앱 버전 응답의 항상 직렬화되는 필드는 `required`로 명시한다.
- 값이 없을 수 있는 필드는 `required + nullable`로 명시해 생성 타입을 `field: T | null`로 만든다.
- 전역 springdoc 설정 변경과 범위 밖 응답 DTO의 일괄 개명은 하지 않는다.

## 검증 기준

- 신규 일반 사용자 로그인 응답에 `role=USER`, `status=ACTIVE`가 포함된다.
- 기존 관리자 재로그인 응답에 `role=ADMIN`, `status=ACTIVE`가 포함된다.
- `GET /api/v1/auth/me`는 존재하지 않고 기존 DELETE 탈퇴 API만 유지된다.
- 관리자 사용자 목록 OpenAPI가 공용 `Item`이 아닌 `AdminUserListItem`을 참조한다.
- 대상 응답의 required·nullable 배열이 기대한 계약과 일치한다.
- 앱 버전 PATCH 후 응답과 DB의 `updated_at`, 수정자 ID가 일치한다.
- 감사 로그가 있는 기존 행과 없는 기존 행의 백필 결과가 각각 검증된다.
- `./gradlew check`가 통과한다.
