# LAN-557 푸시 이력 JDBC 배치 전환

## 범위와 기준

- `feat/LAN-557`을 생성하고 `origin/develop`의 `70ec76ec5`로 충돌 없이 리베이스했다. 시작 시 작업 트리는 깨끗했다.
- `PushDeliveryBatchRepository`의 다중 행 INSERT와 CASE UPDATE만 고정 SQL의 `batchUpdate()`로 전환한다.
- IDENTITY, 잠금 순서, 토큰 소유자 검사, 도메인 상태 전이, 트랜잭션과 외부 호출 경계는 유지한다.
- 생성 키는 중복 방지 키로 연결한다. 실행 건수가 각 행당 1인지 확인하고 불명확한 결과나 누락은 예외로 롤백한다.
- DB 마이그레이션, 운영 데이터 수정, 발송 정책 변경과 배포는 범위 밖이다.

## 복습 알림 검토

- `ReviewNotificationService`는 100명씩 후보를 읽고 복습 생성과 빈도 슬롯 예약 후 공통 `NotificationDispatchService.sendAll()`을 호출한다.
- `review:{날짜}:{사용자 ID}` 이벤트를 사용하며 `EXPRESSION_REVIEW`의 `contentVariant`는 NULL이다.
- 공통 이력 저장 전환의 영향을 받는다. NULL 저장, 문구와 딥 링크 유지, 같은 이벤트의 Receipt 재예약과 중복 전송 방지를 검증한다.
- 복습 생성 및 슬롯 예약의 사용자별 트랜잭션은 이번 저장 경로 전환과 별개로 유지한다.

## 구현과 검증

1. 저장소 단위 테스트로 생성 키, 실행 건수, NULL 타입 계약을 고정한다.
2. INSERT와 UPDATE를 별도 논리 단위로 전환한다.
3. 기존 통합 테스트에 복습 발송과 실패 경계 검증을 보강한다.
4. H2와 격리된 로컬 PostgreSQL로 검증한 뒤 `./gradlew check`를 실행한다.
- JDBC 실행 호출 수와 DB 내부 SQL 실행 수는 서로 다른 지표다. 기존 운영 처리 시간 감소율을 이번 전환 성과로 사용하지 않는다.

## 검증 결과

- 변경 전 H2 배치 통합 테스트 13개 통과. 새 저장소 테스트 16개 중 2개가 기존 구현에서 실패하는 것을 확인한 뒤 전환했다.
- 관련 테스트 83개 통과: 저장소 단위 16개, 배치 통합 15개, 발송 Service 16개, Queue Handler 11개, 복습 기능 통합 25개.
- PostgreSQL 15.18에서 배치 통합 15개 통과. 운영 DB에 접근하지 않고 전용 임시 클러스터를 `127.0.0.1:55468`에 생성했고 검증 후 서버를 중지했다.
- 500토큰 계측: 기존 단건 경로 대비 JDBC 실행 호출 3,501회 → 32회, 쓰기 커밋 1,000회 → 10회. 기존 다중 행 SQL 구현과 호출 및 커밋 수는 같다.
- 배치 실행은 10회이며 `addBatch()`로 등록한 INSERT와 UPDATE는 합계 1,000건이다. 이 수치는 서버 내부 SQL 계측이나 운영 지연 개선 수치가 아니다.
- 복습 생성과 빈도 제한은 기존 통합 테스트로 확인했다. 추가 테스트는 실제 복습 알림 Service에서 공통 발송 및 저장 경로를 연결하고, 복습 생성과 빈도 제한은 대역으로 분리했다.
- 복습 알림의 NULL 문구 유형, 제목, 본문, 딥 링크와 날짜별 이벤트 키를 보존한다. Receipt 예약 실패 후 같은 이벤트를 처리해도 Expo는 1회만 호출하고 Receipt 예약만 재시도한다.
- 실제 UPDATE 배치의 일부 행이 없으면 먼저 성공한 행도 롤백되는 것을 H2와 PostgreSQL에서 확인했다.
- 전체 검사 중 새 테스트의 문자열이 Checkstyle 100자 제한을 1자 초과한 것을 발견해 분리했다.
- 수정 후 `./gradlew spotlessApply check` 성공: 226개 테스트 클래스, 1,784개 중 1,772개 통과, 실패 및 오류 0개, 환경 조건에 따른 기존 테스트 12개 제외.
- 제외된 테스트는 별도 환경 변수가 필요한 관리자 대상 조회 PostgreSQL 4개, 구독 FE 연동 1개, 실제 AI 배포 호환성 1개, 스몰톡 컨텍스트 PostgreSQL 3개, 표현 콘텐츠 마이그레이션 PostgreSQL 3개다. 이번 발송 배치 PostgreSQL 검증 15개는 별도로 실행했다.
- `git diff --check` 통과. 실제 운영 소스 변경은 `PushDeliveryBatchRepository` 한 파일이며, Service와 Scheduler 정책 및 마이그레이션은 변경하지 않았다.
- INSERT, UPDATE, 저장소 계약 테스트, 복습 발송 통합 테스트, 문서의 5개 논리 커밋으로 나눴다. 푸시, PR 생성과 배포는 실행하지 않았다.

### 실행 명령

```bash
./gradlew test --tests '*PushDeliveryBatchRepositoryTest' --tests '*PushDeliveryBatchIntegrationTests' --tests '*ExpressionReviewIntegrationTests' --tests '*NotificationDispatchServiceTest' --tests '*PushQueueMessageHandlerTest'
LAN468_TEST_JDBC_URL=jdbc:postgresql://127.0.0.1:55468/lan468_test ./gradlew test --tests '*PushDeliveryBatchIntegrationTests'
./gradlew check
```

- Java 21을 사용했다. `LAN468_TEST_JDBC_URL`은 기존 테스트의 로컬 DB 안전장치 이름을 유지한 것이다. URL, 사용자와 DB 이름은 테스트에 고정돼 있어 다른 DB로 실행할 수 없다.
- 생성 키를 반환하는 오버로드는 [Spring JDBC API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/NamedParameterJdbcTemplate.html)를 확인했다.
