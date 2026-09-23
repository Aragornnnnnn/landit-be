# LAN-545 푸시 문구별 UTM 식별

## 적용 계약

[FE UTM 정본](https://github.com/Aragornnnnnn/landit-fe/blob/develop/docs/analytics-utm.md)을 따른다. 2026-09-21 확인한 정본 blob은 `35afb3fe2cf90d2a23bd2af9391bae478ed85315`다. 요청자의 확인에 따라 `utm_source=push`, `utm_medium=notification`을 유지한다. BE가 생성하는 문구에는 `utm_content`를 추가하고 기존 `utm_campaign`은 유지한다. 기존 UTM이 없었던 무료 체험 종료 안내에는 `trial_ending` 캠페인을 추가한다.

```text
/scenario?utm_source=push&utm_medium=notification&utm_campaign=daily_scenario_reminder&utm_content=scenario_bird_tip
```

- 슬러그는 문구 템플릿마다 고정된 소문자 스네이크 케이스다. 문구를 다듬거나 내부 enum 이름을 바꿔도 발행한 슬러그를 바꾸지 않는다.
- 닉네임·경과 일수·표현·대화 주제·퀴즈 수처럼 개인화되는 값은 슬러그에 넣지 않는다.
- 시나리오 A2와 R0는 제목과 본문이 동일하므로 `scenario_bird_tip`으로 함께 집계한다. DB에 저장하는 기존 문구 변형 값은 유지한다.
- 편지함 답장은 본문을 자유롭게 입력하므로 고정 템플릿 키 `mailbox_reply_arrived`를 사용한다. 답장별 원문 비교용 식별자는 아니다.
- 새로운 문구가 생기면 새 슬러그와 실제 문구를 아래 표에 추가하고 FE 담당자에게 전달해 정본에 반영한다. 기존 슬러그를 다른 문구에 재사용하지 않는다.
- 새 코드가 만드는 발송 URL부터 적용된다. 이미 저장된 발송 이력과 재시도 URL은 소급 수정하지 않는다.

## FE 공유용 캠페인 목록

아래 경로 모두 공통으로 `utm_source=push&utm_medium=notification`을 사용한다.

| 알림 종류 | utm_campaign | 딥링크 경로 | 변경 전 상태 |
| --- | --- | --- | --- |
| 오늘의 시나리오 | `daily_scenario_reminder` | `/scenario` | 기존 캠페인 유지. |
| 표현 학습 이어가기 | `continue_expression` | `/expressions/scenario/{scenarioId}/{expressionId}` | 기존 캠페인 유지. |
| 스몰톡 | `small_talk_reminder` | `/smalltalk` | 기존 캠페인 유지. |
| 표현 복습 퀴즈 | `expression_review` | `/reviews/{reviewId}` | 기존 캠페인 유지. |
| 편지함 답장 | `mailbox_reply` | `/mailbox/received/{letterId}` | 기존 캠페인 유지. |
| 무료 체험 종료 예정 | `trial_ending` | `/me/subscription` | 이번 변경에서 UTM 세트 추가. |

관리자 발송(`ADMIN_BROADCAST`, `ADMIN_BROADCAST_TEST`)은 관리자가 입력한 딥링크와 UTM을 그대로 사용한다. 고정 캠페인이나 BE 문구가 없다. 이번 변경에서 관리자 입력 계약은 변경하지 않는다. 문구별 집계가 필요하면 관리자 입력 URL에도 고정 `utm_content`를 지정해야 한다. 자동 덧붙이기는 기존 1,000자 URL 허용 범위와 저장 컬럼을 함께 검토해야 한다. 개발용 `TEST_NOTIFICATION`도 고정 운영 캠페인 목록에서 제외한다.

## FE 공유용 슬러그와 실제 문구

`<br>`는 실제 본문의 줄바꿈이다. 중괄호는 개인화 값이다.

| utm_campaign | utm_content | 기존 변형 | 제목 | 본문 |
| --- | --- | --- | --- | --- |
| `daily_scenario_reminder` | `scenario_daily_fruit` | A1 | 오늘만 가능한 시나리오 도착 💌 | 자기 전 5분으로 래디에게 열매를 먹여주세요 |
| `daily_scenario_reminder` | `scenario_bird_tip` | A2, R0 | 어떤 하얀 뱁새가 그러는데,, | 오늘이 지나면 이 시나리오가 사라진대요😵‍💫<br>자기 전 5분만 투자하세요 |
| `daily_scenario_reminder` | `scenario_five_minutes` | A3 | 오늘 학습 포기하실 건가요? 🥺 | 5분만 투자하면 열매를 얻을 수 있어요.<br>오늘만 할 수 있는 시나리오가 당신을 기다리고 있어요 💌 |
| `daily_scenario_reminder` | `scenario_streak_record` | A4 | 🚨 오늘의 시나리오를 깨면 연속 {다음 연속 일수}일 달성 | 5분 투자로 최고 기록을 달성해보세요! |
| `daily_scenario_reminder` | `scenario_restart_today` | R1 | 공든 탑이 무너지랴 | 어제 못했어도 오늘 공부하면 돼요‼️<br>자기 전 5분으로 다시 학습을 시작하세요 |
| `daily_scenario_reminder` | `scenario_waiting_days` | R2 | {미학습 일수}일째 {닉네임}님을 기다리고 있어요… | 배고픈 래디에게 열매를 주세요😭 |
| `daily_scenario_reminder` | `scenario_keep_habit` | R3 | 포기도 습관이다! | 하지만 {닉네임}님은 아직입니다!!<br>습관이 되기 전에 영어 공부 5분만 해봐요🥺 |
| `daily_scenario_reminder` | `scenario_feedback` | R4 | 우리가 마음에 안 드시나요..? | 영어 공부를 안 하시는 이유가 궁금해요. |
| `daily_scenario_reminder` | `scenario_missing_you` | R5 | 어라 이상하다 왜 공부하러 안 오지? | {닉네임}님이 이럴 사람이 아닌데… |
| `daily_scenario_reminder` | `scenario_please_study` | R6 | 제가 어떻게 해야 공부하러 오실까요? | 제발 5분만 영어 공부해요 우리 |
| `continue_expression` | `expression_usage_question` | EXPRESSION_DYNAMIC | “{표현}”, 어떤 상황에서 쓸까요? | 오늘 시나리오에서 이어지는 표현을 배워보세요. |
| `continue_expression` | `expression_continue` | EXPRESSION_GENERIC | 표현 학습을 이어가 볼까요? | 오늘 시나리오에서 이어지는 표현을 배워보세요. |
| `small_talk_reminder` | `small_talk_continue_topic` | SMALL_TALK_DYNAMIC | 하던 얘기 이어서 해봐요 | {최근 대화 주제} 이야기, 테디와 조금 더 나눠볼까요? |
| `small_talk_reminder` | `small_talk_teddy_waiting` | SMALL_TALK_GENERIC | 오늘은 스몰톡 안 하시나요? 🥺 | 테디가 당신과의 대화를 애타게 기다려요. |
| `expression_review` | `expression_review_quiz` | 단일 템플릿 | 배웠던 표현, 다시 꺼내 볼까요? | 표현 {문제 수}개를 짧은 퀴즈로 복습해 보세요. |
| `mailbox_reply` | `mailbox_reply_arrived` | 단일 템플릿 | 문의에 답변이 도착했어요 | {답장 제목} |
| `trial_ending` | `trial_ending_subscription_check` | 단일 템플릿 | 무료 체험 종료 예정 안내 | 무료 체험이 곧 종료돼요. 결제 전 구독 정보를 확인해 주세요. |

## 운영 발송 이력 확인

2026-09-21 운영 DB `push_delivery`를 읽기 전용으로 조회했다. 조회 시점 기준 최근 30일의 `requested_at`을 대상으로 집계했으며, 아래 건수는 사용자 수가 아닌 토큰별 발송 이력 수다. `DELIVERED`는 서버에 기록된 Expo Receipt 성공 상태이며 실제 기기 표시·탭을 증명하지 않는다. 모든 조회 결과에서 `utm_content`는 없었고 source/medium은 `push`/`notification`이었다.

| 종류 | 실제 utm_campaign | DELIVERED | FAILED | 마지막 요청 시각(KST) |
| --- | --- | ---: | ---: | --- |
| 시나리오 | `daily_scenario_reminder` | 7,838 | 305 | 2026-09-20 20:02 |
| 표현 이어가기 | `continue_expression` | 142 | 9 | 2026-09-20 20:02 |
| 스몰톡 | `small_talk_reminder` | 321 | 18 | 2026-09-20 20:02 |
| 편지함 답장 | `mailbox_reply` | 13 | 0 | 2026-09-21 14:39 |
| 관리자 발송 | `admin_선물_받은_프리미엄_써보셨나요_0918` | 58 | 0 | 2026-09-18 12:30 |
| 관리자 발송 | `admin_테스트_0914` | 2 | 0 | 2026-09-14 15:03 |

관리자 캠페인은 가독성을 위해 URL 디코딩한 값이다. 실제 저장 URL에는 한글이 퍼센트 인코딩되어 있어 현재 소문자 스네이크 케이스 규약과 다르다. 과거 집계를 분리하지 않도록 이번 변경에서 기존 값을 바꾸지 않는다. 관리자 사전 테스트 이력도 별도로 10건 있으며 캠페인은 위 두 값과 `admin_테스트_0918`이다. 관리자 발송은 이미 발송 묶음마다 다른 캠페인 값을 사용한다.

`expression_review`, `trial_ending`은 최근 30일 발송 이력이 없다. 코드상 지원하는 종류와 운영 발송이 관측된 종류를 구분해야 한다.

## 검증

- 예약 문구 15개 변형의 고정 슬러그 계약, A2/R0의 동일 슬러그, 표현·스몰톡 개인화 및 일반 문구 전환을 검사한다.
- 학습·복습·답장·체험 종료 발송 경로의 실제 생성 URL에 campaign/content가 함께 실리는지 검사한다.
- `./gradlew spotlessApply check --no-daemon --console=plain` 통과. Spotless·Checkstyle 통과, 테스트 1,368개 중 1,359개 통과·9개 생략·실패 및 오류 0개다.
- `git diff --check` 통과. 공유 표의 슬러그와 고정 문구를 코드와 대조했다.
- 배포, 실제 기기 알림 탭, FE 분석 이벤트 적재는 이번 로컬 구현 검증에 포함하지 않는다.
