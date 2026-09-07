# LAN-462 구현 기록

## 구현

- 캠페인 원문과 처리 커서는 `admin_push_campaign`, 요청 시점의 대상 ID는 `admin_push_target`에 저장한다.
- API는 캠페인 또는 관리자 테스트 ID만 SQS에 발행한다.
- Worker는 활성 Token을 최대 100개씩 읽고 기존 `push_delivery`, Expo Ticket, Receipt 흐름을 사용한다.
- 중복 API·SQS 처리는 기존 발송 이력의 유일 키로 막는다.
- 별도 실행·gate·lease·polling·관리자 전용 Receipt 상태 머신은 제거했다.

## 검증 기록

- 대상 범위 고정, 100건 배치, 중복 처리, 테스트·전체 발송 독립성 통합 테스트를 유지한다.
- 입력 URL과 payload 제한 단위 테스트를 유지한다.
- 실제 FE·기기, 운영 DB·SQS·배포·운영 발송은 미검증이다.
