# LAN-462 어드민 전체 푸시 캠페인 설계

## 문서 상태와 목적

- 작성일: 2026-09-07.
- 상태: 구현 전 검토안. 사용자 요청에 따라 재설계한 문서이며 구현·배포·운영 발송 승인을 의미하지 않는다.
- 대상 독자: BE 구현자, 어드민·앱 FE 담당자, 리뷰어.
- 상세 구현 계약의 기준은 이 문서다. [issue.md](issue.md)는 이슈 등록용 범위와 완료 기준을 요약한다.
- 코드 확인 기준: `88ee78c2f3491c7be1fcb8c00223f206b89fe1a0`. 아래 신규 API·테이블·상태는 구현 제안이며 현재 존재하는 기능이 아니다.

## 목표와 범위

관리자가 제목·내용·딥 링크를 가진 캠페인을 생성하고, 본인에게 테스트한 뒤 활성 사용자들의 활성 Expo Push Token에 비동기로 발송한다. 발송 단위는 토큰이다. 여러 활성 기기를 가진 사용자는 기기마다 알림을 받을 수 있다.

v1은 다음 정책을 사용한다.

- 캠페인 내용은 생성 즉시 불변이다. 수정이 필요하면 새 캠페인을 생성한다. 수정·삭제·예약 발송·취소 API는 제공하지 않는다.
- 캠페인마다 전체 발송 실행은 하나다. 완료·실패 후에도 새 전체 실행을 만들지 않는다.
- 관리자 테스트는 별도 실행으로 관리한다. 의도적인 재테스트는 새 요청 키로 가능하다.
- 동일 캠페인의 같은 토큰에 대한 서버의 중복 제출 방지를 우선한다. 접수 여부가 불명확한 요청은 자동 재발송하지 않는다.
- 대상 확정 후 등록된 토큰을 추가하지 않는다. 미처리 대상의 복구는 최초 확정 목록을 사용한다.
- 예약 학습 알림의 선정·일일 제한·`user_notification_state`를 변경하지 않는다.
- 기존 Push SQS 소비 흐름에서 처리한다. 별도 Worker 저장소 작업이나 새 배포 단위를 이 이슈에 추가하지 않는다.
- 마케팅 캠페인 관리, 사용자 세그먼트, 이미지 첨부, 예약 시간 지정, 열람률·클릭률 추적은 범위 밖이다.

## 기존 코드와 재사용 경계

| 현재 구성 | 확인한 동작 | 이번 변경 방향 |
| --- | --- | --- |
| `NotificationDispatchService` | 사용자별 현재 활성 토큰을 다시 조회하고 100건씩 발송한다. | 기존 호출을 유지하고 확정된 토큰 목록을 받는 발송 진입점을 추가한다. 사용자 기준 재조회로 캠페인 대상이 늘어나지 않아야 한다. |
| `PushDeliveryService` | 잠금·유일 키로 발송을 선점하고 Ticket·Receipt를 기록한다. | 캠페인 대상 연결, 토큰 스냅샷 확인, 공지 전용 결과 불명 정책을 추가한다. |
| `ExpoPushClient` | `data.url`에 딥 링크를 넣고 타임아웃을 재시도 가능으로 분류한다. | 통신 결과를 구분할 정보를 제공하되 공지의 재발송 정책은 발송 계층에서 선택한다. |
| `NotificationTargetPageQueryService` | 활성 사용자와 학습 데이터를 대량 조회한다. | Keyset 방식만 참고한다. 공지는 학습 데이터 조회와 선정 서비스를 호출하지 않는다. |
| `PushNotificationConsumer` | 성공 시 ACK, 동시 처리 2건, visibility 연장을 사용한다. | 공지 메시지 유형을 추가하고 한 메시지의 외부 발송은 최대 100건으로 제한한다. |
| `UserPushToken.claim()` | 같은 토큰의 소유 계정을 변경할 수 있다. | 중복 식별자에 사용자 ID를 포함하지 않고 발송 직전 소유자를 재검증한다. |
| 관리자 필터·감사 서비스 | `/api/v1/admin` 권한과 쓰기 감사 로그를 제공한다. | 같은 경로·권한·감사 서비스와 `AdminAction`을 확장한다. |

기존 예약·편지함 알림의 키, 대상 선정, 재시도 정책, 집계 의미는 유지한다. 공통 코드 변경에는 이 흐름들의 회귀 검증이 필요하다.

## 발송 보장과 실패 정책

### 보장하는 범위

- 중복 API 요청·SQS 재전달·동시 소비로 같은 실행의 같은 토큰에 새 발송 이력을 만들지 않는다.
- Ticket 접수된 토큰에는 다시 알림을 제출하지 않고 Receipt 확인만 복구한다.
- HTTP 호출 직전 DB에 선점 기록을 커밋한다. 외부 HTTP 호출 동안 DB 트랜잭션을 유지하지 않는다.
- 선점 후 접수 여부를 증명할 수 없으면 결과를 `UNKNOWN`으로 분류한다. 실제로 호출 전에 프로세스가 종료됐어도 자동으로 다시 보내지 않는다.
- 이 선택으로 일부 알림이 누락될 수 있다. Expo·APNs·FCM·OS 단계의 중복 표시와 실제 기기 도착은 서버의 보장 범위가 아니다.

Expo 자체도 정확히 한 번 전달을 보장하지 않는다. SQS의 재전달 방지와 외부 Push의 전달 보장은 별개다. 공식 근거는 문서 마지막에 기재한다.

### 공지와 관리자 테스트의 결과 분류

| 상황 | 기록과 재처리 |
| --- | --- |
| DB 대상 선점 전에 장애 발생. | 같은 실행의 미선점 대상을 재처리한다. |
| 토큰 비활성·소유자 변경·사용자 비활성·토큰 문자열 변경. | `EXCLUDED`와 사유를 기록한다. |
| 유효한 Ticket 접수 응답. | `TICKET_ACCEPTED`. 발송 재시도 없이 Receipt 확인을 예약한다. |
| 명시적 Ticket 오류 또는 명확한 요청 거부. | `FAILED`. `DeviceNotRegistered` 등 기존 토큰 무효화 정책을 적용한다. |
| HTTP 429로 명시적으로 거부됨. | 최초 제출 이후 최대 3회, 각각 5초·30초·120초 뒤 재시도하고 초과하면 `FAILED`로 종료한다. 재시도 횟수·다음 시각은 이력에 저장한다. |
| 응답 타임아웃·연결 오류·중단·HTTP 5xx·성공 응답 파싱 실패·Ticket 개수 불일치. | 접수 여부를 보수적으로 판단해 `UNKNOWN`. 자동 재제출하지 않는다. |
| 선점 커밋 후 프로세스 종료·Ticket 저장 실패. | 남은 선점 기록을 `UNKNOWN`으로 정리한다. 정상 처리 중인 요청은 작업 lease가 만료되기 전 정리하지 않는다. |
| Receipt 성공. | `SUCCEEDED`. APNs/FCM 인계 성공으로 표시한다. |
| Receipt의 명시적 오류. | `FAILED`. 기존 등록 해제 토큰 처리를 적용한다. |
| Receipt를 최종 확인하지 못함. | `UNKNOWN`. Receipt 조회만 재시도하며 원본 푸시는 다시 보내지 않는다. |

429 외의 공지 발송 오류는 v1에서 자동 재제출하지 않는다. 공통 클라이언트의 `RetryablePushNotificationException`을 공지에서 일괄 재시도하는 구현은 금지한다. 기존 예약·편지함 알림은 기존 정책을 유지한다.

이미 실행 중이던 요청의 Ticket 결과가 늦게 저장되면 동일 이력의 `UNKNOWN`을 `TICKET_ACCEPTED`로 보정할 수 있다. 유효한 Receipt도 같은 이력의 결과를 보정할 수 있다. 이는 새 HTTP 발송을 만드는 동작이 아니며 상세 응답의 `updatedAt`에 반영한다.

## 캠페인·실행·토큰 식별

캠페인 ID와 실행 ID는 서버 발급 UUID다. 전체 발송과 테스트 발송에 각각 `ADMIN_BROADCAST`, `ADMIN_BROADCAST_TEST` 알림 유형을 추가한다. 기존 개발용 `TEST_NOTIFICATION`은 사용하지 않는다.

```text
전체 발송: push:admin-broadcast:{campaignId}:{userPushTokenId}
테스트: push:admin-broadcast-test:{runId}:{userPushTokenId}
```

`userProfileId`는 발송 당시 소유자 정보로 저장하지만 중복 키에 넣지 않는다. 전체 실행을 캠페인당 하나로 제한하고 대상에 `(run_id, user_push_token_id)` 유일 제약을 두어 같은 토큰의 소유자가 바뀌어도 추가 발송 대상이 생기지 않게 한다.

API 멱등성 키는 `Idempotency-Key` 헤더로 받는다. 1~128자의 ASCII 영숫자·`-`·`_`를 허용한다. 생성과 테스트의 범위는 인증 관리자·작업·캠페인 식별자 조합이다. 생성 요청은 정규화한 본문의 해시도 저장한다.

- 같은 키와 같은 입력이면 기존 캠페인 또는 실행 ID를 반환한다.
- 같은 키에 다른 입력을 사용하면 `409 IDEMPOTENCY_KEY_CONFLICT`다.
- 전체 발송은 다른 키·다른 관리자가 요청해도 기존 전체 실행을 반환한다. 새 실행·감사 로그·대상을 추가하지 않는다.
- 캠페인 생성은 다른 키로 요청하면 별도 캠페인이다. 같은 내용을 서로 다른 캠페인으로 생성한 경우까지 내용 비교로 중복 제거하지 않는다.
- 키는 연관 캠페인·실행의 보존 기간 동안 유지한다. v1에서 별도 자동 삭제나 TTL은 적용하지 않는다.

## 대상 확정

### 예상 대상

예상 대상 조회는 조회 시점의 `user_profile.status = ACTIVE`와 `user_push_token.status = ACTIVE` 조인 결과를 사용한다. `estimatedUserCount`, `estimatedTokenCount`, `estimatedAt`을 함께 반환한다. 별도 학습 이력 조건을 적용하지 않는다. v1의 대상 계약은 활성 토큰 기준이며, 사용자 프로필의 푸시 권한 필드를 별도 필터로 추가하지 않는다.

예상치는 예약·잠금·확정이 아니다. 어드민에 실제 확정 수가 달라질 수 있음을 표시한다.

### 실제 대상 스냅샷

1. 발송 요청은 DB에 실행을 저장하고 `202 Accepted`를 반환한다.
2. 소비자가 실행을 선점한 뒤 대상 목록을 확정한다. 기준은 HTTP 요청 시각이 아니라 이 준비 단계에서 실행하는 DB 조회 시점이다.
3. 하나의 `INSERT ... SELECT` 문장으로 활성 사용자·토큰을 대상 테이블에 저장한다. 토큰 ID·소유 사용자 ID·Expo 토큰 문자열을 함께 저장한다.
4. 대상 저장, `audienceCapturedAt`, 확정 수, 준비 완료 상태는 같은 트랜잭션으로 커밋한다. 실패 시 모두 롤백하고 재시도한다. 준비 완료 실행의 대상을 다시 계산하지 않는다.
5. 네트워크 발송은 저장된 대상 ID를 Keyset으로 최대 100건씩 읽는다. 전체 목록을 JVM 메모리에 올리지 않는다.

단일 SQL 스냅샷을 사용하므로 여러 페이지에 걸친 원본 조회 시점 차이가 없다. 구현 시 운영 규모에 해당하는 데이터로 DB 실행 시간·인덱스를 확인한다. 준비 트랜잭션을 안정적으로 실행할 수 없을 정도의 규모라면 구현 전에 대상 확정 방식을 재설계해야 한다.

발송을 선점하는 트랜잭션에서 현재 토큰 활성 상태·소유자·문자열과 사용자 활성 상태를 다시 검사한다. 상태가 달라졌으면 제외한다. 검사 커밋 뒤 외부 발송 사이의 탈퇴·권한 변경을 완전히 차단하는 것은 보장하지 않는다.

대상이 0건이면 정상 완료하며 확정 수와 모든 결과 수는 0이다. 테스트는 추가로 `NO_ACTIVE_PUSH_TOKEN` 안내를 반환한다.

### 관리자 테스트

- 수신자는 인증 principal에서 결정한다. API에 임의 사용자 ID·Expo 토큰 입력을 받지 않는다.
- 요청 관리자의 모든 활성 토큰에 캠페인의 원문 제목·내용·딥 링크를 사용한다. 테스트 접두사를 메시지에 붙이지 않는다.
- 전체 발송 시작 전 캠페인에서만 새 테스트를 만들 수 있다. 동일 키로 기존 테스트를 조회하는 재요청은 가능하다.
- 새 키의 테스트 실행은 별도 대상·키·집계를 가진다. 본 발송 중복 키를 소비하지 않는다.
- 관리자가 새 키로 반복 클릭하는 것을 제한하기 위해 관리자별 새 테스트 생성은 10초에 1회로 제한한다. 같은 키 재요청은 제한에 포함하지 않는다.
- 테스트 성공을 전체 발송 API의 강제 선행 조건으로 만들지는 않는다. 어드민에서 최근 테스트 상태를 표시한다.

## 데이터 모델

Flyway 신규 마이그레이션으로 세 테이블과 `push_delivery`의 nullable 연결 컬럼을 추가한다. 실제 마이그레이션 번호는 구현 시 최신 이력을 확인해 정한다.

| 테이블 | 핵심 필드·제약 | 역할 |
| --- | --- | --- |
| `admin_push_campaign` | UUID PK, title/body/deep_link, created_by, create_request_key, request_hash, created_at. `(created_by, create_request_key)` 유일. | 변경되지 않는 원문과 생성 멱등성. |
| `admin_push_run` | UUID PK, campaign_id FK, mode, requested_by, request_key, status, audience_captured_at, target_user_count, target_token_count, next_attempt_at, lease_owner, lease_until, work_version, published_at, failure_count, last_error_code, completed_at, created_at/updated_at. | 전체 또는 테스트 실행, 진행 상태, DB에 저장한 SQS 발행·복구 작업. |
| `admin_push_target` | ID PK, run_id FK, user_push_token_id, user_profile_id, expo_push_token_snapshot, excluded_reason, created_at. `(run_id, user_push_token_id)` 유일. | 확정 대상과 선점 전 제외 사유. |
| 기존 `push_delivery` | nullable `admin_push_target_id` FK와 유일 제약. | 대상당 하나의 발송 이력. 기존 일반 푸시는 null이다. |

`admin_push_run`은 `mode = BROADCAST`인 `campaign_id`에 부분 유일 인덱스를 둔다. 테스트 실행은 `(campaign_id, requested_by, request_key, mode)`를 유일하게 만든다. PostgreSQL 부분 인덱스 계약은 PostgreSQL에서 검증한다.

주요 조회 인덱스는 실행의 `(status, next_attempt_at)`, 대상의 `(run_id, id)`, 캠페인의 `(created_at, id)`다. 집계 쿼리 계획에 따라 필요한 연결 인덱스를 확인한다.

공지는 기존 `PushDeliveryStatus`에 `UNKNOWN`을 추가하고 명시적 오류 코드를 저장한다. 제외는 `push_delivery`를 만들기 전 대상 테이블에 기록한다. Receipt 조회 한도 초과는 공지에 한해 `UNKNOWN`이며 기존 알림의 상태 의미를 바꾸지 않는다.

Repository는 각 소유 Service 안에서만 접근한다. 캠페인 실행 서비스는 `PushDeliveryService`와 record 계약을 통해 이력·결과를 처리한다. 별도의 범용 캠페인 엔진·범용 Outbox 프레임워크는 만들지 않는다.

## 상태와 비동기 처리

### 상태 모델

캠페인 표시 상태는 전체 실행으로부터 계산한다. 전체 실행이 없으면 `DRAFT`다. 내용은 `DRAFT`에서도 변경할 수 없다.

| 실행 상태 | 의미 |
| --- | --- |
| `QUEUED` | 발송 요청을 DB에 저장했으며 대상 준비를 기다린다. SQS 발행 성공을 의미하지 않는다. |
| `PREPARING` | 대상을 확정하는 작업이 진행 중이다. |
| `SENDING` | 확정 대상에 대해 선점·발송·429 재시도를 진행한다. |
| `AWAITING_RECEIPTS` | 모든 대상의 제출 여부 처리가 끝났고 Receipt 결과를 기다린다. |
| `COMPLETED` | 미처리·확인 대기 대상이 없다. 실패·제외·확인 불가가 포함될 수 있다. |
| `BLOCKED` | DB/SQS 등 작업 장애가 반복되어 자동 진행을 중단했다. 기존 부분 결과와 미처리 수를 보존한다. |

캠페인 표시와 전체 발송 수치는 전체 실행만 사용한다. 테스트 실행 상태·집계는 별도 목록으로 반환한다. `COMPLETED`를 전원 성공이나 기기 수신 완료로 표현하지 않는다.

### DB 저장과 SQS 발행

실행 행 자체가 발행할 작업의 지속성 저장소다. API 트랜잭션에서 실행과 감사 로그를 함께 커밋하고, 알림 소비가 활성화된 기존 BE 런타임의 주기 작업이 실행 ID를 SQS에 발행한다. 사용자 요청 스레드에서 전체 대상 조회나 Expo 발송을 수행하지 않는다.

```json
{
  "version": 1,
  "messageType": "ADMIN_PUSH_RUN",
  "messageId": "admin-push:{runId}:{workVersion}",
  "occurredAt": "2026-09-07T03:00:00Z",
  "payload": { "runId": "UUID", "workVersion": 1 }
}
```

실제 JSON 필드 연결은 기존 `PushQueueMessage`·`PushQueuePayload`에 맞춰 추가한다. 제목·내용·대상 토큰은 SQS 메시지에 넣지 않는다.

- 발행기는 만기가 된 작업을 DB 잠금·짧은 발행 lease로 선점하고 네트워크 호출은 트랜잭션 밖에서 수행한다.
- SQS 전송 응답을 잃으면 같은 실행·작업 버전을 다시 발행할 수 있다. 발행 성공 표시와 DB 커밋을 외부 SQS와 원자적으로 묶으려 하지 않는다.
- 소비자는 실행 상태·작업 버전을 검사하고 처리 lease를 원자적으로 선점한다. 처리 중 중복 메시지와 이전 버전 메시지는 무해하게 종료한다.
- 유효한 새 버전만 처리한다. 현재보다 미래 버전·알 수 없는 실행·잘못된 payload는 계약 오류로 처리한다.
- 한 메시지에서는 준비 작업 하나 또는 최대 100개 대상의 Expo 요청 하나를 수행하고 다음 작업을 DB에 남긴 뒤 ACK한다. 페이지 진행과 `work_version` 변경은 같은 트랜잭션이다.
- 다음 메시지 발행은 DB 발행기가 담당한다. 다음 SQS 발행 실패 때문에 완료한 페이지를 다시 제출하지 않는다.
- 현재 발행됐지만 5분 동안 처리 시작이 없는 작업과 만료된 처리 lease를 복구 스캔한다. 현재 설정의 HTTP 타임아웃 10초보다 충분히 긴 처리 lease 5분을 기본으로 사용한다. 설정 변경 시 이 관계를 검증한다.
- lease 소유자·작업 버전을 조건으로 진행 상태를 갱신해 오래된 소비자가 새 작업 상태를 덮어쓰지 못하게 한다. 각 토큰의 외부 제출 가능 여부는 별도로 `push_delivery`에서 선점한다.
- 멈춘 실행을 복구할 때는 대상별 기존 이력을 먼저 확인한다. 선점되지 않은 대상과 명시적 429 재시도 대상만 발송할 수 있다. 결과가 불명확한 선점 이력을 새 것으로 만들지 않는다.

DB/SQS 작업 실패는 5초부터 지수적으로 최대 5분 간격으로 재시도하고 연속 8회 실패하면 `BLOCKED`로 표시한다. 성공한 작업 뒤에는 연속 실패 수를 초기화한다. 전체 발행기 자체가 작동하지 않는 장애는 행별 상태 갱신도 불가능하므로 마지막 진행 시각 기반 경보로 감지한다.

`BLOCKED` 실행은 관리자 복구 API로 같은 실행을 재개한다. 대상 확정 전이면 준비 단계부터, 확정 후이면 저장된 대상과 이력으로 다음 작업을 결정한다. 복구는 미처리 대상·명시적 재시도·Receipt 확인만 대상으로 한다. 이미 실패·확인 불가로 끝난 알림을 다시 제출하지 않는다. 원본 SQS 메시지가 DLQ로 이동했더라도 DB 진행 상태가 기준이며, DLQ 재전달은 같은 멱등성 검사를 거친다.

### Receipt 복구

기존 15분 지연 Receipt 확인과 최대 3회 확인 흐름을 재사용한다. 공지의 Ticket 저장 후 Receipt SQS 발행에 실패한 경우를 위해 캠페인 복구 작업이 `TICKET_ACCEPTED` 이력을 다시 찾을 수 있어야 한다.

Receipt 확인 시도·다음 확인 시각은 공지 이력에 지속성 있게 기록한다. 중복 SQS 메시지마다 횟수를 증가시키거나 첫 시도로 초기화하지 않는다. 기존 예약 메시지와 공지 복구 스캔은 같은 확인 회차를 선점한다. 최종 미확인은 `UNKNOWN`이다. 원본 Expo 발송은 다시 하지 않는다.

필요한 공지용 확인 회차·다음 시각·lease 필드는 `push_delivery`에 nullable로 추가한다. 기존 알림의 Receipt 계약은 유지한다.

## 예약 알림과 처리량

독립성은 다음 범위로 정의한다.

- 공지는 예약 학습 알림의 사용자별 일일 키와 상태를 소비하거나 변경하지 않는다.
- 같은 날 다른 캠페인 및 20시 학습 알림은 각각 발송 대상이 될 수 있다.
- 공지 발송 결과로 유효하지 않은 토큰이 발견되면 기존 정책대로 비활성화한다. 이후 예약 알림에서 그 토큰이 빠지는 것은 정상적인 토큰 관리다.
- 기존 SQS·Expo 자원을 공유하므로 20시 알림의 지연이 전혀 없다는 보장은 하지 않는다.

v1 공지 작업은 모든 인스턴스를 합쳐 최대 한 개의 외부 제출 배치를 수행하고 최대 초당 100토큰으로 제한한다. DB에서 공지 발송 공용 선점을 관리하며 테스트와 429 재시도도 이 제한에 포함한다. 한 실행이 전체 루프를 점유하지 않고 최대 100건마다 큐에 실행 기회를 돌려준다. 캠페인 간 처리는 만기 시각 순으로 분배한다.

공용 선점은 실행별 lease와 별개의 범위다. HTTP 제출 직전에 공용 선점과 실행 lease의 소유권·유효성을 다시 확인하며 상실한 소비자는 새 HTTP 호출을 시작하지 않는다. 프로세스 정지 후 늦게 재개되는 경계까지 외부 제공자의 동시 요청 수를 절대 보장하는 것은 아니므로, 초당 제한은 운영 부하 검증과 함께 확인한다. 토큰별 이미 선점한 요청을 다른 소비자가 다시 제출하지 않는 규칙은 유지한다.

이는 공지의 기여량만 제한한다. Expo의 프로젝트 단위 제한은 다른 알림을 포함하므로 실제 동시 부하를 측정해야 한다. 현재 Expo 문서는 프로젝트당 초당 600건을 명시한다. 별도 큐·소비자 분리는 v1 필수 조건이 아니며, 처리 지연 보장이 필요해지면 IaC와 함께 별도 범위로 결정한다.

## 입력과 FE 계약

### 공통 입력

- `title`: 앞뒤 공백 제거 후 비어 있지 않은 문자열, 최대 255자.
- `body`: 앞뒤 공백 제거 후 비어 있지 않은 문자열, 최대 500자. 본문 줄바꿈은 허용한다.
- `deepLink`: 앞뒤 공백 제거 후 필수 문자열, 최대 1,000자. 링크 없이 보내려면 관리자가 `/home`을 지정한다.
- 제목에는 줄바꿈을 허용하지 않는다. 본문 줄바꿈을 제외한 제어문자와 링크의 공백·제어문자는 거부한다.
- 길이 상한은 Bean Validation과 DB 컬럼 제한을 함께 맞춘다. 한글·이모지와 JSON escaping을 포함한 UTF-8 payload 바이트도 검증한다.
- v1은 공통 표시 필드 `title`, `body`, `data.url`, `sound`, `channelId`를 실제 serializer로 직렬화한 크기를 3,000바이트 이하로 제한한다. Expo의 최종 4,096바이트 제한보다 여유를 둔 자체 입력 제한이며 `MessageTooBig` 수신도 처리한다. 바이트 제한 오류는 `400 PUSH_PAYLOAD_TOO_LARGE`다.

### 딥 링크

| 종류 | 허용 조건 | 거부 예시 |
| --- | --- | --- |
| 앱 내부 경로. | 단일 `/`로 시작하고 scheme·authority가 없는 유효한 URI. query·fragment 허용. | `//example.com`, `/\\example.com`, 잘못된 percent encoding, 제어문자. |
| 외부 URL. | `https` scheme, 유효한 DNS 호스트, userinfo 없음, 기본 포트 또는 443. | `http:`, `javascript:`, `intent:`, `file:`, `https://`, `https://user:pass@example.com`, IP literal. |

v1은 특정 도메인 목록을 운영하지 않고 위 조건을 만족하는 외부 HTTPS를 허용한다. URL parser 결과로 검증하며 `startsWith("https://")`만 사용하지 않는다. 역슬래시·인코딩된 제어문자·중복 디코딩으로 authority가 되는 내부 경로도 거부한다. FE와 BE는 동일한 허용·거부 예제 묶음을 공유한다. 서버가 링크를 fetch하거나 리다이렉트를 따라가지는 않는다.

payload는 기존과 동일하게 `data.url`을 사용한다. FE는 내부 경로를 앱 라우터로, 외부 HTTPS를 시스템 브라우저로 연다. 존재하지 않는 내부 경로는 `/home`으로 이동한다. 인증이 필요한 화면은 로그인 후 원래 경로로 이동한다. 제목·본문·URL에 테스트용 문구나 추적 query를 서버가 자동 추가하지 않는다.

FE가 지원하지 않는 경로·외부 URL 처리는 BE URI 검증만으로 보장할 수 없다. 현재 배포 앱의 지원 여부는 미확인이다. 구현 핸드오프 시 FE가 해당 계약을 확인하고, 구버전 앱 대응을 포함해 출시 순서를 확정해야 한다. 지원되지 않는 외부 URL을 운영 발송에 사용하지 않는다.

## 관리자 API

기준 경로는 `/api/v1/admin/push-campaigns`다. 모든 API에 기존 관리자 인증·권한을 적용한다. 일반 사용자에게는 `403`, 미인증 요청에는 `401`을 반환한다. 응답은 기존 `ApiResponse` 형식을 따른다.

| 메서드·경로 | 입력 | 응답·동작 |
| --- | --- | --- |
| `POST /` | `Idempotency-Key`, title/body/deepLink. | `201`, campaignId와 불변 원문. 동일 요청은 기존 ID를 반환한다. |
| `GET /` | 기존 프로젝트 방식의 페이지 파라미터. | 캠페인 목록과 전체 실행 상태. |
| `GET /{campaignId}` | 없음. | 원문, 생성자·시각, 전체 실행 상태·집계, 테스트 실행 요약. |
| `GET /{campaignId}/audience-preview` | 없음. | estimatedUserCount/estimatedTokenCount/estimatedAt. |
| `POST /{campaignId}/test-runs` | `Idempotency-Key`. | `202`, 인증 관리자 대상 runId. 전체 발송 시작 후 새 테스트는 `409`. |
| `POST /{campaignId}/send` | `Idempotency-Key`. | `202`, 유일한 전체 runId. 중복 요청은 같은 runId를 반환한다. |
| `GET /{campaignId}/runs/{runId}` | 없음. | 실행 유형·상태·시각·집계·오류 사유별 건수. |
| `POST /{campaignId}/runs/{runId}/resume` | 없음. | `BLOCKED` 실행의 복구 요청. 같은 실행을 재개하며 이미 재개 중이면 현재 상태를 반환한다. 완료 실행은 `409`. |

모든 쓰기 API의 상태 검사·실행 유일성·감사 로그는 같은 트랜잭션으로 처리한다. 테스트 생성과 전체 발송 요청도 캠페인 잠금으로 직렬화한다. resume는 상태 전이 자체로 멱등이며 반복 요청마다 작업을 추가하지 않는다.

관리자에게 Expo 토큰 원문을 응답하지 않는다. 대상별 원문 토큰 조회·내보내기 API는 v1 범위 밖이다. 새 본 발송을 위해 새 캠페인을 만들면 이미 받은 사용자가 다시 받을 수 있으므로, 어드민에서는 기존 실행 복구와 새 캠페인 생성을 구분해 표시한다.

## 집계와 감사 로그

집계는 대상 목록과 연결된 `push_delivery`를 읽어 계산한다. 재처리 때마다 카운터를 더하지 않는다. v1에서 별도 통계 캐시·집계 테이블을 만들지 않는다. 한 응답의 합계는 단일 집계 쿼리의 일관된 스냅샷에서 계산한다.

```text
targetTokenCount = pendingCount + succeededCount + failedCount + excludedCount + unknownCount
```

- `pendingCount`: 아직 선점되지 않은 대상, 처리 중, 429 재시도 대기, Receipt 확인 대기.
- `succeededCount`: Receipt에서 APNs/FCM 인계 성공이 확인된 대상.
- `failedCount`: 명시적 제출·Receipt 오류 및 제한된 429 재시도 소진 대상.
- `excludedCount`: 대상 확정 후 사전 검증에서 제외된 대상.
- `unknownCount`: 제출·Receipt 결과를 확인할 수 없는 대상.
- `ticketAcceptedCount`: 유효한 Expo Ticket ID를 받은 대상 수인 별도 참고 지표. 위 합계 항목에 더하지 않는다.

준비 완료 전 확정 수는 `null`로 반환해 0건 완료와 구분한다. `BLOCKED`에서는 pending이 남을 수 있다. `COMPLETED`는 pending이 0일 때만 가능하며 늦은 결과 보정으로 다시 Receipt 대기가 생기면 상태도 재계산한다. 성공 지표에 기기 표시·사용자 열람이라는 이름을 사용하지 않는다.

생성·테스트 요청·전체 발송 요청·복구 요청을 각각 관리자 감사 action으로 기록한다. 관리자 ID, 캠페인·실행 ID, 작업, 상태 전이, 시각을 남긴다. 단순 중복 재요청은 새 효과를 만들지 않으므로 동일 감사 이벤트를 추가하지 않는다. 알림 본문·전체 URL·토큰 원문은 감사 before/after에 복사하지 않고 불변 캠페인 참조로 추적한다.

자동 재시도·제외·발송 오류는 실행 이력과 운영 로그에 기록한다. 토큰 원문을 로그·메트릭 태그에 넣지 않는다. 장기 정체 실행·BLOCKED·SQS 발행 실패·DLQ·UNKNOWN 급증을 감지할 수 있게 지표와 운영 확인 절차를 제공한다.

## 검증과 구현 완료 기준

| 구분 | 반드시 확인할 사례 |
| --- | --- |
| API·권한. | 관리자 정상 요청, 미인증·일반 사용자 거부, 임의 테스트 대상 지정 불가, 없는 캠페인·실행 접근. |
| 입력·FE. | 공백·경계 길이·한글·이모지·payload 바이트, URL 허용·거부 corpus, 앱 실행·백그라운드·종료 상태의 내부/외부 이동, 미지원 경로 fallback. |
| 멱등성·동시성. | 같은 생성/테스트 키 동시 요청, 다른 입력 충돌, 같은 캠페인에 서로 다른 관리자의 send 동시 요청, SQS 중복 소비, 이전 작업 버전 재전달. |
| 대상. | 다기기, 비활성 사용자/토큰, 대상 0건, 예상과 확정 차이, 확정 후 신규 토큰, 소유자 변경, 토큰 문자열 변경, 준비 트랜잭션 실패·재처리. |
| 테스트 분리. | 같은 테스트 키 재요청, 새 키 재테스트, 관리자 토큰 0건, 테스트 후 본 발송 수신 가능, 본 발송 통계에 테스트 미포함. |
| 장애. | DB 커밋 뒤 SQS 실패·응답 유실, 다음 페이지 발행 실패, visibility 만료, 선점 직후 종료, Expo 접수 후 타임아웃·Ticket 저장 실패, 429 제한 재시도, 5xx·응답 불일치, DLQ·BLOCKED 복구. |
| Receipt·집계. | Ticket 후 Receipt 예약 유실, 중복 확인 회차, 최종 미확인 UNKNOWN, 늦은 결과 보정, 집계 등식, 중복 재처리 전후 대상 수 불변. |
| 기존 기능. | 예약·편지함 알림의 키·재시도 회귀 없음, 공지 전후 `user_notification_state` 불변, 같은 날 여러 캠페인·예약 알림 각각 처리 가능. |
| 부하·DB. | 100건 배치 제한, 공지 공용 제한의 다중 인스턴스 적용, 실제 규모 스냅샷·페이지·집계 쿼리, 예약 알림과 공유 큐 지연 측정. |

일반 단위·통합 테스트는 기존 도구를 사용한다. 유일 제약·행 잠금·경합·단일 SQL 스냅샷·마이그레이션은 격리된 PostgreSQL에서도 검증한다. H2 통과만으로 운영 PostgreSQL 경합 검증을 대신하지 않는다. 사용 가능한 검증 환경이 없다면 해당 증거는 미확인으로 남긴다.

코드 구현 후 `./gradlew check`를 실행한다. 인증·동시성·마이그레이션·공통 발송 경계 변경이므로 독립 리뷰를 수행하고 지적된 차단 사항을 수정한다. 실제 기기 테스트는 관리자 본인 기기에서 승인된 테스트 발송으로 수행하고 전체 운영 발송과 구분한다.

## 출시와 핸드오프

- BE: OpenAPI, 상태·오류 코드·멱등성 키·집계 의미·장애 복구 절차를 제공한다.
- 어드민: 예상 사용자/토큰 수, 테스트 실행 상태, 전체 발송 상태, 결과 불명 건수, 복구와 새 캠페인의 차이를 표시한다. 버튼 비활성화 외에 서버 상태를 기준으로 중복 요청을 처리한다.
- 앱 FE: `data.url` 검증·내부 경로·외부 HTTPS·인증 후 이동·앱 종료 상태 동작과 구버전 호환을 확인한다.
- 운영: 소비자·DB 발행기 활성 여부, SQS/DLQ 권한과 관측, 공지 처리량 제한을 확인한다. 기존 BE 소비자 배포와의 소유 경계를 확인하고 IaC 변경이 필요하면 별도 범위를 제시한다.
- DB 마이그레이션 적용, 운영 데이터 수동 수정, 실제 운영 테스트·전체 발송은 별도 승인을 받은 뒤 진행한다. 이 문서 작성과 API 구현은 그 승인을 포함하지 않는다.

## 검토 근거와 현재 검증 수준

다음 파일의 현재 소스를 읽어 재사용 경계와 제약을 확인했다.

- [NotificationDispatchService](../../../src/main/java/com/landit/landitbe/feature/notification/service/NotificationDispatchService.java).
- [PushDeliveryService](../../../src/main/java/com/landit/landitbe/feature/notification/service/PushDeliveryService.java), [PushDeliveryRepository](../../../src/main/java/com/landit/landitbe/feature/notification/repository/PushDeliveryRepository.java).
- [ExpoPushClient](../../../src/main/java/com/landit/landitbe/feature/notification/client/expo/ExpoPushClient.java), [PushReceiptService](../../../src/main/java/com/landit/landitbe/feature/notification/service/PushReceiptService.java).
- [UserPushToken](../../../src/main/java/com/landit/landitbe/feature/notification/domain/UserPushToken.java), [UserPushTokenDeliveryService](../../../src/main/java/com/landit/landitbe/feature/notification/service/UserPushTokenDeliveryService.java).
- [PushNotificationConsumer](../../../src/main/java/com/landit/landitbe/feature/notification/messaging/PushNotificationConsumer.java), [SqsPushQueuePublisher](../../../src/main/java/com/landit/landitbe/feature/notification/messaging/SqsPushQueuePublisher.java).
- [기존 스키마](../../../src/main/resources/db/migration/V69__add_push_delivery.sql), [기존 예약 정책](../LAN-184/design.md).

2026-09-07 확인한 공식 자료는 다음과 같다.

- [Expo 전달 보장·프로젝트 처리량](https://docs.expo.dev/push-notifications/faq/): 정확히 한 번 전달 보장 부재와 프로젝트당 초당 600건 제한.
- [Expo 발송·Ticket·Receipt·payload 제한](https://docs.expo.dev/push-notifications/sending-notifications/): Receipt 의미, 100건 발송 배치와 payload 제한.
- [SQS at-least-once delivery](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/standard-queues-at-least-once-delivery.html): 재전달을 고려한 소비자 멱등성 필요.

현재 검증은 소스 대조, 문서 구조·로컬 링크 검사, 문서 수준의 독립 설계 리뷰까지다. 독립 리뷰에서 차단 결함은 발견되지 않았으며 공용 lease 상실 후 제출 금지 조건을 보완했다. 신규 API·테이블·복구 동작·성능·FE·운영 런타임은 아직 구현 또는 검증하지 않았다. 이 문서만 변경한 단계에서는 애플리케이션 테스트를 실행하지 않는다.
