# LAN-236 시나리오 캘린더 API 구현 계획

## 목표

기준 날짜가 포함된 창(WEEK 7칸, MONTH 42칸)의 모든 칸을 채워 반환하는 캘린더 조회 API를 추가한다.

## 구현 순서

1. 완료 이력 조회를 추가한다.
   - `UserScenarioAccessRepository`에 `granted_at` 구간 조회와 첫 완료 권한 조회를 추가한다.
   - `ScenarioAccessService`에 날짜별 완료 시나리오 목록과 첫 완료일 조회를 추가한다.

2. 캘린더 조립 서비스를 추가한다.
   - `ScenarioCalendarType`(`WEEK`, `MONTH`) enum을 추가한다.
   - `ScenarioCalendarService`가 창 시작 일요일과 칸 수를 계산하고 모든 칸을 채운다.
   - 완료일 칸은 완료 시나리오 ID와 썸네일, 미완료 오늘 칸은 `ScenarioProgressionService`의 배정 시나리오 ID만 담는다.
   - `label`은 기준 날짜가 속한 달 기준으로 만든다(1일 포함 주 = 1주차).

3. API를 노출한다.
   - `ScenarioCalendarResponse` DTO를 추가한다.
   - `ScenarioController`에 `GET /api/v1/scenarios/calendar?type&date`를 추가하고 `ScenarioControllerDocs`에 문서를 보완한다.
   - `type`·`date` 바인딩 실패는 공통 검증 오류(400)로 처리한다.

4. 통합 테스트를 추가한다.
   - 주간: date 생략 시 오늘 기준 창, 완료일·미완료 오늘·빈 칸이 7칸에 채워진다.
   - 같은 창을 다른 달 기준 날짜로 조회하면 label만 달라진다.
   - 월간: 42칸 고정, 이웃 달 완료일 포함, 미래 달 정상 응답.
   - 완료 이력 없는 사용자: `startedAt` null, 오늘 칸에만 배정 시나리오 ID.
   - 오늘 완료 시 오늘 칸이 완료 칸으로 바뀐다.
   - 로케일·사용자 격리와 `type`·`date`(불가능한 월 포함) 검증 400을 확인한다.

5. 전체 검증한다.
   - `./gradlew check`를 실행한다.
