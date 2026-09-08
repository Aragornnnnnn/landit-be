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

## 사용자 목록 필터 확장 (2026-09-08)

- 기존 관리자 사용자 목록에 활성 여부·저장된 푸시 동의 여부 필터와 권한 상태 응답을 추가했다. 상세 계약은 `design.md`에 기록했다.
- 9가지 필터 조합, 필터 후 페이지 조회, 잘못된 입력, 기존 권한 및 OpenAPI 계약 테스트를 통과했다.
- `./gradlew spotlessApply check` 통과. 독립 리뷰에서 추가 결함 없음. 실제 PostgreSQL 및 어드민 UI 연동은 미검증이다.
- 페이지 번호 표시용 `totalCount`, `totalPages`를 기존 목록 응답에 추가했다. 필터가 적용된 첫·마지막·범위 밖·빈 페이지 테스트 및 `./gradlew check` 통과. 독립 리뷰에서 추가 결함 없음.

## 선택 사용자 발송 확장 (2026-09-08)

- 캠페인 생성에 `ALL`/`SELECTED`와 선택 사용자 목록을 추가했다. V83에서 유형·목록을 저장하고 기존 캠페인은 ALL로 유지한다.
- 예상 대상과 스냅샷 생성에 같은 SQL 조건을 적용한다. 기존 배치 소비 및 Receipt 흐름은 재사용한다.
- 선택 대상 제한, 정규화·멱등성 충돌, 관리자 테스트 독립성, 선택 목록 저장 실패 롤백, 입력·OpenAPI 테스트 통과.
- `./gradlew spotlessApply check` 통과. 독립 리뷰에서 결함 없음. 실제 PostgreSQL 적용·기기·어드민 UI 연동은 미검증이다.
