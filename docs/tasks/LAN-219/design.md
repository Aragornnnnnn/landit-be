# LAN-219 사용자별 일일 시나리오 정책 설계

## 목표

사용자는 가입 후 시나리오 1번부터 순서대로 학습한다. 한 시나리오를 완료해야 다음 시나리오가 열리며, 완료한 다음 날에만 다음 시나리오를 제공한다.

시나리오를 시작하지 않았거나 시작 후 완료하지 못했으면 다음 날에도 같은 시나리오를 제공한다. 중단 이력이 있으면 프론트가 재도전 안내를 노출할 수 있어야 한다.

## 정책

- 시나리오 제공 순서는 카테고리와 `displayOrder`를 사용하지 않고 시나리오 ID 오름차순으로 정한다.
- 진행 순서에서는 사용자 언어로 노출할 수 있고 카테고리·시나리오·언어 콘텐츠가 모두 활성인 시나리오만 사용한다.
- 기존 사용자를 포함해 아직 새 복습 권한이 없는 사용자는 시나리오 1번부터 시작한다.
- 기존 `user_scenario_progress` 완료 이력은 새 진행 순서와 복습 권한에 사용하지 않는다.
- 사용자가 시나리오를 정상 완료하면 `user_scenario_access`를 생성하고, 그 시나리오는 이후 언제든 복습할 수 있다.
- 정상 완료한 날에는 다음 시나리오를 열지 않는다. 다음 날부터 다음 순서의 시나리오를 제공한다.
- 아직 시작하지 않은 현재 시나리오는 `NEW`다. 이전 날짜에 시작했지만 `COMPLETED`가 아닌 세션이 있으면 `RETRY`다.
- `IN_PROGRESS`와 `INTERRUPTED` 세션은 모두 `RETRY` 판정 대상이다.
- 앱 강제 종료는 세션을 `IN_PROGRESS`로 남기므로 다음 날 재도전 대상이 된다.
- 복습 권한이 없는 현재 시나리오만 새 학습으로 시작할 수 있다. 복습 권한이 있는 시나리오는 언제든 시작할 수 있다.
- 날짜 계산은 `Asia/Seoul` 기준이다.

## 데이터 구조

`user_scenario_access`만 유지한다. 이 테이블의 `granted_at`은 다음 시나리오의 제공 가능 날짜를 계산하는 데 사용한다.

전역 일정을 표현하던 `daily_scenario_schedule`과 `scenario_session.daily_scenario_schedule_id`는 제거한다. 사용자별 배정 테이블도 추가하지 않는다.

현재 제공할 시나리오는 시나리오 ID 순서에서 새 복습 권한이 없는 첫 시나리오다. 가장 최근에 생성된 복습 권한의 날짜가 오늘이면 다음 시나리오는 내일까지 잠긴다.

## API 계약

기존 `GET /api/v1/scenarios` 전체 목록 API는 제거한다. `GET /api/v1/scenarios/daily?date=yyyy-MM-dd`만 제공한다.

- `date`는 필수이며 `Asia/Seoul` 기준 오늘 또는 과거 날짜만 허용한다. 미래 날짜는 `400 INVALID_REQUEST`다.
- 오늘 조회는 먼저 오늘 최초 완료한 시나리오를 조회한다. 완료 이력이 없으면 현재 순서의 미완료 시나리오를 반환한다.
- 과거 조회는 해당 날짜에 `user_scenario_access.granted_at`이 생성된 최초 완료 시나리오만 반환한다. 이력이 없으면 `playable=false`, `scenario=null`이다.
- `scenario`가 있으면 `dailyScenarioType`은 항상 `NEW`, `RETRY`, `CLEARED` 중 하나다. 과거와 오늘의 완료 이력은 `CLEARED`다.
- 완료 시나리오는 복습 가능하므로 `playable=true`다. 오늘의 미완료 시나리오도 `playable=true`다.
- `completedAt`은 `user_scenario_access.granted_at`, `starRating`은 `user_scenario_progress.best_star_rating`을 사용한다.
- `expressionCount`는 사용자 언어 조합의 활성 Writing 표현 수이며, `completedExpressionCount`는 그중 사용자가 완료한 고유 표현 수다.
- 복습 완료는 새로운 날짜별 이력을 만들지 않는다. 기존 `user_scenario_progress` 이력만으로는 과거 이력을 만들지 않는다.

`POST /api/v1/scenarios/{scenarioId}/sessions`는 복습 권한이 있거나 현재 사용자에게 제공 중인 시나리오만 허용한다.

`POST /api/v1/sessions/{sessionId}/messages`에서 정상 완료되면 접근 권한을 생성한다. 중도 종료와 미완료 세션은 접근 권한을 생성하지 않는다.

## 제외 범위

- 앱 로컬 알림.
- 결제와 유료 해제.
- 관리자 일정 관리 API와 관리자 페이지.
- 앱 재실행 후 진행 중 세션 복원 API.
- 장기 `IN_PROGRESS` 세션 자동 정리.
- 기존 완료 이력의 백필.
