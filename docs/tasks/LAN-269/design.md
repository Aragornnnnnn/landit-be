# LAN-269 시나리오 이미지와 표현학습 데이터 반영 설계

## 목표

- 시나리오 `1~40`의 썸네일 URL을 새 CloudFront URL로 교체한다.
- 신규 시나리오 `21~40`에 Writing 표현 `80`개와 연습 예문 `320`개를 추가한다.
- 연습 예문 URL은 실제 DB `scenarioId 21~40`과 `expressionId 84~163`을 경로에 사용한다.

## 입력 기준

- 썸네일: `LAN-269-scenario-thumbnail-cloudfront-urls.md`의 40개 URL.
- 표현학습 이미지: `LAN-207-practice-example-cloudfront-urls.md`의 320개 URL.
- 표현 본문: PR #75의 `V40__add_lan_207_writing_expressions.sql`.
- 시나리오 본문과 TTS 매핑: `develop`에 반영된 PR #82의 V39~V43.

## 반영 방식

- 운영 전용 콘텐츠이므로 `db/postgresql/V44__add_scenario_thumbnails_and_writing_expressions.sql` 한 파일에 반영한다.
- 썸네일은 `CASE scenario.id`로 40개를 일괄 갱신한다.
- 표현 데이터는 ID `84~163`을 명시해 삽입하고 `representative_image_url`은 `NULL`로 유지한다.
- 시나리오 `1~40` 누락, 표현 ID 충돌, 신규 시나리오의 기존 EN/KR 표현 충돌이 있으면 적용을 중단한다.
- 표현 충돌 검사부터 삽입과 sequence 보정까지 동시 INSERT가 끼어들지 않도록 테이블을 잠근다.
- 삽입 후 `writing_expression` identity sequence를 현재 최대 ID로 맞춘다.
- H2에는 PR #82의 신규 시나리오가 없으므로 공통 migration 경로에는 두지 않는다.

## 검증 기준

- 썸네일 URL 40개가 시나리오 ID `1~40`과 일대일 대응한다.
- 연습 예문 URL 320개가 중복 없이 `(scenarioId, expressionId, example)`에 대응한다.
- 표현 ID 공식은 `84 + (scenarioId - 21) * 4 + (displayOrder - 1)`이다.
- 전체 `./gradlew check --rerun-tasks --no-daemon`이 통과한다.
