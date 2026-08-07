# LAN-219 사용자별 일일 시나리오 구현 계획

## 목표

사용자별로 시나리오 1번부터 순서대로 제공하고, 완료 전에는 같은 시나리오를 유지하며, 완료 다음 날에만 다음 시나리오를 제공한다.

## 구현 순서

1. 날짜별 단건 조회 API의 실패 테스트를 먼저 추가한다.
   - 오늘의 `NEW`, `RETRY`, `CLEARED` 응답과 과거 완료 이력, 과거 이력 없음, 미래 날짜 거부를 검증한다.
   - 활성 표현 수와 완료 표현 수를 실제 저장 데이터로 검증한다.

2. 기존 전역 일정 구조를 제거한다.
   - `V26`에서 `daily_scenario_schedule`과 세션 일정 외래 키를 제거한다.
   - `DailyScenarioSchedule` 관련 도메인·Repository·Service를 제거한다.
   - `user_scenario_access`는 유지한다.

3. 목록 API를 날짜별 단건 조회 API로 교체한다.
   - `GET /api/v1/scenarios`와 목록 전용 DTO·Service·Repository·테스트를 제거한다.
   - `GET /api/v1/scenarios/daily?date=`가 `NEW`, `RETRY`, `CLEARED` 중 하나의 상태와 단건 시나리오 정보를 반환한다.
   - 과거 완료 이력은 `user_scenario_access.granted_at`으로 조회하고, 복습 완료는 새로운 이력으로 만들지 않는다.
   - 세션 시작은 복습 권한 또는 현재 제공 시나리오일 때만 허용한다.

4. 완료 시점에 복습 권한을 생성한다.
   - 세션이 정상 완료되면 `user_scenario_access`를 멱등 생성한다.
   - 복습 세션 완료는 기존 권한을 유지한다.

5. OpenAPI·보안 설정·문서와 전체 검증을 갱신한다.
   - 일일 조회 경로에 인증을 적용하고 삭제한 목록 경로를 OpenAPI에서 제거한다.
   - `./gradlew check`를 실행한다.

## 구현 결과

- 전역 `daily_scenario_schedule`과 세션 일정 외래 키를 제거하고, `user_scenario_access`만 새 정책의 완료·복습 기준으로 사용한다.
- 날짜별 단건 API는 오늘의 현재 시나리오 또는 과거 최초 완료 이력을 `NEW`, `RETRY`, `CLEARED`로 반환한다.
- 제공 순서는 카테고리와 노출 순서에 의존하지 않고 시나리오 ID 오름차순으로 계산한다.
- 사용자 언어로 노출할 수 없는 콘텐츠와 비활성 콘텐츠는 진행 순서에서 제외해 다음 활성 시나리오가 열리도록 한다.
- 정상 완료된 시나리오는 즉시 복습 가능하지만 다음 순번은 다음 날에만 제공한다.
- `./gradlew check`를 성공했다.
