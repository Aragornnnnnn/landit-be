# LAN-184 구현 및 검증 기록

## 2026-08-29 정책 변경 상태

예약 알림 정책은 [design.md](design.md)의 `DAILY_SCENARIO_REMINDER → CONTINUE_EXPRESSION → SMALL_TALK_REMINDER` 우선순위로 구현했다. Scheduler는 새 정책의 dev 검증 전까지 계속 비활성 상태로 둔다.

- 오늘 배정 시나리오와 KST 날짜별 스몰톡 사용량을 500명 페이지 조회에 포함했다.
- 기존 전체 콘텐츠 기반 `CONTINUE_SCENARIO`·`REVIEW_LEARNING` 선정 로직을 제거했다.
- 구현·테스트가 끝났지만 dev·prod Scheduler 활성화와 실기기 수신은 별도 E2E 승인 작업이다.

## 최종 구현 범위

- 기존 `UserPushToken` 등록 API와 `ACTIVE`·`REVOKED` 상태를 유지한다.
- 매일 20시 EventBridge가 `SCHEDULED_NOTIFICATION_BATCH` 한 건을 Push Queue에 발행한다.
- 기존 API 서버의 `PushNotificationConsumer`가 예약 배치와 `PUSH_RECEIPT_CHECK`만 처리한다. 별도 Worker는 만들지 않는다.
- 예약 배치는 활성 사용자를 `userProfileId` Keyset Pagination으로 500명씩 조회한다. 각 페이지에서 학습 후보·발송 가능한 Token을 일괄 조회하고, 사용자별 알림 하나를 선정한다.
- 선정 결과는 `user_notification_state`에 스냅샷으로 저장한다. 실제 발송은 Token별 `push_delivery` 멱등성 이력을 먼저 선점한 뒤, 여러 사용자의 Token을 합쳐 Expo에 최대 100건씩 직접 요청한다.
- 사용자별 `PUSH_SEND` Queue 메시지는 만들지 않는다. `PUSH_RECEIPT_CHECK`만 900초 지연 발행한다.
- 일시적인 Expo 오류는 발송 이력에 재시도 표식을 남기고 예약 배치 SQS 재전달로 복구한다. 이미 Ticket을 접수한 이력은 Expo에 다시 보내지 않고 Receipt 확인만 다시 예약한다.
- dev 테스트 API는 로그인 사용자의 `TEST_NOTIFICATION`을 `NotificationDispatchService`로 직접 요청한다.

## 구현한 학습 알림 정책

| 유형 | 선택 결과 | 딥링크 |
| --- | --- | --- |
| `DAILY_SCENARIO_REMINDER` | 오늘 배정 시나리오 미완료 | `/conversation/scenario/{scenarioId}` |
| `CONTINUE_EXPRESSION` | 오늘 완료한 시나리오의 다음 미완료 표현 | `/expressions/scenario/{scenarioId}/{expressionId}` |
| `SMALL_TALK_REMINDER` | 오늘 시나리오·표현 완료 및 스몰톡 사용량 0ms | `/smalltalk` |

- 오늘의 시나리오 완료 여부는 `user_scenario_access.granted_at`의 KST 날짜로 판단한다.
- 표현 완료 여부는 `learning_source = 'SCENARIO'`인 `user_writing_expression_completion`만 반영한다.
- 오늘 시나리오를 결정할 수 없으면 다른 완료 이력으로 추론하지 않고 로그를 남긴 뒤 건너뛴다.
- 발송 딥링크는 현재 프런트 라우트(`/conversation/scenario`, `/expressions/scenario`, `/smalltalk`)에 맞춘다.

## 현재 구현 상태

| 영역 | 상태 |
| --- | --- |
| Token 관리 | 기존 `UserPushToken`과 `ACTIVE`·`REVOKED` 상태, Token 소유권 이전 유지 |
| 공개 API | 기존 `PUT /api/v1/me/expo-push-token` 계약 유지 |
| 대상 선정과 `user_notification_state` | 새 정책 구현·테스트 완료 |
| 500명 페이지 처리와 Expo 100건 배치 | 구현·테스트 완료 |
| Ticket·Receipt·Token 무효화·DLQ 계약 | 구현·테스트 완료 |
| dev 테스트 API | 구현 완료 |
| dev·prod Scheduler | IaC에서 `DISABLED` 유지 |

현재 구현은 `feat/LAN-184-user-push-token-delivery`부터 `feat/LAN-184-scheduled-learning`까지의 스택 PR로 구성한다. 각 브랜치는 앞선 스택 PR을 기준으로 하며, 최종 병합 전에 스택 순서와 base branch를 다시 확인한다.

스택 브랜치는 다음 순서다.

1. `feat/LAN-184-user-push-token-delivery`.
2. `feat/LAN-184-expo-delivery-history`.
3. `feat/LAN-184-push-queue-reliability`.
4. `feat/LAN-184-push-dispatch`.
5. `feat/LAN-184-scheduled-learning`.

## 검증 결과

- 재구성한 각 로컬 스택에서 `./gradlew check`로 Spotless, Checkstyle, 전체 테스트를 검증한다.
- `git diff --check`가 통과했다.
- 새 선정 단위 테스트, 딥링크 테스트, 날짜·언어·표현 출처를 검증하는 대상 조회 통합 테스트가 통과했다.
- 같은 날짜의 재처리 중 선정 유형이 바뀌는 경우와 발송 가능한 UserPushToken 조건을 테스트했다.
- 오늘 미완료·완료 시나리오, 표현 우선순위, 스몰톡 0ms·부분 사용·한도 소진, 오늘 배정 불가 상태를 테스트했다.
- 다중 Token, 500명 페이지 경계, UserPushToken 일괄 조회, 여러 사용자 Expo 100건 분할, SQS 중복 전달 멱등성을 테스트했다.

## 배포 및 E2E 남은 작업

- [ ] 다섯 스택 브랜치를 GitHub에 push하고 Ready PR을 생성한다.
- [ ] 스택 PR을 순서대로 병합하고 dev에 배포한다.
- [ ] 배포 전 Push Queue와 DLQ에 과거 `PUSH_SEND` 메시지가 남아 있지 않은지 확인한다. 새 Handler는 이를 처리하지 않으며, 남은 메시지는 DLQ로 이동한다.
- [ ] dev Scheduler를 활성화하기 전에 인증 사용자·UserPushToken·Queue 소비·Expo 환경 변수를 확인한다.
- [ ] iOS와 Android 실기기에서 dev 테스트 API와 20시 예약 알림을 수신한다.
- [ ] 알림 탭 시 `/conversation/scenario/{id}`, `/expressions/scenario/{scenarioId}/{expressionId}`, `/smalltalk` 딥링크와 UTM 값이 보존되는지 확인한다.
- [ ] 중복 예약 배치, Expo 일시 오류, Receipt 지연과 Push DLQ 이동을 dev에서 확인한다.
- [ ] dev E2E 이후 prod Scheduler 활성화 계획을 검토한다.
