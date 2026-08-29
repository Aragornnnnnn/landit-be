# LAN-184 구현 및 검증 기록

## 2026-08-29 정책 변경 상태

예약 알림 정책은 [design.md](design.md)의 `DAILY_SCENARIO_REMINDER → CONTINUE_EXPRESSION → SMALL_TALK_REMINDER` 우선순위로 변경하기로 결정했다. 아래 구현 기록은 정책 변경 전 브랜치 상태를 설명하며, 새 정책은 아직 코드와 테스트에 반영되지 않았다.

- 현재 `CONTINUE_SCENARIO`, `CONTINUE_EXPRESSION`, `REVIEW_LEARNING` 선정 로직은 교체 대상이다.
- 새 정책을 구현·검증하기 전에는 dev·prod Scheduler를 활성화하지 않는다.
- 구현 후 이 문서의 학습 알림 정책, 현재 구현 상태, 검증 결과를 새 근거로 갱신한다.

## 최종 구현 범위

- 기존 `UserPushToken` 등록 API와 `ACTIVE`·`REVOKED` 상태를 유지한다.
- 매일 20시 EventBridge가 `SCHEDULED_NOTIFICATION_BATCH` 한 건을 Push Queue에 발행한다.
- 기존 API 서버의 `PushNotificationConsumer`가 예약 배치와 `PUSH_RECEIPT_CHECK`만 처리한다. 별도 Worker는 만들지 않는다.
- 예약 배치는 활성 사용자를 `userProfileId` Keyset Pagination으로 500명씩 조회한다. 각 페이지에서 학습 후보·발송 가능한 Token을 일괄 조회하고, 사용자별 알림 하나를 선정한다.
- 선정 결과는 `user_notification_state`에 스냅샷으로 저장한다. 실제 발송은 Token별 `push_delivery` 멱등성 이력을 먼저 선점한 뒤, 여러 사용자의 Token을 합쳐 Expo에 최대 100건씩 직접 요청한다.
- 사용자별 `PUSH_SEND` Queue 메시지는 만들지 않는다. `PUSH_RECEIPT_CHECK`만 900초 지연 발행한다.
- 일시적인 Expo 오류는 발송 이력에 재시도 표식을 남기고 예약 배치 SQS 재전달로 복구한다. 이미 Ticket을 접수한 이력은 Expo에 다시 보내지 않고 Receipt 확인만 다시 예약한다.
- dev 테스트 API는 로그인 사용자의 `TEST_NOTIFICATION`을 `NotificationDispatchService`로 직접 요청한다.

## 현재 코드의 학습 알림 정책 변경 전 기록

| 유형 | 선택 결과 | 딥링크 |
| --- | --- | --- |
| `CONTINUE_SCENARIO` | 접근 가능한 다음 미완료 시나리오 | `/conversation/{scenarioId}` |
| `CONTINUE_EXPRESSION` | 부모 시나리오가 `CLEARED`인 다음 미완료 표현 | `/expressions/{expressionId}` |
| `REVIEW_LEARNING` | 활성 콘텐츠가 있고 이어 하기 후보를 모두 완료한 상태 | `/home` |

- 최근 학습 활동은 실제 시나리오·표현 완료 시각만 사용한다. 조회·시작·알림 탭은 제외한다.
- 최근 완료 유형의 후보를 우선하고, 후보가 없으면 다른 이어 하기 유형으로 대체한다.
- 신규 사용자는 접근 가능한 첫 미완료 시나리오를 선택한다.
- 활성 시나리오와 표현이 모두 없는 경우에는 `REVIEW_LEARNING`도 보내지 않는다.

## 현재 구현 상태

| 영역 | 상태 |
| --- | --- |
| Token 관리 | 기존 `UserPushToken`과 `ACTIVE`·`REVOKED` 상태, Token 소유권 이전 유지 |
| 공개 API | 기존 `PUT /api/v1/me/expo-push-token` 계약 유지 |
| 대상 선정과 `user_notification_state` | 구현·테스트 완료 |
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
- 신규 사용자, 최근 시나리오·표현 완료, fallback, 카테고리별 잠금, 표현 부모 시나리오 조건, 완주자, 콘텐츠 0건을 테스트했다.
- 다중 Token, 500명 페이지 경계, UserPushToken 일괄 조회, 여러 사용자 Expo 100건 분할, SQS 중복 전달 멱등성을 테스트했다.

## 배포 및 E2E 남은 작업

- [ ] 다섯 스택 브랜치를 GitHub에 push하고 Ready PR을 생성한다.
- [ ] 스택 PR을 순서대로 병합하고 dev에 배포한다.
- [ ] 배포 전 Push Queue와 DLQ에 과거 `PUSH_SEND` 메시지가 남아 있지 않은지 확인한다. 새 Handler는 이를 처리하지 않으며, 남은 메시지는 DLQ로 이동한다.
- [ ] dev Scheduler를 활성화하기 전에 인증 사용자·UserPushToken·Queue 소비·Expo 환경 변수를 확인한다.
- [ ] iOS와 Android 실기기에서 dev 테스트 API와 20시 예약 알림을 수신한다.
- [ ] 알림 탭 시 `/conversation/{id}`, `/expressions/{id}`, `/home` 딥링크와 UTM 값이 보존되는지 확인한다.
- [ ] 중복 예약 배치, Expo 일시 오류, Receipt 지연과 Push DLQ 이동을 dev에서 확인한다.
- [ ] dev E2E 이후 prod Scheduler 활성화 계획을 검토한다.
