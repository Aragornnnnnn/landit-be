# LAN-494 푸시 복습

## 범위와 기준

- 기준 브랜치는 `feat/LAN-491`(PR #191), 작업 브랜치는 `feat/LAN-494`다.
- BE에서 복습 선정·스냅샷·진행·채점·완료 API와 기존 Push SQS의 별도 복습 배치를 구현한다. FE 화면 및 배포는 별도다.
- 완료한 표현 최대 3개를 사용한다. 3개면 EN 2/KR 1, 2개면 EN 1/KR 1, 1개면 무작위 언어다. LAN-491의 한국어 복수 정답을 보존한다.
- 푸시 URL만 제공하며 목록·임의 생성 API는 제공하지 않는다. API는 로그인 사용자와 복습 소유자를 검증한다. HTTP 요청만으로 OS 알림 클릭을 증명하지는 않는다.
- 유료 제한은 기존 `LANDIT_SUBSCRIPTION_LAUNCHED_AT`과 현재 프리미엄 권한을 사용한다. 발송 대상 선정과 새 시작에서 검사하며, 이미 시작한 복습은 시작부터 24시간 동안 재개한다.

## 구현 기본값

작업 시작 시 제시한 초기 정책이다. 설정으로 조정할 수 있게 한다.

- 복습 알림 간격 3일, 최근 학습·복습·출제 표현 제외 기간 3일.
- 기존 학습 알림과 복습 알림은 별도 이벤트이며 발송 예약을 합산해 하루 최대 2건, 서로 최소 3시간 간격을 둔다. 기기별 중복은 기존 push_delivery가 처리한다.
- 생성 후 7일 안에 시작하며, 시작 후에는 24시간 동안 진행한다. 완료 결과는 이후에도 조회할 수 있다.
- 오래 학습·복습한 순서로 후보를 30개씩 조회하고 각 묶음에서 무작위 선정한다. 유효한 문제가 없는 표현은 건너뛰며, 유효한 표현 3개를 채우거나 후보가 소진될 때까지 다음 묶음을 확인한다.
- 표현과 문제·정답 배열을 스냅샷으로 저장한다. 서버가 토큰 순서·중복 개수를 포함해 채점하며 오답은 큐 뒤로 보낸다. 제출 UUID로 재전송을 멱등 처리한다.

## 구현 및 검증 기록

- [x] 스키마·문제 선정·유료 접근·진행 API.
- [x] Push SQS 배치와 학습 알림 빈도 관리.
- [x] HTTP·시간 경계·구독·동시성·배치 재전송 통합 검증.
- [x] `./gradlew check`, API 문서 및 배포 연결 계약 정리.

2026-09-16 검증 결과:

- `./gradlew spotlessApply check` 통과. JUnit XML 합계 1,246건 중 실패·오류 0건, 건너뜀 9건이다. 복습 통합 테스트 17건과 메시지 라우팅 테스트를 포함한다.
- 별도 로컬 PostgreSQL 15에서 V107 생성, 실제 후보 선정 SQL, 시작 시각·만료 시각 제약, 일자별 복습 및 알림 종류별 유일성, 문제·제출·완료 저장과 프로필 잠금 SQL을 검증했다. 전체 운영 마이그레이션 체인을 재실행한 검증은 아니다. 임시 DB는 검증 후 종료했다.
- 시나리오와 자유 대화에 같은 표현의 이력이 있으면 두 이력의 최신 완료 시각으로 제외한다. 무효 콘텐츠를 건너뛸 때 상위 복습 트랜잭션이 롤백되지 않도록 읽기 서비스의 예외 계약을 명시했다.
- 복습 생성과 알림 슬롯 예약은 같은 사용자 잠금·트랜잭션으로 처리한다. 빈도 제한에 걸리면 새 복습도 롤백하여 다음 기회를 막지 않는다. 같은 이벤트 재시도에도 다른 종류 알림과의 간격을 확인한다.
- 독립 리뷰에서 확인한 두 결함을 수정했다. 같은 날짜의 DAILY 슬롯이 있으면 최초 예약 시각으로 간격을 계산하고, 슬롯이 없는 기존 알림 기록은 계속 확인한다. 앞의 후보가 무효여도 다음 묶음에서 유효한 표현을 찾는다. 재시도·날짜 경계와 무효 후보·묶음 간 문제 수 보충 회귀 사례 6건을 추가했다.
- 로컬 PostgreSQL 15에서 변경한 실제 후보 SQL의 offset 0·30·60 조회로 첫 묶음·다음 묶음·소진을 추가 검증했다.
- 초기 전체 테스트에서 새 복습 데이터가 기존 테스트의 삭제 순서와 충돌했다. 복습 통합 테스트에 전용 H2 DB를 사용한 후 전체 검증이 통과했다.
- 스냅샷은 JDBC 저장소와 값 record로 관리한다. 문제 JSON은 조회 조건에 사용하지 않는 TEXT이며, 원본 표현·기존 학습 완료 이력은 수정하지 않는다. 새 교차 조회의 소유 경계는 `docs/architecture/backend.md`에 반영했다.
- Ponytail 점검 후 표현 API·복습 테스트의 동일한 콘텐츠 생성 코드를 `ExpressionPracticeFixture`로 추출했다. 테스트별 문제 데이터와 검증문은 유지했다.
- 단일 호출자인 문제 선정 Service를 제거하고 `ExpressionReviewService`의 private 메서드로 옮겼다. 두 정리로 Java 코드는 순수 78줄 줄었으며, 각 변경 후 `./gradlew spotlessApply check`가 통과했다. 수정은 테스트 fixture와 운영 코드 정리의 두 커밋으로 나눴다.

## FE 연결 계약

모든 API는 Bearer 인증이 필요하며, 응답은 기존 `ApiResponse.data`에 담긴다. 복습 생성·목록 API는 없다.

| 메서드 | 경로 | 요청 | 성공 응답 |
| --- | --- | --- | --- |
| GET | `/api/v1/reviews/{reviewId}` | 없음 | `ReviewResponse` |
| POST | `/api/v1/reviews/{reviewId}/start` | 본문 없음 | `ReviewResponse` |
| POST | `/api/v1/reviews/{reviewId}/answers` | 아래 JSON | `{ correct, review: ReviewResponse }` |

답안 제출 예시:

```json
{
  "submissionId": "4c0293fc-bfb3-43b1-b75b-43f2a5c06a7d",
  "questionId": "a16f55f0-062d-4c9a-a3b0-1be915c236af",
  "words": ["선택한", "칩", "순서"]
}
```

새 시도마다 `submissionId`를 새로 생성하고, 네트워크 재전송에서는 ID와 내용을 그대로 유지한다. 동일 제출의 재전송은 기존 판정과 현재 진행 상태를 반환한다. 제출은 `currentQuestionId`에만 가능하다.

| `ReviewResponse` 필드 | 의미 |
| --- | --- |
| `reviewId` | 푸시에 포함된 복습 UUID |
| `status` | `READY`, `IN_PROGRESS`, `COMPLETED`, `EXPIRED` |
| `availableUntil` | 최초 시작 기한. 해당 시각부터 시작 불가 |
| `expiresAt` | 시작 후 진행 기한. 시작 전에는 null |
| `completedAt` | 전체 완료 시각. 완료 전에는 null |
| `currentQuestionId` | 현재 풀 문제 ID. 시작 전·완료·만료이면 null |
| `questions` | 최초 출제 순서의 고정 문제. 시작 전·만료이면 빈 배열 |

각 문제에는 `questionId`, `expressionId`, `targetExpressionText`, `baseExpressionMeaningText`, `quiz`, `displayOrder`, `queueOrder`, `wrongCount`, `completedAt`이 있다. `quiz`는 기존 `WritingSentenceResponse`와 같으며 `writingSentenceAcceptedAnswers`의 한국어 복수 정답을 보존한다. 마지막 정답에서 자동 완료되므로 별도 완료 요청은 없다. 완료 화면은 `questions`의 표현과 뜻을 표시한다.

| HTTP 상태 | 조건 |
| --- | --- |
| 400 | UUID·본문 검증 실패 |
| 401 | 인증 실패 |
| 403 | 유료 전환 후 현재 권한 없이 미시작 복습을 조회·시작함 (`PREMIUM_REQUIRED`) |
| 404 | 복습이 없거나 다른 사용자 소유 |
| 409 | 시작 전 제출 (`REVIEW_NOT_STARTED`), 문제 순서 오류 또는 같은 제출 ID의 내용 충돌 |
| 410 | 만료된 복습 시작·새 답안 제출 (`REVIEW_EXPIRED`) |

GET은 만료 상태를 200 응답의 `EXPIRED`로 반환한다. 이미 처리한 제출의 재전송은 만료 후에도 상태 변경 없이 기존 판정을 반환한다. 완료 결과는 구독·진행 기한 만료 후에도 조회한다.

## 설정

| 설정 키 | 기본값 |
| --- | --- |
| `landit.review.interval-days` | 3 |
| `landit.review.exclusion-days` | 3 |
| `landit.review.available-days` | 7 |
| `landit.review.session-hours` | 24 |
| `landit.review.notification-gap-hours` | 3 |

하루는 기존 서비스 Clock의 서울 시간대 기준이다. 알림 빈도는 서버의 발송 예약 기준이며, 기기의 실제 수신·표시 시각을 보장하지 않는다. 유효한 표현이 없거나 발송 가능한 기기가 없으면 복습을 생성하지 않는다.

## 배포 연결

기존 Push SQS 소비자는 BE가 소유한다. AI Worker의 학습 작업 Queue에 복습 처리를 추가하지 않는다. 실제 발송 활성화는 FE 딥링크 화면 배포 후 별도 EventBridge 스케줄을 기존 Push Queue에 연결해야 한다. 이 작업에서는 클라우드 스케줄 생성·운영 발송을 실행하지 않는다.

Scheduler는 하루 한 번 호출하고 사용자별 3일 간격은 BE가 판단한다. 기존 학습 알림과 최소 3시간 떨어진 시각을 선택한다. 설정 간격 안에 다른 학습 알림이 있으면 해당 호출에서는 복습 발송을 건너뛴다.

```json
{
  "version": 1,
  "messageId": "<aws.scheduler.execution-id>",
  "messageType": "REVIEW_NOTIFICATION_BATCH",
  "occurredAt": "<aws.scheduler.scheduled-time>",
  "payload": {}
}
```

`occurredAt`이 처리 시점의 서울 날짜와 다르면 건너뛴다. 같은 날짜 재시도에서는 사용자별 `review:{date}:{userId}` 이벤트 키와 복습 UUID를 재사용하며, 기기별 중복 처리는 기존 `push_delivery` 경로에 맡긴다.

FE는 `/reviews/{reviewId}?utm_source=push&utm_medium=notification&utm_campaign=expression_review`를 처리하고 시작 API를 호출해야 한다. 앱 내 일반 메뉴에는 진입점을 추가하지 않는다. 실제 기기 딥링크·푸시 수신, FE 화면, 클라우드 Scheduler, 배포와 운영 데이터 검증은 아직 수행하지 않았다.

## 2026-09-16 hotfix 마이그레이션 번호 예약

- PR #197의 V105~V107을 위해 선행 알람은 V108, 복수 정답은 V109, 이 PR의 복습은 `V110__add_expression_review.sql`로 변경했다. 세 SQL의 내용은 모두 보존했다. 위 V107 검증은 당시 파일명 기준 기록이다.
- 개발·운영 DB의 Flyway 이력은 모두 V104까지 적용된 상태였다. 적용된 마이그레이션과 이력은 수정하지 않았다.
- #197을 main에 배포한 뒤 develop으로 역병합하여 V105~V107을 포함해야 한다. 이후 #189의 V108 → #191의 V109 → 이 PR의 V110 순으로 적용하며 V108 이상을 먼저 배포하지 않는다.
