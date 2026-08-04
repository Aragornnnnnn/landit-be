# LAN-234 스트릭 API 설계

## 범위

- 현재 PR은 정상 완료된 시나리오만 KST 날짜별 활동으로 기록한다.
- 프리토킹은 완료 흐름이 병합된 뒤 같은 스트릭 기록 서비스를 호출해 통합한다.
- 로그인용 현재 스트릭 API와 월별 달력 API를 제공한다.
- 출시 이전 완료 기록은 백필하지 않는다.

## API

| API | 응답 |
| --- | --- |
| `GET /api/v1/me/streak` | `currentStreakDays`, `activeToday`, KST 기준 `today` |
| `GET /api/v1/me/streak/calendar?year={year}&month={month}` | KST 기준 `today`, `currentStreakDays`, `activeToday`, `firstActiveDate`, `longestStreakDays`, `totalActiveDays`, 요청 월의 `activeDates` |

`month`는 1~12만 허용한다. `today`는 `activeToday` 계산에 사용한 KST 날짜다. `firstActiveDate`는 현재 연속 구간의 시작일이 아니라 기능 출시 후 최초 활성일이다. 스트릭 기록이 없으면 숫자는 0, `activeToday`는 false, 최초 활성일은 null, 날짜 목록은 빈 배열이다.

## 집계 정책

- `COMPLETED` 상태의 `GOAL_COMPLETED`, `MAX_TURNS_REACHED`, `TIME_LIMIT_REACHED`만 인정한다.
- `USER_ENDED`와 `INTERRUPTED`는 인정하지 않는다.
- 같은 날 여러 번 완료해도 활동일과 스트릭은 한 번만 증가한다.
- 마지막 활동일이 어제면 현재 스트릭을 1 증가시키고, 오늘이면 유지하고, 그보다 이전이면 1로 재시작한다.
- 조회 시 마지막 활동일이 어제보다 이전이면 현재 스트릭은 0을 반환한다. GET 요청은 DB를 갱신하지 않는다.

## 저장과 조회

- 기존 `user_learning_activity_summary`의 현재·최장 스트릭과 마지막 활동일을 사용한다.
- 기존 `user_daily_activity`에 사용자별 날짜 활동을 한 행으로 저장한다.
- `UNIQUE (user_profile_id, activity_date)`가 만드는 복합 인덱스로 사용자별 월 범위 조회를 처리한다. 날짜 단독 인덱스는 추가하지 않는다.
- 세션 완료, 일별 활동 기록, 스트릭 요약 갱신은 같은 트랜잭션에서 처리한다. 완료 응답을 반환한 뒤 달력 API를 호출하면 방금 완료한 날짜가 즉시 보인다.
- 현재 PR에는 시나리오 완료 흐름만 있다. 프리토킹 완료 흐름이 병합되면 같은 스트릭 기록 서비스를 호출한다.

## 검증

- 첫 완료, 연속 완료, 같은 날 재완료, 스트릭 재시작, 최장 기록 갱신을 검증한다.
- 현재 스트릭과 월별 달력 API의 기본값, 월 범위, 잘못된 월을 검증한다.
- 대화 완료 응답 직후 달력 조회에 오늘 활동이 보이는 통합 테스트를 추가한다.
- 최소 검증 명령은 `./gradlew check`다.
