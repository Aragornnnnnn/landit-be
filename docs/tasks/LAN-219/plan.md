# LAN-219 Daily Scenario Access Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 한국 시간 기준 오늘의 시나리오만 새로 시작할 수 있고, 새 정책에서 완료한 시나리오는 언제든 복습할 수 있게 한다.

**Architecture:** Content가 날짜별 전역 일정을, Learning이 사용자별 영구 복습 권한을 소유한다. Session은 두 기능의 공개 Service를 사용해 시작 가능 여부를 판단하고, 일일 세션이 정상 완료되면 복습 권한을 생성한다.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, Flyway, PostgreSQL, H2, JUnit 5, MockMvc, Gradle

## 기준 문서

- 상세 정책과 예외는 `docs/tasks/LAN-219/design.md`를 따른다.
- 이 문서는 구현 순서와 단계별 검증 기준만 관리한다.

## 공통 제약

- 날짜 계산에는 주입된 `Clock`과 `Asia/Seoul`을 사용한다.
- 오늘 시나리오는 배정일의 `[00:00, 다음 날 00:00)`에만 새로 시작할 수 있다.
- 배정일 안에 시작한 세션에는 별도 완료 기한을 두지 않는다.
- 기존 `session_history`와 `user_scenario_progress`는 보존하며 접근 권한으로 백필하지 않는다.
- 비활성 콘텐츠는 접근 권한이나 오늘 일정이 있어도 `LOCKED`다.
- 결제, 알림, 관리자 API, 세션 복원, 장기 `IN_PROGRESS` 정리는 구현하지 않는다.
- 테스트를 먼저 실패시킨 뒤 최소 구현으로 통과시킨다.
- 새 Java 파일의 한국어 역할 주석과 공개 API Javadoc 규칙을 지킨다.

## 변경 지점

| 영역 | 주요 파일 | 책임 |
| --- | --- | --- |
| DB | `src/main/resources/db/migration/V26__add_daily_scenario_access.sql` | 일정·접근 권한 테이블과 세션 일정 FK 추가 |
| 시간 | `src/main/java/com/landit/landitbe/config/time/TimeConfiguration.java` | 서울 시간 기준 `Clock` 제공 |
| Content | `feature/content/domain`, `repository`, `service` | 오늘 일정 조회와 만료 시각 계산 |
| Learning | `feature/learning/domain`, `repository`, `service` | 사용자별 복습 권한 조회와 멱등 생성 |
| 목록 | `ScenarioQueryService`, `ScenarioListResponse`, 목록 projection·repository | 전체 시나리오의 상태와 만료 시각 반환 |
| Session | `ScenarioSession`, `ScenarioSessionStartService`, `GeneratedMessageService` | 시작 제한, 일정 연결, 완료 시 권한 생성 |
| 테스트 | 기존 스키마·목록·세션 통합 테스트와 필요한 단위 테스트 | 정책과 회귀 검증 |

---

### Task 1: 일정과 복습 권한 기반을 추가한다

#### 대상 파일

- Create: `src/main/resources/db/migration/V26__add_daily_scenario_access.sql`
- Create: `src/main/java/com/landit/landitbe/config/time/TimeConfiguration.java`
- Create: `src/main/java/com/landit/landitbe/feature/content/domain/DailyScenarioSchedule.java`
- Create: `src/main/java/com/landit/landitbe/feature/content/repository/DailyScenarioScheduleRepository.java`
- Create: `src/main/java/com/landit/landitbe/feature/content/service/DailyScenarioScheduleService.java`
- Create: `src/main/java/com/landit/landitbe/feature/learning/domain/UserScenarioAccess.java`
- Create: `src/main/java/com/landit/landitbe/feature/learning/repository/UserScenarioAccessRepository.java`
- Create: `src/main/java/com/landit/landitbe/feature/learning/service/ScenarioAccessService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/domain/ScenarioSession.java`
- Test: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`
- Test: `src/test/java/com/landit/landitbe/feature/content/service/DailyScenarioScheduleServiceTest.java`
- Test: `src/test/java/com/landit/landitbe/feature/learning/service/ScenarioAccessServiceIntegrationTests.java`

#### 구현 계약

- `daily_scenario_schedule.service_date`는 유일하며 하나의 `scenario_id`를 참조한다.
- `user_scenario_access`는 `(user_profile_id, scenario_id, target_locale)` 조합이 유일하다.
- `scenario_session.daily_scenario_schedule_id`는 nullable 외래 키다.
- 일정 Service는 평가 시각 하나를 받아 서울 기준 오늘 일정과 다음 날 자정을 반환한다.
- 접근 권한 Service는 사용자·시나리오·학습 언어별 보유 여부와 목록을 조회하고, 중복 완료를 성공으로 처리한다.

#### 작업과 검증

- [ ] 최신 Flyway 번호를 다시 확인하고 스키마 실패 테스트를 작성한다.
- [ ] 일정의 자정 경계와 접근 권한 중복 생성 테스트를 작성한다.
- [ ] 대상 테스트가 새 스키마와 Service 부재로 실패하는지 확인한다.
- [ ] 마이그레이션, 엔티티, Repository, Service, `Clock` 설정을 최소 구현한다.
- [ ] `ScenarioSession`에 nullable 일정 ID를 연결한다.
- [ ] 대상 테스트를 통과시킨다.

```bash
./gradlew test \
  --tests 'com.landit.landitbe.DatabaseSchemaIntegrationTests' \
  --tests '*DailyScenarioScheduleServiceTest' \
  --tests '*ScenarioAccessServiceIntegrationTests'
```

완료 후 `일일 시나리오 일정과 접근 권한 기반 추가` 단위로 커밋한다.

### Task 2: 전체 시나리오 목록을 새 상태로 전환한다

#### 대상 파일

- Create: `src/main/java/com/landit/landitbe/feature/content/domain/ScenarioAvailabilityStatus.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/dto/ScenarioListResponse.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/ScenarioListQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/projection/ScenarioListProjection.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ScenarioQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/docs/ScenarioControllerDocs.java`
- Test: `src/test/java/com/landit/landitbe/feature/content/ScenarioListApiIntegrationTests.java`

#### 구현 계약

- 모든 시나리오를 유지하면서 `availabilityStatus`와 nullable `expiresAt`을 반환한다.
- 상태 우선순위는 비활성 `LOCKED` → 접근 권한 `CLEARED` → 오늘 일정 `TODAY` → `LOCKED`다.
- `completed`, `locked`, `openingPreview`, 별점은 새 상태와 일관되게 계산한다.
- 기존 `UserScenarioProgress.status`와 직전 시나리오 완료 여부는 상태 판정에서 제외한다.
- 오늘 일정이 없거나 비활성이면 목록은 정상 반환하고 경고 로그를 남긴다.

#### 작업과 검증

- [ ] `CLEARED`, `TODAY`, `LOCKED`와 기존 필드의 조합을 통합 테스트로 작성한다.
- [ ] 기존 완료 기록 비반영, 일정 누락, 비활성 콘텐츠, `expiresAt`을 테스트한다.
- [ ] 테스트가 기존 순차 잠금 응답 때문에 실패하는지 확인한다.
- [ ] 조회 projection과 조립 로직을 새 정책으로 변경하고 OpenAPI 문서를 갱신한다.
- [ ] 목록 통합 테스트를 통과시킨다.

```bash
./gradlew test --tests 'com.landit.landitbe.feature.content.ScenarioListApiIntegrationTests'
```

완료 후 `시나리오 목록에 일일 접근 상태 적용` 단위로 커밋한다.

### Task 3: 세션 시작을 오늘 일정 또는 복습 권한으로 제한한다

#### 대상 파일

- Modify: `src/main/java/com/landit/landitbe/feature/session/service/ScenarioSessionStartService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/service/ScenarioSessionService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/repository/ScenarioSessionStartQueryRepository.java`
- Delete: `src/main/java/com/landit/landitbe/feature/session/repository/projection/ScenarioSessionLockProjection.java`
- Test: `src/test/java/com/landit/landitbe/feature/session/ScenarioSessionApiIntegrationTests.java`
- Test: `src/test/java/com/landit/landitbe/feature/session/service/ScenarioSessionServiceTest.java`

#### 구현 계약

- 접근 권한이 있으면 날짜와 무관한 복습 세션을 만들고 일정 ID는 저장하지 않는다.
- 접근 권한이 없으면 요청 시각의 오늘 일정과 일치할 때만 세션을 만들고 일정 ID를 저장한다.
- 잠긴 시나리오는 기존 `SCENARIO_LOCKED` 오류로 거절한다.
- 같은 날 여러 번 시작할 수 있고, 기존 또는 전날의 `IN_PROGRESS` 세션은 시작을 막지 않는다.
- 직전 시나리오 완료 기반 조회와 `PREVIOUS_SCENARIO_NOT_COMPLETED` 검증을 제거한다.

#### 작업과 검증

- [ ] 00시, 23시 59분 59초, 다음 날 00시의 시작 경계 테스트를 작성한다.
- [ ] 복습 시작, 잠긴 시나리오, 반복 시작, 전날 진행 중 세션 테스트를 작성한다.
- [ ] 테스트가 기존 순차 잠금과 일정 미검증 때문에 실패하는지 확인한다.
- [ ] 시작 검증과 세션 일정 연결을 구현하고 불필요해진 이전 잠금 조회를 제거한다.
- [ ] 세션 시작 관련 테스트를 통과시킨다.

```bash
./gradlew test \
  --tests 'com.landit.landitbe.feature.session.ScenarioSessionApiIntegrationTests' \
  --tests 'com.landit.landitbe.feature.session.service.ScenarioSessionServiceTest'
```

완료 후 `오늘 일정과 복습 권한으로 세션 시작 제한` 단위로 커밋한다.

### Task 4: 정상 완료 시 복습 권한을 부여한다

#### 대상 파일

- Modify: `src/main/java/com/landit/landitbe/feature/session/service/GeneratedMessageService.java`
- Test: `src/test/java/com/landit/landitbe/feature/session/ScenarioSessionApiIntegrationTests.java`

#### 구현 계약

- AI 응답 처리로 세션이 `COMPLETED`가 되는 트랜잭션 안에서 복습 권한을 생성한다.
- 일일 일정 ID가 연결된 세션만 새 권한 생성 대상이다.
- 자정 이후 완료, 최종 피드백 전 조회, 중복·동시 완료도 한 건의 권한으로 처리한다.
- 명시적 중도 종료, 미완료, 복습 세션 완료는 새 권한을 만들지 않는다.
- 기존 피드백과 `user_scenario_progress` 집계 흐름은 유지한다.

#### 작업과 검증

- [ ] 정상 완료와 비대상 완료의 권한 생성 여부를 통합 테스트로 작성한다.
- [ ] 자정 이후 완료와 중복 완료의 멱등성을 테스트한다.
- [ ] 테스트가 완료 직후 권한이 없어 실패하는지 확인한다.
- [ ] 완료 전이 지점에 접근 권한 생성을 연결한다.
- [ ] 완료·중도 종료·피드백 회귀 테스트를 통과시킨다.

```bash
./gradlew test --tests 'com.landit.landitbe.feature.session.ScenarioSessionApiIntegrationTests'
```

완료 후 `일일 시나리오 완료 시 복습 권한 부여` 단위로 커밋한다.

### Task 5: 전체 계약을 검증하고 결과를 기록한다

- [ ] `git diff --check`로 공백 오류를 확인한다.
- [ ] 전체 정적 검사와 테스트를 실행한다.
- [ ] 실패가 있으면 실제 오류를 기준으로 수정하고 영향받은 테스트부터 다시 실행한다.
- [ ] `design.md`의 완료 조건과 제외 범위를 diff에 대조한다.
- [ ] 아래 구현 결과에 실제 명령, 결과, 설계 변경이나 남은 위험을 기록한다.

```bash
git diff --check
./gradlew check
```

## 구현 결과

- 완료 커밋은 `d0bf464`, `55ecbde`, `9875b25`, `858fcea`, `c635fdf`다.
- `daily_scenario_schedule`, `user_scenario_access`, `scenario_session.daily_scenario_schedule_id`를 추가했고, 기존 진행·히스토리 데이터는 변경하거나 백필하지 않았다.
- 목록은 `CLEARED`, `TODAY`, `LOCKED` 상태와 `TODAY`의 다음 서울 자정 `expiresAt`을 반환한다. 기존 순차 잠금과 기존 완료 이력은 새 접근 상태 계산에서 제외했다.
- 세션 시작은 권한이 있으면 복습으로, 없으면 요청 시각의 오늘 일정과 일치할 때만 일일 세션으로 생성한다. 일일 세션 완료 시에만 복습 권한을 멱등 생성한다.
- `DailyScenarioScheduleServiceTest`에서 23시 59분 59초와 다음 날 00시의 서울 날짜 전환을, `ScenarioSessionApiIntegrationTests`에서 23시 59분 59초 시작 후 자정 완료 권한 부여를 검증했다.
- `ScenarioAccessServiceIntegrationTests`에서 서로 다른 트랜잭션의 동시 권한 부여가 한 건으로 수렴함을 검증했다.
- 최종 검증은 `git diff --check`와 `./gradlew check`가 모두 성공했다.
- 설계와 다른 구현은 없으며, 관리자 일정 관리·결제·알림·세션 복원·장기 진행 세션 정리는 범위 밖으로 유지했다.
