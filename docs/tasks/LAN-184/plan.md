# LAN-184 구현 및 검증 기록

## 2026-09-24 어드민 배치 개선과 main 발송 경로 점검

이번 작업의 기준은 `origin/main`의 `47c7c103e`와 `origin/develop`의 `06f477579`다.
두 참조의 notification 운영 소스에는 차이가 없었다. 깨끗한 작업 트리에서
`develop` 기준 `feat/LAN-184`를 생성했다. 아래 8월 기록은 당시 정책과 검증 이력이며
현재 운영 상태를 뜻하지 않는다.

### 개선 범위

- 어드민 캠페인의 고정 대상 100개를 `prepareAll()`로 선점한다.
- 정상 Expo 응답은 `recordTicketResults()`로 한 번에 저장하고 커밋한다.
- 현재 페이지의 접수 이력만 조회해 `scheduleReceiptChecks()`로 최대 10개씩 예약한다.
- 캠페인 키, 대상 고정, 토큰 소유권/상태 재확인, 페이지 커서와 별도 SQS 페이지 작업은 유지한다.
- 어드민 Expo 요청의 일시 오류도 종료하는 기존 정책을 유지한다. 정기 푸시의 자동 재시도 경로로 합치지 않는다.
- Ticket 저장 실패로 남은 REQUESTED는 자동 재발송하거나 커서를 넘기지 않는다.
- Receipt 예약 실패 시 저장된 Ticket을 바탕으로 예약만 복구한다. 이미 폐기된 토큰의 접수 이력도 포함한다.
- API, DB 마이그레이션, 운영 데이터, Scheduler 설정, 배포는 변경하지 않는다.
- LAN-557의 미병합 JDBC 저장 방식 전환을 가져오지 않고 현재 develop의 공통 배치 API를 재사용한다.

정상 신규 발송 100토큰의 선점/Ticket 쓰기 트랜잭션은 코드 구조상 200개에서 2개로,
Receipt 예약 요청은 100개에서 10개로 줄어든다. 캠페인 조회와 상태 갱신, 후속 Receipt 처리는
이 계산에서 제외한다. 운영 시간이나 DB CPU의 감소율을 측정한 결과는 아니다.

### main 알림 경로 점검 결과

| 경로 | 현재 처리 방식 | 이번 조치 |
| --- | --- | --- |
| 20시 시나리오/표현/스몰톡 | 500명 조회 후 sendAll, DB/Expo 최대 100건, SQS 예약 최대 10건 | 기존 배치와 재시도 회귀 검증 |
| 표현 복습 | 100명 후보, 사용자별 복습 생성/빈도 슬롯 예약 후 sendAll | 발송은 이미 배치. 사용자별 업무 트랜잭션은 유지 |
| 어드민 즉시/예약 캠페인 | 고정 토큰 100개씩, 선점/Ticket/Receipt 예약은 건별 | 세 작업을 기존 공통 배치 API로 교체 |
| 어드민 본인 테스트 및 dev 테스트 API | send 또는 sendAll을 통한 공통 배치 | 캠페인과 다른 키 및 기존 실패 정책 유지 |
| 편지함 답장 | 답장 커밋 후 SQS, 수신자 목록을 sendAll로 처리 | 이미 배치. 커밋 후 SQS 발행 실패는 로그 기록이며 영속 재발행 장치는 없음 |
| 무료 체험 종료 푸시 | 사용자별 예약 Job에서 sendAll | 다중 토큰은 공통 배치. Job 선점/재시도 계약 유지 |
| 무료 체험 종료/관리자 테스트 이메일 | 별도 Job과 SES 경로 | 접수/재시도/불확실 응답 정책 점검, 변경 없음 |
| Receipt 확인 | 메시지별 단건 조회/저장, 900초 지연, NOT_READY 최대 3회 | 예약 API만 묶음 전환. 확인 자체의 메시지 계약은 유지 |

공통 Push Queue 소비 동시성은 인스턴스당 2이며 캠페인, 정기 알림, Receipt와 Job이 공유한다.
Receipt 적체 시 다른 작업의 대기에도 영향을 줄 수 있으나 실제 적체는 이번에 조회하지 않았다.
Receipt 확인의 일괄화나 Queue 분리, 편지함 발행의 영속 재시도는 별도 설계가 필요해 추가하지 않는다.
Expo 접수 후 결과 유실 및 프로세스 종료의 불확실성을 제거한 것은 아니며 exactly-once를 보장하지 않는다.

### 이번 검증

- 100건과 잔여 1건이 배치 선점/저장/예약 경로를 사용하고 단건 API를 호출하지 않는지 확인한다.
- 동시 실행, 소유권 변경, 폐기/늦게 등록된 토큰 제외, 테스트/공지 키 분리, 요청 실패 정책을 확인한다.
- 성공/실패 Ticket의 원자적 저장과 DeviceNotRegistered 폐기, DB 트랜잭션 밖 Expo/SQS 호출을 확인한다.
- SQS 일부 예약 실패 뒤 10+2건 예약을 다시 실행해도 Expo는 한 번만 호출하는지 확인한다.
- Ticket 저장 실패 시 REQUESTED와 커서를 보존하고 자동 재전송하지 않는지 확인한다.
- 관련 테스트 51개 통과: 어드민 캠페인 통합 24개, 공통 발송 18개, SQS Publisher 9개.
- `./gradlew check` 최종 통과: 229개 클래스, 1,795개 중 1,783개 통과, 실패/오류 0개, 환경 조건에 따른 기존 테스트 12개 제외.
- 첫 전체 검사에서 테스트 변수의 선언/사용 거리 규칙 위반 1건을 확인하고 final로 수정했다. 재검사에서 Spotless와 Checkstyle도 통과했다.
- 제외된 테스트는 관리자 대상 조회 PostgreSQL 4개, 구독 FE 연동 1개, 실제 AI 배포 호환성 1개, 스몰톡 컨텍스트 PostgreSQL 3개, 한국어 퀴즈 PostgreSQL 3개다.
- 새 캠페인 통합 테스트의 DB는 H2이며 Expo와 SQS 응답은 대역이다. 이번 작업에서 별도 PostgreSQL 실행, 운영 로그/DB 조회, 실제 외부 발송과 성능 측정은 하지 않았다.
- `git diff --check` 통과. 운영 소스 변경은 어드민 Processor와 공통 Dispatch 두 파일이다.

```bash
# Java 21 사용.
./gradlew spotlessApply test --tests '*AdminPushCampaignIntegrationTests' --tests '*NotificationDispatchServiceTest' --tests '*SqsPushQueuePublisherTest' --console=plain
./gradlew check --console=plain
git diff --check
```

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
| `DAILY_SCENARIO_REMINDER` | 오늘 배정 시나리오 미완료 | `/scenario` |
| `CONTINUE_EXPRESSION` | 오늘 완료한 시나리오의 다음 미완료 표현 | `/expressions/scenario/{scenarioId}/{expressionId}` |
| `SMALL_TALK_REMINDER` | 오늘 시나리오·표현 완료 및 스몰톡 사용량 0ms | `/smalltalk` |

- 오늘의 시나리오 완료 여부는 `user_scenario_access.granted_at`의 KST 날짜로 판단한다.
- 표현 완료 여부는 `learning_source = 'SCENARIO'`인 `user_writing_expression_completion`만 반영한다.
- 오늘 시나리오를 결정할 수 없으면 다른 완료 이력으로 추론하지 않고 로그를 남긴 뒤 건너뛴다.
- 시나리오 알림은 시나리오 홈(`/scenario`)으로 보내고, 표현과 스몰톡은 기존 학습 경로를 유지한다.

## 현재 구현 상태

| 영역 | 상태 |
| --- | --- |
| Token 관리 | 기존 `UserPushToken`과 `ACTIVE`·`REVOKED` 상태, Token 소유권 이전 유지 |
| 공개 API | 기존 `PUT /api/v1/me/expo-push-token` 계약 유지 |
| 대상 선정과 `user_notification_state` | 새 정책 구현·테스트 완료 |
| 500명 페이지 처리와 Expo 100건 배치 | 구현·테스트 완료 |
| Ticket·Receipt·Token 무효화·DLQ 계약 | 구현·테스트 완료 |
| dev 테스트 API | 구현 완료 |
| FE 20시 로컬 리마인더 제거·기존 예약 정리 | FE 작업 필요 |
| 서버 푸시 UTM·`Page Viewed` 계측 정렬 | BE·FE 작업 필요 |
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
- [ ] FE에서 `ReminderSync`, 50일 로컬 리마인더 생성과 `SYNC_REMINDERS` 예약 경로를 제거한다.
- [ ] 서버 푸시에 필요한 알림 권한, Expo Token 등록, `data.url` 딥링크 처리는 유지한다.
- [ ] 기존 설치에 남은 `REMINDER_KIND` 예약·표시 알림을 한 번 제거하는 FE 정리 릴리스를 먼저 배포한다.
- [ ] FE 정리 버전의 보급과 로컬 예약 제거를 확인하기 전에는 dev·prod Scheduler를 활성화하지 않는다.
- [ ] BE UTM을 `utm_source=landit`, `utm_medium=push`로 맞추고 FE가 세 예약 알림 campaign을 `Page Viewed` 알림 유입으로 기록하게 한다.
- [ ] 배포 전 Push Queue와 DLQ에 과거 `PUSH_SEND` 메시지가 남아 있지 않은지 확인한다. 새 Handler는 이를 처리하지 않으며, 남은 메시지는 DLQ로 이동한다.
- [ ] dev Scheduler를 활성화하기 전에 인증 사용자·UserPushToken·Queue 소비·Expo 환경 변수를 확인한다.
- [ ] iOS와 Android 실기기에서 dev 테스트 API와 20시 예약 알림을 수신한다.
- [ ] 알림 탭 시 `/scenario`, `/expressions/scenario/{scenarioId}/{expressionId}`, `/smalltalk` 딥링크와 UTM 값이 보존되는지 확인한다.
- [ ] 중복 예약 배치, Expo 일시 오류, Receipt 지연과 Push DLQ 이동을 dev에서 확인한다.
- [ ] dev E2E 이후 prod Scheduler 활성화 계획을 검토한다.
