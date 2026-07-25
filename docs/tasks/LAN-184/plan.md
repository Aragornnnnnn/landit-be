# LAN-184 구현 및 검증 기록

## 구현 결과

| 영역 | 결과 |
| --- | --- |
| 설치 상태 | 설치 ID·Token unique, ON/OFF, `ACTIVE`·`INVALID` 상태와 소유권 이전 구현 |
| 공개 API | `PUT /api/v1/me/push-devices/{installationId}` 구현 및 OpenAPI 반영 |
| 발송 | `READY` 복습 대상 Cursor 페이지와 페이지별 설치 일괄 조회, 설치별 이력 선점, Expo 최대 100건 묶음 Ticket 발송 구현 |
| 신뢰성 | 발송 이력 선점, 재시도 분류, Receipt 확인·무효 Token 처리 구현 |
| Queue | API 내부 Consumer, 동시성 2, 900초 Receipt 지연 메시지 구현 |
| dev 검증 | 조건부 수동 복습 리마인더 테스트 API 구현 |

스택 브랜치는 다음 순서다.

1. `feat/LAN-184-push-device`.
2. `feat/LAN-184-expo-delivery`.
3. `feat/LAN-184-push-reliability`.
4. `feat/LAN-184-review-reminder`.

## 확인한 사항

- 각 브랜치에서 도메인·Repository·Service·Consumer·Expo 연동·멱등성 테스트를 실행했다.
- 최신 `origin/develop` rebase 뒤 `./gradlew check`를 다시 실행해 Spotless, Checkstyle, 전체 테스트가 통과했다.
- `git diff --check origin/develop..feat/LAN-184-review-reminder`가 통과했다.
- CodeRabbit 연결 저장소에 `landit-fe`, `landit-ai`, `landit-iac`를 등록했다.

## 배포 및 E2E 남은 작업

- [ ] 네 스택 브랜치를 GitHub에 push하고 순서대로 PR을 생성한다.
- [ ] dev BE와 dev 테스트 API 활성화 환경 변수를 배포한다.
- [ ] `READY` 복습 항목이 있는 dev 계정으로 iOS·Android 실기기 수신을 확인한다.
- [ ] 알림 탭 시 `/expressions` 딥링크 이동과 같은 날짜의 중복 발송 방지를 확인한다.
- [ ] 잘못된 Queue 메시지의 DLQ 이동과 Ticket 뒤 Receipt 확인을 점검한다.
- [ ] dev E2E 후 prod Scheduler 활성화 plan을 별도로 검토한다.
