# LAN-462 어드민 전체 푸시 캠페인 설계

## 목표

관리자가 제목, 내용, 딥 링크를 가진 캠페인을 만들고 본인 테스트 후 전체 활성 Expo Push Token에 비동기로 발송한다. 캠페인 생성 후 원문은 수정하지 않는다.

## API

- `POST /api/v1/admin/push-campaigns`: 캠페인 생성.
- `GET /api/v1/admin/push-campaigns`: 목록 조회.
- `GET /api/v1/admin/push-campaigns/{campaignId}`: 원문, 상태, 집계 조회.
- `GET /api/v1/admin/push-campaigns/{campaignId}/audience-preview`: 현재 예상 사용자·Token 수 조회.
- `POST /api/v1/admin/push-campaigns/{campaignId}/test`: 관리자 본인 테스트를 SQS에 발행.
- `POST /api/v1/admin/push-campaigns/{campaignId}/send`: 전체 발송을 SQS에 발행.

모든 API는 기존 `/api/v1/admin/**` 권한 검사를 사용한다. 생성·테스트·전체 발송은 기존 관리자 감사 로그에 기록한다.

## 입력 계약

- 제목, 내용, 딥 링크는 필수이며 최대 길이는 각각 255자, 500자, 1,000자다.
- 실제 Expo 표시 payload는 UTF-8 기준 3,000바이트 이하여야 한다.
- 딥 링크는 `/`로 시작하는 앱 내부 경로 또는 사용자 정보가 없는 `https` URL만 허용한다.
- 캠페인 생성과 테스트는 `Idempotency-Key`를 사용한다.

## 발송 흐름

1. 전체 발송 요청 시 활성 사용자와 Token ID를 캠페인 대상 테이블에 고정한다.
2. SQS에는 캠페인 ID만 발행한다.
3. 소비자는 고정된 대상 중 현재도 같은 활성 사용자가 소유한 활성 Token을 최대 100개 조회한다.
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

- 성공: Receipt가 `DELIVERED`인 Token 수.
- 실패: Ticket 또는 Receipt가 `FAILED`인 Token 수.
- 대기: `REQUESTED` 또는 `TICKET_ACCEPTED`인 Token 수.
- 제외: 최초 대상 Token 수에서 생성된 전체 발송 이력 수를 뺀 값.

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
