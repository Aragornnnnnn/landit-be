# 정기 푸시 발송의 건별 DB·SQS 요청을 배치 처리로 개선

## 요구사항

- 정기 푸시 발송 준비·Ticket 기록·Receipt 예약의 반복 왕복을 줄인다.
- 500명 조회, Expo 최대 100건 발송, 중복 선점 방지와 토큰 소유자 확인은 유지한다.
- 캐시, 새 인프라, FE, 운영 DB 수정·배포·실제 알림 전송은 포함하지 않는다.

## 완료 기준

- [x] 접수 이력을 페이지 단위로 조회하고, 발송 준비와 Ticket 결과를 최대 100건씩 DB에서 처리한다.
- [x] 다중 행 SQL로 요청 수를 줄이고, Expo 호출은 트랜잭션 밖에서 수행한다.
- [x] 중복 실행·재시도·토큰 소유자 변경·비활성화 회귀 테스트를 통과한다.
- [x] Ticket 커밋 후 Receipt 예약을 최대 10건씩 요청하며 부분 실패를 감지한다. 재처리 때 이미 접수한 푸시는 다시 보내지 않는다.
- [x] Receipt 지연·최대 확인 횟수와 기존 외부 제공자 오류 정책을 유지한다.
- [x] 준비·Ticket 저장·Receipt 예약 구간 지표를 추가한다. 사용자·토큰·실행 ID를 metric tag에 넣지 않는다.
- [x] PostgreSQL에서 SQL 수와 동시성 검증을 수행하고 `./gradlew check`를 통과한다.
- [x] 블로그의 실측·추산·개선 결과를 구분해 반영한다.

## 작업 메모

- 2026-09-06: 372명·377토큰·Expo 5회, 배치 311.199초.
- 2026-09-07: 380명·384토큰·Expo 6회, 배치 312.228초.
- 정상 최초 발송의 dispatch SQL 추산은 `P + U + 6T`, 개선 계획은 `2P + 6B`다. 9월 7일 대상 수로 2,687 → 42회 예상이며 실측 성능은 아니다. 학습 조회·선정 상태 저장·Receipt 후속 처리·트랜잭션 제어는 제외한다.
- 준비: 기존 이력 잠금 → 토큰 잠금 및 소유자 확인 → 중복 재확인 → 다중 행 저장. 잠금 순서를 결정적으로 유지한다.
- Ticket: 묶음 잠금 → 도메인 상태 전이 → 일괄 저장 → 커밋 후 Receipt 예약.
- SQS 부분 실패는 예외로 재처리하고 DB의 접수 이력에서 예약을 복구한다. 이미 성공한 Receipt 예약의 중복은 기존 Receipt 상태 확인으로 안전하게 처리한다.
- 구현 순서: DB 배치 → 발송 연결 → SQS 배치·구간 지표 → 회귀·PostgreSQL 검증 → 독립 리뷰.
- 설계 변경과 검증 결과는 이 문서에만 갱신한다.

## 리뷰 체크

- [x] 구현 완료
- [x] 확인 완료
- [x] 문서 반영

## 검증 결과 · 2026-09-08

- 브랜치 `feat/LAN-468`, 기준 `origin/develop`의 `88ee78c2`.
- `./gradlew test --tests '*NotificationDispatchServiceTest' --tests '*SqsPushQueuePublisherTest' --tests '*PushDeliveryServiceTest'` 통과.
- 전용 로컬 PostgreSQL 15.18에서 `PushDeliveryBatchIntegrationTests` 13개 통과. 테스트용 notification 엔티티 스키마를 생성·삭제하며 운영 Flyway 전체 적용 테스트는 아니다.
- `LAN468_TEST_JDBC_URL=jdbc:postgresql://127.0.0.1:55468/lan468_test ./gradlew test --tests '*PushDeliveryBatchIntegrationTests' --rerun-tasks`로 재현한다. 먼저 로컬 임시 DB와 `lan468_test` 테스트 역할을 준비한다. 다른 주소는 테스트에서 거부한다. 종료 후 임시 PostgreSQL은 중지했다.
- 500명·500토큰·정상 Ticket 접수 조건에서 JDBC 실행 SQL **3,501 → 32회**, 쓰기 커밋 **1,000 → 10회**를 단언으로 확인했다. 기존 단건 Service 경로와 새 dispatch를 비교한다. 준비 단계에서 제외되는 후보나 재시도 표식 갱신이 있으면 이 식과 달라진다.
- 생성 키 매핑, 문구 스냅샷, 묶음 INSERT 실패, 잘못된 Ticket, 외부 트랜잭션 롤백, 동시 신규 선점, 단건 재시도와의 경합, 소유자 변경·BadDeviceToken Receipt와의 경합, SQS 실패 후 복구를 검증했다.
- `./gradlew check`: **905개 테스트, 실패 0, 건너뜀 0**, Spotless·Checkstyle 통과. 최초 Checkstyle 오류 5개는 수정 후 전체 재검증했다.
- 독립 읽기 전용 리뷰 2회에서 확정 결함 없음. `git diff --check` 통과.
- 추가 지표: `landit.notification.dispatch.stage.duration`, `stage=prepare|ticket_persist|receipt_publish`, `outcome=success|failure`. Timer는 Service 호출의 트랜잭션 커밋까지 포함한다.
- DB migration 없음. 기존 단건 API, 오류 시 재시도 표식 기록, Receipt 900초 지연·최대 3회 정책 유지.
- 운영 DB 수정·배포·실제 푸시 전송·Grafana 편집은 하지 않았다. 운영 처리 시간과 API 영향은 배포 후 별도 측정한다.

## 후속 독립 리뷰 반영

- 희소한 재처리 대상에서 DB 후보 100건마다 소수 알림을 즉시 보내면 Expo 요청이 증가하는 P2 회귀를 수정했다.
- DB 후보 버퍼는 100건씩 유지하고, 선점 성공분을 별도 전송 버퍼에 합쳐 Expo에 100건씩 보낸다. 마지막 잔여분도 100건 한도를 지킨다.
- 전송 버퍼가 99건이어도 DB를 1후보씩 조회하지 않는다. DB 선점 결과를 합친 직후 버퍼는 최대 199건이며 전송 후 100건 미만으로 줄어든다.
- 다음 DB 선점이나 Expo·Receipt 예약이 실패하면 아직 Expo에 넘기지 않은 버퍼 이력에만 재시도 표식을 기록한다. 이미 전송을 시도한 묶음은 기존 결과·오류 정책을 유지한다.
- 복구 쓰기가 실패해도 다른 이력의 복구를 계속하며 원예외에 suppressed exception으로 남긴다. DB 장애가 지속되거나 프로세스가 종료되어 복구 쓰기 자체가 불가능하면 기존 REQUESTED 불확정 이력과 마찬가지로 별도 복구가 필요하다.
- 추가 회귀: 500후보·5발송은 Expo 1회, 300후보·150발송은 100+50건, 마지막 잔여 198건은 100+98건, DB 묶음 유지, Receipt 실패의 미전송 잔여분 복구, 다음 선점 실패 복구, 원예외 보존.
- 수정 전 희소 대상·묶음 경계 테스트 2개 실패로 회귀를 재현했다. 최종 `./gradlew test --tests '*NotificationDispatchServiceTest'` 17개 통과, `./gradlew check` 912개 통과(실패·건너뜀 0), Spotless·Checkstyle 통과.
- 최종 독립 재리뷰에서 추가 결함 없음. DB SQL·잠금 구현은 변경하지 않았으며 이번 수정에서는 PostgreSQL을 별도로 재실행하지 않았다. 기존 500토큰 SQL 32회 검증은 전체 검사에 포함된 H2 통합 테스트에서 유지됐다.

## develop 충돌 해결 · 2026-09-11

- `origin/develop`의 `d2727246`을 병합해 기존 7개 기능 커밋을 보존했다. `SqsPushQueuePublisher`와 `NotificationDispatchService`에서 관리자 캠페인 추가분과 정기 발송 배치 추가분을 함께 유지했다.
- 관리자 캠페인의 페이지별 Receipt 복구와 일시적 Expo 오류의 발송 종료 정책을 유지했다. 관리자 Queue payload·지연 시간과 실패 후 중복 발송 방지 회귀 테스트 2개를 추가했다.
- 관련 검사: `./gradlew test --tests '*NotificationDispatchServiceTest' --tests '*SqsPushQueuePublisherTest' --tests '*PushDeliveryServiceTest' --tests '*PushDeliveryBatchIntegrationTests' --tests '*AdminPushCampaignIntegrationTests'` 65개 통과. 배치 DB 검증은 이번 실행에서는 H2를 사용했다.
- 전체 `./gradlew check`는 기본 테스트 JVM의 메모리 부족과 시나리오 테스트 정리 오류로 실패했다. 저장소 설정 변경 없이 임시 Gradle init script에서 `Test.maxHeapSize = '2g'`로 재실행한 결과 1,066개 중 실패 4개·건너뜀 4개, Spotless·Checkstyle 통과였다.
- 남은 4개 실패는 모두 `ScenarioSessionApiIntegrationTests.clearLearningData()`의 `user_level_assessment` → `learning_session` 외래 키 오류다. 원본 `d2727246` 임시 작업 디렉터리에서도 시나리오·구독 테스트 79개 중 같은 오류 3개가 재현됐다. 해당 시나리오 코드·테스트는 develop과 동일하며 이번 충돌 해결에서 수정하지 않았다.
- 충돌 해결 범위의 독립 리뷰에서 추가 결함 없음. 충돌 표식·미해결 인덱스 없음, `git diff --check` 통과.

## 테스트 안정화 · 2026-09-11

- 시나리오 테스트 정리가 비동기 수준 평가의 INSERT와 경합했다. 기존 완료 대기를 `clearLearningData()` 시작으로 옮겨 테스트 시작·종료 양쪽에서 평가 트랜잭션 커밋 이후 데이터를 삭제한다. Clock·Fake 초기화도 대기 이후로 유지한다.
- `build.gradle`의 테스트 JVM 힙 상한을 2GB로 명시해 전체 Spring 통합 테스트 컨텍스트를 보관할 때 발생한 메모리 부족을 해결했다. 운영 JVM 설정과 테스트 선택 범위는 변경하지 않았다.
- `./gradlew test --tests '*ScenarioSessionApiIntegrationTests'`: 수정 전 69개 중 외래 키 오류 3개 재현, 수정 후 69개 모두 통과.
- 추가 실행 옵션 없이 `./gradlew check` 통과: 1,066개 중 성공 1,062개, 실패 0개, 건너뜀 4개. 건너뜀은 전용 PostgreSQL 환경이 필요한 `AdminPushAudiencePostgresTest`이며 기존 조건을 유지했다. Spotless·Checkstyle 통과.
- 독립 리뷰에서 평가 INSERT·완료 상태의 원자적 커밋, 대기 실패 전파와 테스트 JVM 설정 범위를 확인했고 추가 결함 없음. `git diff --check` 통과.
