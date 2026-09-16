# LAN-505 무료 체험 종료 알림

## 확정 범위

- 연간 상품 무료 체험 종료 24시간 전에 EventBridge 일회성 예약으로 푸시와 이메일을 발송한다.
- 이메일은 AWS SES와 `Landit <no-reply@landit.im>`을 사용한다. Reply-To는 생략하고 발신 전용임을 안내한다.
- 푸시 동의와 이메일 발송을 분리하며, 두 채널은 운영자가 독립적으로 켜고 끈다. 기본값은 OFF다.
- 발송 직전 구독 상태, 만료 시각, 사용자 상태와 채널 설정을 재확인한다.
- 개발 서버에는 수신자 허용 목록을 두지 않는다. 관리자 전용 임의 수신 주소 테스트 기능을 제공한다.
- 개발 SES와 서버 권한·설정을 준비한다. 운영 환경 활성화와 운영 사용자 발송은 범위에 포함하지 않는다.

## 구현 결정

- 기존 BE Push SQS 소비자가 알림을 처리한다. AI 저장소에 새 Worker를 추가하지 않는다.
- 웹훅 트랜잭션에서 예약 의도를 DB에 저장하고, 예약 발행 작업이 미등록 건을 재시도한다. 사용자 전체를 주기적으로 조회해 발송하는 방식이 아니다.
- 채널마다 발송 작업과 이력을 분리한다. 웹훅·SQS 재전송은 같은 키로 처리한다.
- SES 응답이 불확실한 경우 자동 재발송하지 않고 UNKNOWN으로 남긴다. SES 접수와 실제 수신은 별도 증거다.
- 상품 ID는 설정한 연간 상품 목록과 정확히 비교한다. 개발에서만 SANDBOX 예약을 허용한다.
- 예정 시각을 지나 도착한 작업은 허용 지연 범위 안에서만 처리하고, 체험이 끝난 알림은 보내지 않는다.
- 런타임 ON/OFF는 DB의 `trial_reminder_settings.push_enabled`, `email_enabled`로만 관리한다. 관리자 API 변경을 예약 등록과 발송 직전에 확인하며 재시작은 필요 없다. 별도 이메일 허용·체험 예약 허용 환경 변수는 사용하지 않는다.
- 채널 OFF 중에도 웹훅의 발송 의도는 저장하지만 외부 예약은 등록하지 않는다. ON으로 바꾸면 남은 PENDING 작업을 예약하며, 발송 시점의 구독 상태와 허용 지연을 다시 검사한다. 이미 SKIPPED 처리된 작업은 재활성화하지 않는다.
- 관리자 테스트 이메일은 체험 채널 ON/OFF와 독립적이다. 기존 소비자 구동 설정과 발신 주소 등 전송에 필요한 설정은 유지한다.

## 확인 및 진행

- 기존 웹훅에 TRIAL·상품·만료 시각이 저장되고, EventBridge/SQS/Expo 기반이 존재한다.
- 서울 리전 SES는 샌드박스이며 운영 발송 승인은 아직 없다. 현재 한도는 하루 200통, 초당 1통이다.
- 구현 및 로컬 검증과 SES 기반 적용을 완료했다. 상세 결과와 남은 단계는 아래에 기록한다.

## 관리자 API와 사용 방법

기존 관리자 인증(`ADMIN` 프로필 + Bearer 토큰)을 그대로 사용한다. UI는 이 저장소 범위에 없으며 Swagger에서 입력·호출할 수 있는 API를 제공한다.

| Method | Path | 용도 |
| --- | --- | --- |
| POST | `/api/v1/admin/notifications/email-tests` | 임의 주소로 고정된 테스트 이메일 접수. `Idempotency-Key` UUID 헤더와 `{"recipient":"tester@example.com"}` 본문을 사용한다. |
| GET | `/api/v1/admin/notifications/jobs/{id}` | 접수 응답의 작업 ID로 현재 처리 상태를 확인한다. |
| GET | `/api/v1/admin/notifications/trial-reminder-settings` | 푸시·이메일 ON/OFF 조회. |
| PUT | `/api/v1/admin/notifications/trial-reminder-settings` | `{"pushEnabled":true,"emailEnabled":true}`로 독립 변경한다. |

- 테스트 API의 202는 DB 접수 완료다. 같은 관리자와 같은 요청 키는 동일 작업을 반환하고, 수신 주소가 바뀌면 409를 반환한다.
- 이메일 상태 `ACCEPTED`는 SES 접수이며 메일함 수신 성공이 아니다. `FAILED`는 명시적 거절, `UNKNOWN`은 응답 또는 결과 기록을 확인할 수 없는 상태다. UNKNOWN을 자동 재발송하지 않는다.
- 작업이 `PROCESSING`인 상태에서 서버가 중단되면 SQS 재시도 시 5분 후 이메일을 UNKNOWN으로 전환한다. 푸시는 기존 토큰별 전달 이력으로 복구한다.
- 429 응답은 SQS 재시도로 처리한다. 채널 작업이 나뉘므로 이메일 재시도가 푸시 중복 발송을 만들지 않는다. DLQ로 이동한 작업은 자동 성공 처리하지 않는다.
- 예정 시각 이후 2시간이 지나거나 체험이 종료된 자동 알림은 제외한다. 관리자 테스트는 접수 후 1시간 이내만 발송한다.
- 채널 OFF 상태로 이미 제외된 작업은 ON으로 바꿔도 다시 보내지 않는다. 이미 외부 제공자가 접수한 메시지는 OFF로 취소할 수 없다.

## 배포 설정

| 설정 | 기본값 / 의미 |
| --- | --- |
| `LANDIT_EMAIL_FROM` | `Landit <no-reply@landit.im>`. |
| `LANDIT_EMAIL_CONFIGURATION_SET` | 개발은 `develop-landit-transactional`. |
| `LANDIT_TRIAL_REMINDER_ANNUAL_PRODUCT_IDS` | 기본 빈 목록. 실제 연간 상품 ID를 쉼표로 구분한다. |
| `LANDIT_TRIAL_REMINDER_LEAD_TIME` | `24h`. |
| `LANDIT_TRIAL_REMINDER_MAX_LATENESS` | `2h`. lead-time보다 작게 설정한다. |
| `LANDIT_TRIAL_REMINDER_SANDBOX_ENABLED` | `false`. 개발에서만 true로 설정한다. |

- 기존 `LANDIT_NOTIFICATION_CONSUMER_ENABLED`, Push Queue URL 및 Scheduler 그룹·역할·Queue ARN·DLQ ARN 설정을 함께 사용한다.
- 예약 의도를 30초 간격으로 발행하며 EventBridge 일회성 예약이 지정 시각에 SQS로 전달한다. 정확한 초 단위 수신을 보장하지 않으며 큐·제공자 지연이 더해진다.
- 실제 연간 상품 목록이 설정된 이후 반영되는 웹훅부터 예약한다. 기존 체험 사용자의 일괄 소급 예약은 추가하지 않았다.
- 짧은 스토어 SANDBOX 체험을 종단 검증할 때는 개발에서만 lead-time과 max-lateness를 함께 줄인다. 단순 이메일 테스트는 체험 구독 없이 사용할 수 있다.
- 운영 적용 전에는 운영 서버에도 SES SendEmail 및 `notification-job-*` Scheduler 권한을 추가하고, SES 인증·발송 한도와 Apple 비공개 릴레이 발신 도메인 등록을 확인해야 한다.

## 검증 결과와 남은 단계

- `origin/main`의 `0e8dd221`에서 `hotfix/LAN-505`를 생성했다. 기준 브랜치의 다음 Flyway 버전인 V105를 사용한다.
- `./gradlew check` 성공: 1,214개 테스트 중 실패 0개, 건너뜀 6개. 새 테스트 15개를 포함하며 Spotless·Checkstyle도 통과했다.
- 새 검증 범위: 웹훅과 예약 저장, 중복 이벤트, 푸시 미동의와 이메일 독립성, 구독 취소, 채널 OFF, 지연 제외, SES 재시도/UNKNOWN, 관리자 인증·주소 검증·멱등 요청, UTC 예약과 큐 payload, SES 발신 설정.
- 개발 IaC는 별도 `landit-iac-LAN-505` 저장소의 `feat/LAN-505`에 준비했다. SES identity·configuration set·지표·개발 EC2 권한 4개 추가를 적용했고 실제 AWS 설정을 읽어 확인했다.
- Vercel에 DKIM CNAME 3개를 등록했고 SES 도메인·DKIM 인증 SUCCESS와 발신 가능 상태를 확인했다.
- 2026-09-16 사용자 요청으로 서울 리전(`ap-northeast-2`) SES 샌드박스 해제를 신청했다. 용도는 TRANSACTIONAL이며 서비스 URL, 무료 체험 종료·결제 예정 안내, 중복 방지, 반송·스팸 신고 suppression 및 CloudWatch 지표, 실제 수신 검증 결과를 제출했다. `get-account` 재조회에서 `Details.ReviewDetails.Status=PENDING`, `ProductionAccessEnabled=false`를 확인했다. 신청 접수 상태이며 승인 전에는 샌드박스 제한이 유지된다. 자동 알림 활성화는 별도다.
- 인증된 테스트 수신 주소로 SES API를 직접 호출해 테스트 메일 1통을 발송했다. SES 접수에 이어 사용자가 네이버 메일함 수신을 화면과 함께 확인했다. 발신 표시는 `Landit <no-reply@landit.im>`, 제목은 `[Landit] 이메일 발송 테스트`였다.
- 로컬 서버의 실제 관리자 HTTP API를 호출해 202 접수 → DB 작업 → 전용 AWS SQS → 실제 BE 소비자 → SES ACCEPTED를 확인했다. 외부 발송 모의 객체 없이 로컬 H2 DB와 실제 AWS SQS·SES를 사용했다. 같은 Idempotency-Key로 두 번 호출해 동일 작업 ID와 DB 작업 1건을 확인했다.
- 사용자가 관리자 API로 발송한 메일의 실제 네이버 메일함 수신 화면을 제공했다. 2026-09-16 20:26 KST 수신 시각, 발신 주소, 제목 및 관리자 테스트 본문을 확인했다. 로컬 관리자 HTTP API부터 실제 메일함 수신까지의 검증을 완료했다.
- 검증 후 로컬 서버를 종료하고 대기·처리 중 메시지 수가 모두 0인 전용 임시 SQS 큐를 삭제했다. 개발 DB와 개발 큐는 사용하지 않았다.
- 브라우저에서 멱등 요청 헤더를 보낼 수 있도록 CORS에 Idempotency-Key를 허용하고 관리자 이메일 API preflight 회귀 테스트를 추가했다.
- BE 개발 배포와 runtime-env/SSM 문서 반영, 기기 푸시 수신은 아직 검증하지 않았다. 자동 채널은 기본 OFF로 유지된다.

## DB ON/OFF 통일

- 사용자 결정에 따라 중복된 이메일 발송·체험 예약 환경 변수 스위치를 제거했다. 기존 DB 테이블과 기본 OFF 값은 유지하며 추가 스키마 변경은 없다.
- 관리자 채널 활성화에는 소비자 구동과 연간 상품 ID가 필요하고 이메일에는 발신 주소가 필요하다. 이 항목들은 전송을 위한 구성 검증이며 별도 체험 채널 ON/OFF가 아니다.
- 개발 IaC runtime-env에서도 제거한 스위치를 삭제했다. 실제 서버 배포와 DB 채널 활성화는 별도다.
- `./gradlew spotlessApply check --no-daemon` 성공: 1,214개 테스트, 실패·오류 0개, 건너뜀 6개. 관리자 API의 즉시 예약 채널 변경, 예약 후 이메일 OFF와 푸시 독립성, 푸시 OFF와 이메일 독립성, 두 채널 OFF에서도 관리자 테스트 발송을 검증했다.
