# LAN-184 구현 및 검증 기록

> **에이전트 구현 요구:** `superpowers:executing-plans`로 아래 코드 리뷰 수정 작업을 테스트 우선으로 실행한다.

**목표:** 신규 Push Device 동시 등록과 Ticket 접수 후 Receipt 메시지 발행 실패를 안전하게 복구한다.

**구조:** Push Device 동기화는 독립 트랜잭션을 한 번 재시도한다. 복습 리마인더 재전달은 기존 `TICKET_ACCEPTED` 이력의 Receipt 메시지만 다시 발행한다.

**기술:** Java 21, Spring Boot 4, Spring Transaction, JPA, JUnit 5, Mockito, Gradle.

## 전역 제약

- 별도 Worker, Lock 테이블, Outbox와 DB 마이그레이션을 추가하지 않는다.
- Expo 발송은 중복하지 않고 Receipt 메시지의 드문 중복은 멱등 처리한다.
- 새 Java 파일은 첫 줄 한국어 역할 주석과 공개 API Javadoc 규칙을 지킨다.
- 각 수정은 실패 테스트 확인 후 최소 구현하고 독립 커밋한다.

---

## 목표

앱 설치 상태를 API 하나로 동기화하고, 기존 API 서버가 Push 전용 SQS를 소비해 복습 리마인더를 Expo로 발송한다. 상세 계약과 흐름은 `design.md`를 기준으로 한다.

## 구현 결과

| 영역 | 구현 내용 | 상태 |
| --- | --- | --- |
| 설치 저장 | `PushDevice`, 설치 ID와 Token unique, `ACTIVE`·`INVALID` 상태 | 완료 |
| 공개 API | `PUT /api/v1/me/push-devices/{installationId}` | 완료 |
| 복습 대상 | 지정 날짜의 `READY` 항목과 활성 사용자 조회 | 완료 |
| Expo | Push Ticket 발송과 Receipt 조회 | 완료 |
| SQS | API 내부 Consumer, 동시성 2, 성공 시 삭제 | 완료 |
| 멱등성 | 날짜·사용자·설치별 발송 이력 선점 | 완료 |
| Receipt | 900초 지연, 최대 3회 확인, 무효 Token 처리 | 완료 |

구현은 `push-device → expo-delivery → push-reliability → review-reminder` 순서의 스택형 PR 네 개로 분리한다. Flyway는 같은 순서로 V25 Push Device, V26 Push Delivery, V27 복습 조회 인덱스를 적용한다.

## 확정 결정

- 별도 Push Worker를 만들지 않고 기존 API 애플리케이션에서 SQS를 소비한다.
- Push Queue는 AI jobs Queue와 분리하고 첫 알림 유형은 `REVIEW_REMINDER`만 구현한다.
- 발송 시각은 `Asia/Seoul` 매일 20시다.
- Expo 네트워크 호출 중에는 DB 트랜잭션을 유지하지 않는다.
- 명시적인 HTTP 429·5xx·timeout만 같은 발송 이력으로 재시도한다.
- 결과를 알 수 없는 `REQUESTED` 이력은 중복 발송을 막기 위해 자동 재발송하지 않는다.

## 자동 검증

`./gradlew check`로 다음을 확인했다.

- Spotless와 Checkstyle.
- 전체 단위·통합 테스트.
- 설치 동기화의 검증, 멱등성, 사용자·Token 소유권 이전.
- 복습 대상 날짜와 사용자 상태 필터.
- Expo 요청 JSON과 Ticket·Receipt 응답 변환.
- IaC Scheduler JSON 역직렬화와 서울 기준 날짜 변환.
- SQS Consumer 동시성 2와 Receipt 메시지 900초 지연.
- 같은 발송 키의 중복 방지와 `DeviceNotRegistered` Token 무효화.
- Consumer 비활성 상태에서 AWS 연결 없는 애플리케이션 시작.

## IaC 상태

IaC 작업 `019f8fd8-2ef1-7243-a11c-63c2bb3f03a4`의 구현과 plan 검증은 완료됐고 apply는 하지 않았다.

- IaC HEAD는 `ccd07b8`이다.
- dev plan은 `8 add, 2 change, 1 destroy`다.
- prod plan은 `12 add, 2 change, 1 destroy`다.
- dev와 prod Scheduler는 최초 `DISABLED`다.
- Queue visibility timeout은 300초, `maxReceiveCount`는 3, DLQ retention은 14일이다.
- prod plan에는 범위 밖 Athena·Glue 4개 생성이 있어 분리 또는 정합화 전에는 apply하지 않는다.

## 남은 검증

- [ ] dev IaC 적용과 BE 배포.
- [ ] 설치 동기화 API를 이용한 실기기 수신.
- [ ] 알림 탭 시 `/expressions` 딥링크 이동.
- [ ] 같은 배치 메시지 재전달 시 중복 발송 방지.
- [ ] 잘못된 메시지의 재시도와 Push DLQ 이동.

## 후속 정책

- 로그아웃 시 기존 사용자 알림을 막으려면 프론트가 로그아웃 전에 같은 API로 `pushEnabled=false`를 동기화한다.
- 프론트가 정의한 나머지 네 알림 유형은 발송 조건과 문구, 딥링크를 확정한 뒤 별도 작업으로 추가한다.

## 코드 리뷰 수정 구현 계획

### Task 1. Push Device 동시 동기화 재시도

**파일**

- 생성: `src/test/java/com/landit/landitbe/feature/notification/service/PushDeviceServiceTest.java`
- 수정: `src/main/java/com/landit/landitbe/feature/notification/service/PushDeviceService.java`

**인터페이스**

- 유지: `PushDeviceSyncResponse synchronize(Long, UUID, PushDeviceSyncRequest)`
- 추가 의존성: `TransactionOperations`
- 동작: 첫 `DataIntegrityViolationException` 또는 `TransientDataAccessException` 발생 시 새 트랜잭션으로 한 번 재실행한다.

- [x] 첫 `saveAndFlush`가 unique 충돌을 던지고 두 번째 실행이 기존 설치를 갱신하는 테스트를 작성한다.
- [x] `./gradlew test --tests '*PushDeviceServiceTest'`를 실행해 현재 구현에서 예외가 그대로 전파되는 실패를 확인한다.
- [x] 아래 구조로 트랜잭션 실행과 1회 재시도를 구현한다.

~~~java
try {
  return executeSynchronization(userProfileId, installationId, request);
} catch (DataIntegrityViolationException | TransientDataAccessException exception) {
  return executeSynchronization(userProfileId, installationId, request);
}
~~~

- [x] 같은 테스트를 다시 실행해 통과를 확인한다.
- [x] `./gradlew test --tests '*PushDevice*'`로 기존 API·도메인 회귀를 확인한다.
- [x] `fix: LAN-184 푸시 설치 동시 동기화 보완`으로 1번 브랜치에 커밋한다.

### Task 2. 스택 브랜치 재배치

- [x] 새 `feat/LAN-184-push-device` 위로 `expo-delivery`, `push-reliability`, `review-reminder`를 순서대로 rebase한다.
- [x] 각 인접 브랜치의 `git diff --stat`과 ancestry를 확인해 PR 범위가 섞이지 않았는지 검증한다.
- [x] 기존 `review-reminder` 설계 커밋이 최상단에 유지되는지 확인한다.

### Task 3. Ticket 접수 이력의 Receipt 재예약

**파일**

- 수정: `src/test/java/com/landit/landitbe/feature/notification/service/PushDeliveryServiceTest.java`
- 수정: `src/test/java/com/landit/landitbe/feature/notification/service/ReviewReminderServiceTest.java`
- 수정: `src/main/java/com/landit/landitbe/feature/notification/service/PushDeliveryService.java`
- 수정: `src/main/java/com/landit/landitbe/feature/notification/service/ReviewReminderService.java`

**인터페이스**

- 추가: `Optional<Long> findAcceptedDeliveryId(String deduplicationKey)`
- 동작: 기존 이력이 `TICKET_ACCEPTED`이면 해당 ID를 반환하고, 호출자는 Expo 발송 없이 `scheduleReceiptCheck(id, 1)`만 실행한다.

- [x] `TICKET_ACCEPTED` 이력 ID 조회 테스트와 재전달 시 Expo 미호출·Receipt 재예약 테스트를 작성한다.
- [x] `./gradlew test --tests '*PushDeliveryServiceTest' --tests '*ReviewReminderServiceTest'`를 실행해 새 계약 부재로 실패하는지 확인한다.
- [x] `PushDeliveryService`에 상태 필터 조회를 추가하고 `ReviewReminderService.sendToDevice`가 이를 먼저 처리하도록 구현한다.
- [x] 같은 테스트를 다시 실행해 통과를 확인한다.
- [x] `./gradlew test --tests '*notification*'`로 알림 기능 회귀를 확인한다.
- [x] `fix: LAN-184 Receipt 예약 재시도 보완`으로 4번 브랜치에 커밋한다.

### Task 4. 최종 검증과 기록

- [x] 네 브랜치 각각에서 `./gradlew check`를 실행한다.
- [x] 코드 리뷰 그래프를 증분 갱신하고 두 수정의 호출자·테스트 연결을 확인한다.
- [x] 구현 결과와 실제 검증 명령을 이 문서에 반영한다.
- [x] 구현하지 않은 인프라·마이그레이션 변경이 없는지 최종 diff를 검토한다.

## 코드 리뷰 수정 검증 결과

- 스택 기준 커밋은 `feat/LAN-184-push-device` `e54dc9f`, `expo-delivery` `7a63eeb`, `push-reliability` `e474ec4`, `review-reminder` `2aa47c4`다. 인접 브랜치는 각각 부모가 선행 브랜치인 상태로 1, 1, 5개의 추가 커밋을 가진다.
- 2026-07-24에 각 브랜치에서 `./gradlew check`를 별도 실행해 모두 성공했다. 실제 경과 시간은 순서대로 12.74초, 12.07초, 13.49초, 12.88초다.
- `code-review-graph update --skip-flows`는 `572e88a`에서 `2aa47c4`로 갱신하며 노드 1,654→1,656개, 엣지 17,240→17,318개가 됐다. `PushDeviceController.synchronize`와 `PushQueueMessageHandler.handle`의 실제 Service 호출은 확인됐다.
- 그래프의 `tests_for PushDeviceService.synchronize`는 0건, `PushDeliveryService.findAcceptedDeliveryId`는 node 없음으로 나왔다. 이는 `PushDeviceServiceTest`, `PushDeliveryServiceTest`, `ReviewReminderServiceTest`가 존재하고 전체 Gradle 검사가 성공한 사실과 별개의 정적 그래프 누락이다. `ReviewReminderService.send` 검색의 이름만 같은 `send` 결과도 호출 근거로 사용하지 않았다.
- 리뷰 수정 커밋 `511347b`, `2aa47c4`에는 인프라 또는 Flyway 마이그레이션 추가가 없다. PR4에 이미 있던 `V27__add_review_reminder_index.sql`은 원래 review-reminder 기능 범위다. `git diff --check e474ec4..2aa47c4`는 출력 없이 통과했다.
- 남은 Git 확인 사항은 `origin/develop`과의 분기다. 문서 커밋 후 기준 이 브랜치는 원격 기준 10개 커밋 앞서고 3개 커밋 뒤에 있으며, 통합 전 최신 base 반영 여부를 별도로 확인해야 한다.

## 전체 스택 리뷰 보완 계획

### Task 5. PR1 트랜잭션과 조회 기반 보완

- [x] 외부 트랜잭션 안에서도 두 동기화 시도가 각각 `REQUIRES_NEW`로 실행되는 테스트를 추가했다.
- [x] `TransientDataAccessException` 재시도와 두 번째 실패 전파를 검증했다.
- [x] V25에 발송 가능 설치 조회용 `(user_profile_id, push_enabled, status)` 인덱스와 스키마 테스트를 추가했다.
- [x] 집중 테스트와 `./gradlew check`를 통과해 PR1에 커밋했다.

### Task 6. PR2 발송 Token 스냅샷 보완

- [x] 발송 이력이 실제 Expo 발송 Token을 보존하는 테스트를 추가했다.
- [x] `PushDelivery`와 V26에 `sent_expo_push_token`을 추가하고 원문을 API·로그에 노출하지 않았다.
- [x] 도메인·Repository·스키마 테스트와 `./gradlew check`를 통과해 PR2에 커밋했다.

### Task 7. PR3 발송 상태와 오류 분류 보완

- [x] 별도 트랜잭션 동시성 테스트로 첫 `prepare`만 재시도 표식을 소비하는지 검증했다.
- [x] 429·5xx·timeout만 재시도 가능 예외로 분리하고 interruption·일반 I/O·파싱 실패는 재시도하지 않게 했다.
- [x] `PushDeviceRepository`의 잠금 조회와 Token 무효화를 `PushDeviceService` 공개 계약으로 이동했다.
- [x] Token 교체·이전 뒤 오래된 `DeviceNotRegistered`가 현재 Token 소유자만 무효화하는 통합 테스트를 추가했다.
- [x] 집중 테스트와 `./gradlew check`를 통과해 PR3에 커밋했다.

### Task 8. PR4 복구와 배치 격리 보완

- [x] 현재 READY·사용자·설치 상태와 무관하게 기준 날짜의 `TICKET_ACCEPTED` 이력을 먼저 조회해 Receipt를 예약한다.
- [x] 첫 설치가 실패해도 뒤 설치를 처리하고 전체 순회 뒤 최초 오류를 전파하는 테스트를 추가했다.
- [x] 재시도 가능 오류만 표식 처리하고, 기존 최초 발송·중복 방지·Receipt 멱등성 회귀와 `./gradlew check`를 통과했다.

### Task 9. 스택 통합과 최종 검증

- [x] 각 책임 브랜치 수정 뒤 후속 브랜치를 순서대로 rebase하고 인접 PR 부모 관계를 확인했다.
- [x] 최신 `origin/develop` `e55ca37` 위로 전체 스택을 재배치했다.
- [x] 네 브랜치 각각에서 `./gradlew check`를 실행했다.
- [x] 새 공개 API의 Javadoc과 새 파일 역할 주석을 점검하고 누락을 보완했다.
- [x] 코드 리뷰 그래프를 갱신하고 상태 전이·Token·Receipt 복구·오류 격리 경로를 직접 검토했다.

### Task 10. 커밋 책임 단위 재구성

- [x] 대형 기능 커밋과 후속 보완을 도메인·스키마·저장소·서비스·API·검증 단위로 재구성했다.
- [x] 각 브랜치의 재구성 전후 최종 트리가 동일함을 `git diff --exit-code`로 확인했다.
- [x] 재구성한 네 브랜치에서 `./gradlew check`와 최종 `git diff --check`를 다시 실행했다.

커밋 재구성 후 스택 기준은 PR1 `a6b82e2`, PR2 `bd50714`, PR3 `fec18c4`, PR4 현재 문서 커밋이다. 네 브랜치의 `./gradlew check`가 모두 통과했고, 재구성 전후 코드 트리는 동일하다. PR4의 차이는 이 커밋 재구성 기록뿐이다.

### Task 11. Dev 수동 복습 리마인더 테스트 API

- [x] `landit.notification.test-api-enabled` 기본값을 `false`로 추가했다.
- [x] `POST /api/v1/internal/test/push/review-reminder`를 조건부 Controller로 추가하고, 인증된 요청이 `REVIEW_REMINDER_BATCH`를 현재 시각으로 Push Queue에 발행하게 했다.
- [x] `PushQueuePublisher`와 `SqsPushQueuePublisher`에 즉시 배치 메시지 발행 계약을 추가했다. Receipt 확인 메시지의 900초 지연 계약은 유지했다.
- [x] 테스트 API가 활성화됐을 때 202 응답과 SQS 메시지 계약을, 인증되지 않은 요청의 401 응답을 테스트했다.
- [x] 테스트 API가 비활성인 기본 설정에서는 Controller가 생성되지 않음을 확인했다.
- [x] 집중 테스트와 `./gradlew check`를 실행했다.
- [ ] IaC에 dev 전용 `LANDIT_NOTIFICATION_TEST_API_ENABLED=true` 주입을 요청하고, prod 미주입 및 plan 결과를 확인한다.
