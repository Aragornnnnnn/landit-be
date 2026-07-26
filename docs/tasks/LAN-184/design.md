# LAN-184 푸시 알림 백엔드 설계

## 최종 정책 확장

`SCHEDULED_NOTIFICATION_BATCH`는 EventBridge Scheduler가 매일 20시 `Asia/Seoul` 기준으로 Push Queue에 발행한다. 기존 API 서버의 Consumer가 500명 Keyset 페이지로 활성 사용자를 조회하고, 페이지마다 시나리오·표현·완료 이력·발송 가능한 설치를 일괄 조회한다.

- 사용자별 최근 실제 완료 시각으로 `CONTINUE_SCENARIO`, `CONTINUE_EXPRESSION`, `REVIEW_LEARNING` 중 하루 한 건을 선정한다.
- 시나리오는 카테고리별 순차 잠금을 유지하고, 표현은 부모 시나리오가 `CLEARED`인 경우만 알림 후보가 된다. 이 조건은 기존 표현 API의 접근 규칙을 바꾸지 않는다.
- 계산 결과는 `user_notification_state`에 스냅샷으로 저장한다. 실제 발송은 기존 `push_delivery`의 날짜·사용자·기기·유형 멱등성으로 한 번만 처리한다.
- Listener는 `ON_SUCCESS`로 ack하며, 배치 처리 시작과 페이지 전환 시 `Visibility.changeTo(300)`으로 현재 SQS 메시지 visibility를 연장한다.
- Scheduler payload는 `version`, `messageId`, `messageType`, `occurredAt`, 빈 `payload` 객체를 사용한다. Scheduler는 BE 호환 배포 전까지 비활성화한다.

## 현재 범위

이번 구현은 앱 설치 상태와 공통 Push 전달 인프라까지만 책임진다. 대상 선정, 반복 주기, 우선순위, 문구와 딥링크 같은 제품 정책은 아직 확정되지 않았으므로 후속 PR로 분리한다.

```mermaid
flowchart LR
    APP["Landit 앱"] -->|"설치 상태 동기화"| API["기존 API 서버"]
    PRODUCER["정책 Service 또는 dev 테스트 API"] --> SQS["Push SQS"]
    SQS --> CON["PushNotificationConsumer"]
    CON --> HANDLER["PushQueueMessageHandler"]
    HANDLER --> SEND["NotificationDispatchService"]
    SEND --> DB[("PostgreSQL")]
    SEND --> EXPO["Expo Push Service"]
    EXPO --> DEVICE["iOS / Android"]
    SEND -->|"Receipt 확인 예약"| SQS
    SQS -->|"반복 실패"| DLQ["Push DLQ"]
```

`PushNotificationConsumer`는 기존 API 애플리케이션 내부 Listener이며 초기 동시성은 2다. 별도 Push Worker는 추가하지 않는다.

## 설치 상태 API

```http
PUT /api/v1/me/push-devices/{installationId}
Authorization: Bearer {accessToken}
```

동일한 `installationId`는 행을 추가하지 않고 현재 사용자, 플랫폼, 수신 설정과 Token으로 갱신한다. 같은 Expo Token이 다른 설치에 연결돼 있으면 기존 연결을 해제한다.

발송 가능한 설치는 아래 조건을 모두 만족해야 한다.

```text
pushEnabled == true
&& expoPushToken != null
&& tokenStatus == ACTIVE
```

## Queue와 발송

확정된 대상 사용자와 알림 내용은 `PUSH_SEND` 메시지로 전달한다.

```json
{
  "version": 1,
  "messageId": "<event id>",
  "messageType": "PUSH_SEND",
  "occurredAt": "<event time>",
  "payload": {
    "userProfileId": 1,
    "notificationType": "TEST_NOTIFICATION",
    "title": "Landit 알림 테스트",
    "body": "푸시 알림이 정상적으로 도착했어요.",
    "deepLink": "/home"
  }
}
```

`NotificationDispatchService`는 메시지의 사용자에게 속한 최신 발송 가능 설치를 조회한다. 설치별 `push_delivery` 이력을 먼저 저장하고, 선점된 알림을 최대 100건씩 Expo에 보낸다. 중복 방지 키는 `push:{messageId}:{pushDeviceId}`다.

상태는 `REQUESTED → TICKET_ACCEPTED → DELIVERED`이며 실패는 `FAILED`로 기록한다. Ticket 접수 뒤 `PUSH_RECEIPT_CHECK`를 900초 지연 발행하고 Receipt가 준비되지 않으면 최대 세 번 확인한다.

- HTTP 429·5xx·timeout만 같은 발송 이력으로 재시도한다.
- 일반 I/O, interruption, 응답 파싱 실패는 자동 재발송하지 않는다.
- `DeviceNotRegistered`는 발송 당시 Token의 현재 소유 설치를 `INVALID`로 변경한다.
- 같은 Queue 메시지가 다시 전달되면 Expo에 중복 발송하지 않고 누락된 Receipt 예약만 복구한다.

## dev 테스트 API

dev에서는 인증 사용자가 아래 API로 자기 기기에 일반 테스트 알림을 발행한다.

```http
POST /api/v1/internal/test/push
Authorization: Bearer {accessToken}
```

이 API는 Expo를 직접 호출하지 않고 `PUSH_SEND`를 Queue에 발행한다. `landit.notification.test-api-enabled=true`일 때만 생성되며 prod에는 활성화하지 않는다.

IaC는 dev ECS에 테스트 API 활성화 값을 적용했고 prod에는 주입하지 않았다. 실제 API 사용은 이번 BE 브랜치가 병합·dev 배포된 뒤 가능하다.

## 후속 정책

- 운영 코드에서 생성되지 않는 `review_item READY` 조건과 전용 조회 코드는 제거한다.
- `user_notification_state`는 알림 정책이 확정된 뒤 별도 이슈와 PR로 추가한다.
- IaC의 dev·prod Scheduler는 계속 `DISABLED`로 유지한다. 제품 정책과 Scheduler 메시지 계약을 구현하기 전에는 활성화하지 않는다.
- IaC는 Queue, DLQ, API Task Role, 환경 변수와 Alarm을 이미 관리한다. visibility timeout은 300초, `maxReceiveCount`는 3, DLQ retention은 14일이다.
