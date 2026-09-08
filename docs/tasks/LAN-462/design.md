# LAN-462 어드민 푸시 캠페인 설계

## 목표

관리자가 제목, 내용, 딥 링크를 가진 캠페인을 만들고 본인 테스트 후 전체 또는 선택 사용자의 활성 Expo Push Token에 비동기로 발송한다. 캠페인 생성 후 원문과 대상 조건은 수정하지 않는다.

## API

- `POST /api/v1/admin/push-campaigns`: 캠페인 생성.
- `GET /api/v1/admin/push-campaigns`: 목록 조회.
- `GET /api/v1/admin/push-campaigns/{campaignId}`: 원문, 상태, 집계 조회.
- `GET /api/v1/admin/push-campaigns/{campaignId}/audience-preview`: 현재 예상 사용자·Token 수 조회.
- `POST /api/v1/admin/push-campaigns/{campaignId}/test`: 관리자 본인 테스트를 SQS에 발행.
- `POST /api/v1/admin/push-campaigns/{campaignId}/send`: 저장된 대상 조건의 발송을 SQS에 발행.

모든 API는 기존 `/api/v1/admin/**` 권한 검사를 사용한다. 생성·테스트·전체 발송은 기존 관리자 감사 로그에 기록한다.

## 입력 계약

### 어드민 사용자 선택용 목록

기존 `GET /api/v1/admin/users`에 선택 필터를 추가한다. 두 필터는 AND 조건이며, 생략하면 해당 조건을 제한하지 않는다. 필터 적용 후 가입일·ID 내림차순으로 기존 `page`, `size` 페이지를 조회한다.

- `active=true`: 활성 사용자(`ACTIVE`). `false`: 탈퇴·차단 사용자(`WITHDRAWN`, `BANNED`).
- `pushConsent=true`: 저장된 푸시 권한이 `GRANTED`. `false`: `DENIED` 또는 `NOT_DETERMINED`.
- 목록에 `userProfileId`, 기존 기본 정보와 `pushPermissionStatus`를 반환한다.
- 푸시 동의 필터는 서버 저장값이다. 실제 기기 권한이나 활성 Token 보유 여부를 의미하지 않는다.
- 예: `/api/v1/admin/users?active=true&pushConsent=true&page=0&size=20`.
- 응답의 `totalCount`, `totalPages`는 필터 적용 결과 기준이다. 결과가 없으면 둘 다 0이며, 범위 밖 페이지는 빈 목록과 실제 전체 수를 반환한다.
- API의 `page`는 0부터 시작한다. 화면은 `page + 1`로 표시하며 마지막 페이지 요청 값은 `totalPages - 1`이다. 기존 `hasNext`도 유지한다.

### 캠페인 입력

- 제목, 내용, 딥 링크는 필수이며 최대 길이는 각각 255자, 500자, 1,000자다.
- 실제 Expo 표시 payload는 UTF-8 기준 3,000바이트 이하여야 한다.
- 딥 링크는 `/`로 시작하는 앱 내부 경로 또는 사용자 정보가 없는 `https` URL만 허용한다.
- 캠페인 생성과 테스트는 `Idempotency-Key`를 사용한다.
- `audienceType`: `ALL`(생략 시 기본값) 또는 `SELECTED`.
- `ALL`은 `userProfileIds`를 생략하거나 빈 목록으로 보낸다. ID가 포함되면 오류다.
- `SELECTED`는 양수 사용자 ID 1~1,000개가 필수다. 중복 제거·정렬 후 저장하며 존재하지 않는 ID는 거부한다.
- 같은 생성 요청 키에서 대상 유형이나 사용자 집합이 달라지면 충돌이다. 순서·중복만 달라진 목록은 같은 요청으로 처리한다.
- 선택 사용자가 비활성이거나 활성 Token이 없으면 실제 대상에 포함하지 않는다. 대상 수는 입력 ID 수가 아닌 발송 가능한 사용자·Token 수다.
- 생성·목록·상세 응답에 `audienceType`, 정규화된 `userProfileIds`를 반환한다. 본인 테스트는 이 목록과 관계없이 인증 관리자에게 발송한다.

```json
{"title":"공지","body":"내용","deepLink":"/home","audienceType":"SELECTED","userProfileIds":[123,456]}
```

## 발송 흐름

1. 발송 요청 시 저장된 ALL/SELECTED 조건에 맞는 활성 사용자와 Token ID를 캠페인 대상 테이블에 고정한다. 예상 대상 조회도 같은 SQL 조건을 사용한다.
2. SQS에는 캠페인 ID만 발행한다.
3. 소비자는 고정된 대상을 최대 100개 조회한다. 실제 발송 적격성은 다음 단계에서 확인한다.
4. 기존 `PushDeliveryService`가 Token을 다시 확인하고 `push_delivery`를 선점한다.
5. 기존 Expo 배치 발송과 `PUSH_RECEIPT_CHECK` 흐름으로 Ticket과 Receipt를 기록한다.
6. 마지막 대상 ID를 캠페인 커서에 저장하고 다음 페이지를 SQS에 발행한다.

별도의 실행, lease, polling, rate-limit 테이블은 두지 않는다. SQS 재전달은 `push_delivery.deduplication_key` 유일 제약으로 무해하게 처리한다. 외부 요청 전에 만들어진 이력은 자동 재제출하지 않아 중복 발송을 우선 방지한다. Ticket 저장 후 Receipt 예약이 실패한 경우에는 재처리에서 접수된 Ticket의 Receipt 작업만 다시 예약한다.

전체 발송 키는 다음과 같다.

```text
push:admin-broadcast:{campaignId}:{userProfileId}:{userPushTokenId}
```

테스트는 별도 키 공간을 사용하므로 이후 전체 발송을 막지 않는다.

```text
push:admin-broadcast-test:{campaignId}:{idempotencyKey}:{userPushTokenId}
```

## 데이터와 상태

`admin_push_campaign`에는 불변 원문, 생성 멱등성 키, `DRAFT/QUEUED/SENDING/COMPLETED` 상태, 대상 수와 처리 커서를 저장한다. `admin_push_target`에는 발송 요청 시점의 사용자와 Token ID만 저장한다. 결과 수는 기존 `push_delivery`에서 캠페인 키 접두어로 집계한다.

V83에서 `audience_type`과 `admin_push_campaign_user(campaign_id, user_profile_id)`를 추가한다. 캠페인과 선택 목록은 한 트랜잭션에 저장한다. 기존 캠페인은 ALL로 유지하며 기존 생성 요청 해시도 호환된다.

- 성공: Receipt가 `DELIVERED`인 Token 수.
- 실패: Ticket 또는 Receipt가 `FAILED`인 Token 수.
- 대기: `REQUESTED` 또는 `TICKET_ACCEPTED`인 Token 수.
- 제외: 처리 완료 커서 이하의 대상 수에서 같은 범위의 발송 이력 수를 뺀 값.

`COMPLETED`는 대상 페이지 제출 완료를 뜻하며 실제 기기 표시나 전원 성공을 뜻하지 않는다.

## 독립성 및 한계

- 일괄 공지는 `user_notification_state`를 읽거나 변경하지 않는다.
- 예약 학습 알림과 다른 `NotificationType` 및 중복 키를 사용한다.
- 발송 시작 후 활성화되거나 소유자가 바뀐 Token은 대상에 포함하지 않는다.
- SQS 발행 실패 시 같은 캠페인의 전체 발송 API를 다시 호출하면 미완료 캠페인을 재발행한다.
- 실제 운영 DB 적용, 실제 기기 테스트, FE 외부 URL 이동, 운영 전체 발송은 별도 승인과 검증이 필요하다.

## 검증

- 캠페인 생성 멱등성, 입력과 URL 형식.
- 대상 범위 고정, 비활성 Token 제외, 100건 배치.
- 중복 SQS 처리 시 동일 Token 재발송 방지.
- 테스트와 전체 발송 및 예약 학습 알림 키의 독립성.
- 캠페인 상태와 대상·성공·실패·제외 집계.
- `./gradlew check`와 인증·동시성·마이그레이션 독립 리뷰.
