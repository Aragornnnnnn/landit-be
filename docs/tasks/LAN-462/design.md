# LAN-462 어드민 푸시 캠페인 설계

## 목표

관리자가 제목, 내용, 딥 링크를 가진 캠페인을 만들고 본인 테스트 후 전체 또는 선택 사용자의 활성 Expo Push Token에 즉시 또는 한국 시간 예약으로 비동기 발송한다. 선택 사용자는 개별 클릭, ID 붙여넣기, 읽기 SQL을 조합한다. 캠페인 생성 후 원문과 대상 조건은 수정하지 않는다.

## API

- `POST /api/v1/admin/push-campaigns`: 캠페인 생성.
- `GET /api/v1/admin/push-campaigns`: 목록 조회.
- `GET /api/v1/admin/push-campaigns/{campaignId}`: 원문, 상태, 집계 조회.
- `GET /api/v1/admin/push-campaigns/{campaignId}/audience-preview`: 현재 예상 사용자·Token 수 조회.
- `POST /api/v1/admin/push-campaigns/{campaignId}/test`: 관리자 본인 테스트를 SQS에 발행.
- `POST /api/v1/admin/push-campaigns/{campaignId}/send`: 저장된 대상 조건의 발송을 SQS에 발행.

- `POST /api/v1/admin/push-campaigns/audience-query`: `{"sql":"SELECT ..."}` → 중복 없는 ID 배열. 발송은 하지 않는다.
- `POST /api/v1/admin/push-campaigns/{campaignId}/schedule`: `{"scheduledAt":"2026-09-10T19:00:00+09:00"}`. `Idempotency-Key` 필수.
- `POST /api/v1/admin/push-campaigns/{campaignId}/cancel-schedule`: 시작 전 예약 취소.

모든 API는 기존 `/api/v1/admin/**` 권한 검사를 사용한다. 생성·테스트·발송·SQL 미리보기·예약·취소는 기존 관리자 감사 로그에 기록한다. 감사 로그에 SQL 원문이나 DB 자격 증명을 넣지 않는다.

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
- `SELECTED`는 `userProfileIds` 또는 `audienceSql`을 받는다. 캠페인 1,000명 제한은 없으며 DB 조회·저장은 내부에서 1,000개씩 처리한다.
- 대상은 `(SQL 결과 ∪ userProfileIds) − excludedUserProfileIds`다. 제외가 우선한다. 수동 입력 ID는 양수·실재 여부를 검증하고 중복 제거·정렬한다. SQL 결과에 없는 사용자나 비활성 사용자는 실제 대상에서 제외된다.
- 개별 클릭과 쉼표·공백·줄바꿈으로 붙여넣은 ID는 어드민 UI에서 같은 `userProfileIds` 배열로 보낸다. 이 저장소는 BE API만 제공한다.
- SQL 결과를 고정하려면 미리보기 ID를 `userProfileIds`에 넣고 SQL을 생략한다. 발송 시 재조회하려면 `audienceSql`을 저장한다.
- SQL이 없는 선택에서 모든 ID를 제외하면 발송 대상은 0명이다. 빈 결과를 ALL로 대체하지 않는다.
- 같은 생성 요청 키에서 대상 유형이나 사용자 집합이 달라지면 충돌이다. 순서·중복만 달라진 목록은 같은 요청으로 처리한다.
- 선택 사용자가 비활성이거나 활성 Token이 없으면 실제 대상에 포함하지 않는다. 대상 수는 입력 ID 수가 아닌 발송 가능한 사용자·Token 수다.
- 생성·목록·상세 응답에 `audienceType`, 정규화된 `userProfileIds`, `audienceSql`, `excludedUserProfileIds`, UTC `scheduledAt`을 반환한다. 본인 테스트는 이 목록과 관계없이 인증 관리자에게 발송한다.

```json
{"title":"공지","body":"내용","deepLink":"/home","audienceType":"SELECTED","userProfileIds":[123,456]}
```

## 발송 흐름

1. SQL 없는 즉시 발송은 API 요청에서 대상을 고정한다. SQL 즉시 발송은 `PENDING`을 저장한 뒤 SQS Worker가 SQL을 실행해 대상을 고정한다. 예약도 예약 시각의 Worker에서 조회·고정한다. 조회 실패 시 대상 일부나 ALL로 발송하지 않는다.
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

`admin_push_campaign`에는 불변 원문, 생성 멱등성 키, `DRAFT/PENDING/SCHEDULE_PENDING/SCHEDULED/CANCELLED/QUEUED/SENDING/COMPLETED` 상태, 대상 수와 처리 커서를 저장한다. `admin_push_target`에는 발송 시작 시 고정한 사용자와 Token ID만 저장한다. 결과 수는 기존 `push_delivery`에서 캠페인 키 접두어로 집계한다.

V83에서 `audience_type`과 `admin_push_campaign_user(campaign_id, user_profile_id)`를 추가한다. 캠페인과 선택 목록은 한 트랜잭션에 저장한다. 기존 캠페인은 ALL로 유지하며 기존 생성 요청 해시도 호환된다.

- 성공: Receipt가 `DELIVERED`인 Token 수.
- 실패: Ticket 또는 Receipt가 `FAILED`인 Token 수.
- 대기: `REQUESTED` 또는 `TICKET_ACCEPTED`인 Token 수.
- 제외: 처리 완료 커서 이하의 대상 수에서 같은 범위의 발송 이력 수를 뺀 값.

`COMPLETED`는 대상 페이지 제출 완료를 뜻하며 실제 기기 표시나 전원 성공을 뜻하지 않는다.

V84에서 `audience_sql`, `scheduled_at`과 선택 목록의 `excluded`를 추가한다. 기존 데이터와 V82/V83 체크섬은 유지한다.

## SQL 조회와 설문 미응답자

사용자가 제공한 `public.survey_responses(user_id PK, email, answers JSONB, created_at TIMESTAMPTZ)`는 같은 BE DB에 있다. `user_id`는 `user_profile.id`와 연결하며, 행 존재를 응답으로 판단한다. 설문 ID·완료 여부 컬럼이 없으므로 여러 설문 회차나 임시 저장을 구분할 수 없다. 이 테이블의 소유권·스키마는 변경하지 않는다.

```sql
SELECT u.id AS user_profile_id
FROM public.user_profile u
WHERE u.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM public.survey_responses s WHERE s.user_id = u.id
  )
```

- `SELECT` 또는 읽기 `WITH`만 지원한다. 결과는 `user_profile_id` 하나의 SMALLINT/INTEGER/BIGINT 컬럼이어야 한다. 데이터 수신 전에 JDBC Describe로 검사한다.
- 세미콜론, SQL 주석, 역슬래시·달러 문자열, 따옴표 식별자, 쓰기 CTE, 임의 함수·UNION은 거부한다. 함수는 `count/min/max/sum/avg/coalesce/nullif/lower/upper/length`만 지원하며 최종 결과는 정수 타입이어야 한다. 일반 JOIN, NOT EXISTS, IN, 조건 비교를 지원한다.
- 별도 읽기 계정의 `READ ONLY` 트랜잭션에서 실행하고 항상 롤백한다. 애플리케이션 DB 연결로 대체하지 않는다. 관리자 권한·문법 검사만으로 쓰기 차단을 보장한다고 가정하지 않는다.
- DB statement timeout 10초, lock timeout 1초, 연결 제한 5초, socket timeout 15초다. 서버 커서를 사용하지 않아 SELECT 실행 전체에 statement timeout을 적용한다.
- 기본 결과 상한 100,000행은 SQL 자원 보호용이며 초과하면 조회 전체가 실패한다. 일부 결과로 캠페인을 진행하지 않는다. 중복 제거 전 행 수에 적용하며 환경 설정으로 조정한다. 수동 SELECTED 캠페인의 1,000명 제한과는 별개다.
- `LANDIT_PUSH_AUDIENCE_DB_URL/USERNAME/PASSWORD`를 API와 소비 환경 모두 설정한다. 같은 BE DB에 별도 로그인 역할을 만들고 필요한 기본 테이블에만 SELECT를 부여한다. superuser/CREATEDB/CREATEROLE/REPLICATION/BYPASSRLS 역할은 서버에서 거부한다. 역할 상속, 쓰기 권한, SECURITY DEFINER 함수·뷰 및 넓은 스키마 권한은 부여하지 않는다.
- 초기 허용 테이블은 `public.user_profile`, `public.survey_responses`다. 추가 SQL 대상 테이블은 읽기 역할의 GRANT로 명시적으로 확장한다. 운영 계정·GRANT 생성은 별도 인프라 적용 작업이다.

## 한국 시간 예약

- 입력은 초 단위 `+09:00` 오프셋이며 최초 요청은 현재보다 1분 이후여야 한다. DB에는 TIMESTAMPTZ로 저장하고 어드민은 `Asia/Seoul`로 표시한다.
- DB에 `SCHEDULE_PENDING`과 불변 예약 시각을 먼저 저장한 뒤 EventBridge Scheduler의 일회성 `at(...)` 예약을 만든다. 예약 이름은 `admin-push-{campaignId}`로 고정한다.
- Scheduler는 `Asia/Seoul`, flexible window OFF, 완료 후 DELETE로 기존 Push SQS에 캠페인 ID를 보낸다. 분 단위 예약이며 정각 초 단위 기기 수신을 보장하지 않는다.
- 등록 성공 후 `SCHEDULED`로 변경한다. 등록 실패·응답 유실은 같은 캠페인·같은 시각의 schedule API로 재시도한다. 그때 이미 예약 시각이 지났다면 SQS에 즉시 발행한다. 시각 변경은 409이며 취소 후 새 캠페인을 만든다.
- 발송 대상은 예약 당시가 아니라 실제 시작 시 고정한다. 그전에 설문을 응답한 사용자는 재조회 결과에서 빠진다. 수동 추가 ID는 SQL과 별도의 명시적 포함이므로 계속 포함된다.
- 취소와 대상 고정은 같은 DB 행의 조건부 갱신으로 경쟁한다. 대상 고정이 먼저 성공하면 취소는 409다. 취소가 먼저면 남은 Scheduler/SQS 작업도 발송하지 않는다. DB 취소 후 AWS 삭제가 실패하면 취소 API를 재호출한다.
- `LANDIT_PUSH_SCHEDULER_GROUP`, `LANDIT_PUSH_SCHEDULER_QUEUE_ARN`, `LANDIT_PUSH_SCHEDULER_ROLE_ARN` 설정이 필요하다. API 역할에는 지정 그룹의 Create/Get/DeleteSchedule과 지정 실행 역할의 PassRole, Scheduler 실행 역할에는 기존 Push SQS의 SendMessage가 필요하다. 환경별 그룹을 사용한다.
- Worker SQL/설정 오류는 SQS 재시도와 기존 DLQ로 처리한다. 상태가 PENDING/예약 대기에 남으면 DLQ·설정을 확인하고 기존 메시지를 재처리한다. 새로운 자동 복구/폴링 시스템은 추가하지 않는다.

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

예약 동작의 외부 계약은 [AWS Scheduler 공식 문서](https://docs.aws.amazon.com/scheduler/latest/UserGuide/schedule-types.html)를 기준으로 한다.
