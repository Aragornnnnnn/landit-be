# LAN-462 구현 진행 기록

## 기준과 범위

2026-09-07 사용자가 설계를 바탕으로 구현 시작을 승인했다. 기준은 [design.md](design.md)이며 운영 데이터 변경과 실제 운영 발송은 제외한다.

## 구현 순서

1. 캠페인·실행·대상 저장과 API 멱등성을 구현한다.
2. 확정 토큰 발송, 결과 불명·429 정책, DB 기반 SQS 작업 복구를 연결한다.
3. Receipt 확인의 지속성과 집계·운영 핸드오프를 구현한다.
4. API·장애·동시성 테스트, PostgreSQL 검증, Gradle check, 독립 리뷰를 수행한다.

API·DTO·입력 검증 파일만 독립 에이전트가 담당하고 DB·공통 발송 변경은 주 에이전트가 담당한다. Gradle 전체 검증은 한 번에 하나만 실행한다.

## 발견 사항과 구현 결정

- H2와 PostgreSQL에서 동일한 유일성 계약을 검증하기 위해 전체 실행은 nullable `broadcast_campaign_id` 유일 컬럼과 CHECK 제약으로 캠페인당 하나를 보장한다.
- 인스턴스 전체 발송 제한과 짧은 API 멱등성 직렬화를 위한 단일 행 게이트 테이블을 추가한다. 원본 설계의 공용 DB 선점 저장 위치를 구체화한 것이다.

- 기존 Receipt의 15분·3회 정책과 결과 변환을 유지하면서 공지는 DB 작업으로 최대 100개 Receipt를 한 HTTP 요청에서 확인한다. 별도 Receipt 발행 유실 구간을 줄이고 대량 조회를 처리하기 위한 구체화다.
- 기존 알림의 최초 요청 시각·키·재시도 정책은 유지한다.
- 마이그레이션은 PostgreSQL 전용 이력의 V81까지 확인해 V82로 배정했다.

## 검증 결과

- API/DTO/입력 검증, DB 작업 저장·SQS 발행, 확정 대상 발송·복구, Receipt 배치와 결과 집계를 구현했다.
- 독립 리뷰의 Receipt 회차 fencing, 제출 후 cooldown, 최초 대상 cursor, 늦은 Ticket 복구 지적을 수정하고 회귀 테스트를 추가했다. 재리뷰에서 남은 차단 결함은 발견되지 않았다.
- H2와 로컬 PostgreSQL의 LAN-462 테스트 89개가 통과했다. PostgreSQL 4개는 동시 요청·유일 제약, 10,002개 대상 스냅샷·인덱스 커서 조회, 롤백·lease, 인스턴스 공용 제출 제한을 검증했다.
- `LANDIT_ADMIN_PUSH_PG_TEST=true ./gradlew spotlessApply check --console=plain`: 성공. 전체 981개 테스트, 실패 0개, 오류 0개, 건너뜀 0개. Spotless·Checkstyle 통과.
- Expo Receipt HTTP 배치·429 분류·SQS handler 및 기존 예약·편지함 회귀도 포함한다. 문서 링크·코드 블록과 `git diff --check`를 확인했다.
- 실제 FE/기기, 운영 DB·SQS·부하·배포는 미검증이다. 임시 PostgreSQL은 검증 종료 후 중지한다.
