# LAN-184 구현 및 검증 기록

## 최종 정책 구현

- `feat/LAN-184-scheduled-learning`은 기존 4개 스택 PR 위에서 최종 제품 정책을 구현한다. 기존 PR의 닫기·교체·강제 push는 이 브랜치의 검증과 분할 전까지 수행하지 않는다.
- 시나리오 마지막 완료 시각 `last_cleared_at`, 사용자별 계산 결과 `user_notification_state`, 세 가지 제품 알림 유형을 추가했다.
- `SCHEDULED_NOTIFICATION_BATCH`는 Keyset 500명 단위로 대상 선정 데이터를 조회한다. 각 페이지에서 기기별 `push_delivery`를 선점한 뒤, Expo Push 요청을 최대 100건씩 직접 호출한다. 사용자별 `PUSH_SEND` Queue 메시지는 만들지 않는다.
- Expo의 일시 오류는 선점된 발송 이력에 재시도 표식을 남기고 배치 메시지 재전달로 복구한다. 이미 Ticket을 접수한 이력은 Expo에 다시 보내지 않고 Receipt 확인만 복구한다.
- `PUSH_RECEIPT_CHECK`만 Push Queue에 지연 발행한다.
- 검증 순서는 페이지 단위 다중 사용자 Expo 100건 분할, 재시도, 기존 dev 테스트 API, Queue 계약 정리, 전체 `./gradlew check`다.
- 배포 전 Push Queue와 DLQ에 기존 `PUSH_SEND` 메시지가 남아 있지 않은지 확인한다. 새 Consumer는 이 유형을 처리하지 않으므로 남은 메시지는 DLQ로 이동한다.

## 구현 결과

| 영역 | 결과 |
| --- | --- |
| 설치 상태 | 설치 ID·Token unique, ON/OFF, `ACTIVE`·`INVALID` 상태와 소유권 이전 구현 |
| 공개 API | `PUT /api/v1/me/push-devices/{installationId}` 구현 및 OpenAPI 반영 |
| 발송 | 사용자·알림 내용이 확정된 Queue 메시지를 받아 설치별 이력을 선점하고 Expo 최대 100건씩 Ticket 발송 |
| 신뢰성 | 발송 이력 선점, 재시도 분류, Receipt 확인·무효 Token 처리 구현 |
| Queue | API 내부 Consumer, 동시성 2, 900초 Receipt 지연 메시지 구현 |
| dev 검증 | 인증 사용자에게 일반 테스트 알림을 발행하는 조건부 API 구현 |

## 현재 범위 결정

- 이번 PR은 Push Device, Queue Consumer, Expo 발송, Ticket·Receipt와 멱등성 같은 공통 전달 인프라까지만 책임진다.
- 아직 확정되지 않은 대상 선정, 알림 우선순위, 반복 주기, 문구와 딥링크는 후속 정책 PR에서 결정한다.
- 운영 코드에서 생성되지 않는 `review_item`의 `READY` 상태를 발송 조건으로 사용하지 않는다.
- `user_notification_state`는 정책이 확정된 뒤 후속 PR에서 추가한다. 현재 스키마를 미리 만들지 않는다.
- dev 테스트 API는 로그인한 사용자 ID와 고정 테스트 문구를 `PUSH_SEND` 메시지로 발행해 실제 Queue·Expo·Receipt 경로만 검증한다.

스택 브랜치는 다음 순서다.

1. `feat/LAN-184-push-device`.
2. `feat/LAN-184-expo-delivery`.
3. `feat/LAN-184-push-reliability`.
4. `feat/LAN-184-review-reminder`.

## 확인한 사항

- 네 스택 PR은 GitHub에 Ready 상태로 열려 있다. PR 번호는 #50, #51, #52, #53이다.
- 2026-07-26 현재 변경에서 `./gradlew check`를 실행해 Spotless, Checkstyle, 전체 테스트가 통과했다.
- `git diff --check`가 통과했다.
- CodeRabbit 연결 저장소에 `landit-fe`, `landit-ai`, `landit-iac`를 등록했다.
- dev·prod Push Queue, DLQ, IAM과 Consumer 환경 변수는 적용됐다.
- dev에는 테스트 API 활성화 값이 적용됐고 prod에는 주입되지 않았다.
- dev·prod Scheduler는 모두 `DISABLED`다.

## 배포 및 E2E 남은 작업

- [x] 네 스택 브랜치를 GitHub에 push하고 Ready PR을 생성한다.
- [x] dev 테스트 API 활성화 환경 변수를 적용한다.
- [ ] 변경된 BE 스택 PR을 순서대로 병합하고 dev에 배포한다.
- [ ] Push Device를 등록한 dev 계정으로 테스트 API를 호출해 iOS·Android 실기기 수신을 확인한다.
- [ ] 알림 탭 시 테스트 딥링크 이동과 같은 Queue 메시지의 중복 발송 방지를 확인한다.
- [ ] 잘못된 Queue 메시지의 DLQ 이동과 Ticket 뒤 Receipt 확인을 점검한다.
- [ ] 제품 알림 정책과 `user_notification_state`가 확정되면 별도 이슈와 PR로 대상 선정 로직을 구현한다.
- [ ] 정책 구현과 dev E2E 후 prod Scheduler 활성화 plan을 별도로 검토한다.
