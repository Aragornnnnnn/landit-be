# 프리톡 표현 1,672개와 검수 이미지 연결 계획

> **For agentic workers:** Use superpowers:executing-plans to implement this plan in the current session. All work is explicitly authorized through draft PR creation.

**Goal:** 검수된 ID 4329~6000의 문구·퀴즈·임베딩을 보존하고 게시 완료 이미지 5,016개의 URL을 반영한 PostgreSQL migration을 develop 대상 draft PR로 제출한다.

**Architecture:** 현재 마지막 migration V140 다음에 V141을 추가한다. 원본 SQL의 단일 INSERT와 보호 절차를 유지하며 대표 이미지와 추가 예문 3·4번 URL만 채운다. ID·표시 순서 충돌, 이미지·임베딩 사후 조건을 검사한다.

**Tech Stack:** Java 21, Spring Boot 4, Gradle, Flyway, PostgreSQL, pgvector, Jackson.

**Spec:** 사용자의 이번 요청과 아래 승인 범위·고정 입력이 이 계획의 기준이다.

## 승인 범위와 고정 입력

- 이슈: `LAN-610`. 사용자가 최종 브랜치를 `feat/LAN-610`으로 지정했다. origin/develop 기준 별도 브랜치에서 검증·커밋·push·draft PR까지 허용됐다.
- 기준 커밋: `6eac5db55` (`origin/develop`). 기존 체크아웃의 사용자 변경은 보존한다.
- 검수 원본: `writing_expression_quiz_corrected.sql`, SHA-256 `a9592c303a90ef3986819b12127bc388dd1f048158c2abf942aa87fd8402d8dd`.
- 업로드 매핑 SHA-256: `ad2a730bb23b52a6f6c14f682d6c4c2762840770c918a33285939fe022fdbcda`.
- 표현 1,672개, 예문 6,688개, 대표 이미지 1,672개, 예문 3·4번 이미지 3,344개.
- 이미지 외 원본 행을 재구성한 SHA-256: `17fa28c525b6e9cae7efc9c1073ce20f943f360000b0ab94ae58faf4ffa74723`.
- 원본 검수 SQL·문구·정답·보기·배열 순서·임베딩은 변경하지 않는다. 새 S3 변경, 실제 개발·운영 DB 접근/실행, merge, 배포는 하지 않는다. 폐기 가능한 로컬 테스트만 실행한다.
- 음성 및 발음 평가 기준 자산은 범위 밖이다. 콘텐츠 조회에서는 미준비 음성을 null로 처리하지만 발음 평가는 별도 자산 준비가 필요하다.

## Review Focus

- 표현 ID 또는 예문 3·4번에 다른 이미지가 연결되지 않아야 한다.
- 한국어 허용 정답은 원본 토큰의 중복 개수와 순서를 보존해야 한다.
- 1~5 난이도와 1,536차원 임베딩이 실제 스키마에서 적재되어야 한다.
- 기존 ID/표시 순서 충돌 시 전체 적재가 중단되고 기존 데이터·앞선 시퀀스를 보존해야 한다.
- 음성 없는 표현의 조회와 발음 평가 제한을 구분하며, 실제 DB 충돌 검증을 했다고 주장하지 않아야 한다.

## Task 1: 원본 보존 migration과 검증 자료

**Files:**
- Create: `src/main/resources/db/postgresql/V141__insert_reviewed_free_talk_expressions_4329_6000.sql`.
- Create: `src/test/java/com/landit/landitbe/FreeTalkExpressionV141MigrationTests.java`.
- Create: `src/test/java/com/landit/landitbe/FreeTalkExpressionV141PostgresTests.java` 및 필요한 로컬 테스트 fixture.
- Create: 이 디렉터리의 `image-assets.jsonl`, `validation.json`.

**Interfaces:** 원본 SQL의 25컬럼 행과 게시 매핑의 `(expressionId, exampleNumber)`를 소비한다. V141은 기존 서비스가 사용하는 `writing_expression` 데이터만 추가하며 공개 API를 변경하지 않는다.

- [x] `./gradlew check --offline --no-daemon --console=plain`로 변경 전 기준 상태가 통과함을 확인한다.
- [x] 원본 1,672행 전체를 파싱해 필수 문자열·길이·enum·FK/source·순서·난이도·배열·보기 수량·허용 정답·임베딩을 검사한다. 오류 0건이다.
- [x] 신규 테스트를 먼저 추가하고 V141 부재에 대해 실패함을 확인한다.
- [x] 원본을 읽기만 하며 이미지 URL 5,016개를 삽입한다. 추가 예문 1·2번 imageUrl은 null로 유지한다.
- [x] migration에서 ID 4329~6000, FREE_TALK 표시 순서 3518~5189 충돌을 차단하고 이미지·임베딩 사후 조건을 강화한다.
- [x] 테스트에서 이미지 URL을 제거한 행 해시를 고정 원본 해시와 비교하고 실제 ExpressionPracticeService로 모든 예문 응답을 구성한다.
- [x] 격리된 로컬 PostgreSQL/pgvector에서 정상 적재·기존 행 보존·시퀀스·충돌·사후 검증 실패 롤백을 확인한다.
- [x] 관련 테스트와 `./gradlew check` 및 배포 검증 스크립트를 실행한다. 예상 결과: 실패 0건.
- [x] 새 컨텍스트에서 전체 브랜치를 리뷰하고 중요한 결함을 수정한다.
- [x] `feat: 검수된 프리톡 표현 1672개와 이미지 URL 추가`로 구현을 커밋한다.

검증 후 제출 단계는 원격 feature 브랜치 push와 base `develop`의 draft PR 생성이다. 기존 `feat` label과 작업자 assignee를 지정하며, 실제 제출 URL·원격 head SHA·자동 CI 상태는 PR과 최종 보고에 기록한다.

## 확인 근거

- V127은 프리톡 ID 3001~4000과 표시 순서 2518~3517을 적재한다. V136은 시나리오 표현 ID 4001~4328을 적재한다. 현재 migration에 이번 표현 ID가 적재된 선례는 없다.
- V19가 대표 영어 단어/보기 배열을 NOT NULL로 만들며 V23이 enum, V51이 source/scenario 관계와 vector(1536), V73이 난이도 1~5를 규정한다.
- `ExpressionPracticeService`는 필수 문자열 5종·단어 배열 4종·예문 4개를 요구하고 앞 2개를 퀴즈, 뒤 2개를 이미지 예문으로 사용한다.
- `ParsedPracticeSentence`는 한국어 허용 정답의 토큰별 개수를 검사한다. 원본의 12,629개 허용 배열이 계약을 만족했다.
- `ExpressionPronunciationQueryService.findAudio`는 자산이 없으면 두 음성 URL을 null로 반환한다. `ExpressionPronunciationService.requireCompleteAsset`는 자산이 없으면 발음 평가를 거부한다.
- S3와 CloudFront는 선행 승인 작업에서 5,016개 모두 크기·SHA-256을 검증했다. 이번 PR에서는 추가 게시하지 않는다.

## 검증 결과

- 브랜치명 지정 전 `./gradlew check --offline --no-daemon --console=plain`: **BUILD SUCCESSFUL**, 1분 34초. 테스트 1,969건 중 1,948건 통과, 21건 제외, 실패·오류 0건. Spotless와 Checkstyle 포함.
- 신규 검증 13건 모두 통과: 원본 행 해시·이미지 매핑·서비스 소비·임베딩 4건, 격리 PostgreSQL 적재·충돌·롤백·시퀀스 9건.
- `bash -n .github/scripts/verify-ecs-deployment.sh` 및 `bash .github/scripts/test/verify-ecs-deployment_test.sh`: 종료 코드 0.
- 로컬 PostgreSQL 15.18과 pgvector v0.8.0의 실제 vector 타입·함수로 V141을 실행했다. 운영 PostgreSQL 17 또는 V1~V141 전체 migration 체인을 재실행한 검증은 아니다. CI에는 전용 DB가 없어 PostgreSQL 9건은 별도 설정 없이는 제외된다.
- 이미지 예문 3·4번 중 988개는 선택적 한국어 허용 정답 배열이 없다. 서비스의 정규 답안 fallback을 확인했으며 검수 데이터를 수정하지 않았다. 퀴즈로 쓰이는 예문 1·2번에는 해당 데이터가 모두 있다.
- 원본 SQL SHA-256은 보존됐고, 이미지 URL 치환을 되돌린 1,672개 행이 원본과 바이트 단위로 일치한다.
- 최신 원격 `develop`은 기준 커밋 `6eac5db5584a2b86f844d0309f33d593e2f17a39`와 같음을 제출 전 확인했다.

## 최종 리뷰와 검증 한계

새 컨텍스트의 읽기 전용 리뷰에서 Critical/Important 결함은 발견되지 않았다. 검수 원본 보존, 전체 이미지 매핑, 한국어 토큰 수량·순서, 충돌 차단, 시퀀스 조정 및 음성 처리 계약을 확인했다.

경미한 미수정 사항: `src/test/resources/db/v141-writing-expression.sql`의 시나리오 순서 고유키가 V78 이전 정의다. V78은 난이도 그룹을 포함하는 인덱스로 교체한다. 이번 FREE_TALK 행의 `scenario_id`는 모두 null이므로 V141 적재 결과에는 영향이 없지만, 이 fixture를 시나리오 적재 테스트에 재사용하기 전에는 갱신해야 한다.

리뷰 범위 판단은 다음과 같다.

- 실제 개발·운영 DB 점유 여부는 확인하지 않았다. migration의 충돌 거부는 격리 테스트로 검증했으며, 실제 충돌이 있으면 배포 시 migration이 중단될 수 있다.
- S3·CloudFront는 선행 게시 작업의 5,016개 전수 검증을 근거로 한다. 이후 원격 객체 변경까지 보장하지 않는다.
- 문구·이미지의 편집 품질은 사용자 검수 결과를 유지했다. 의미나 정답을 새로 교정하지 않았으므로 기존 검수 오류가 있다면 그대로 남는다.
- 음성 URL과 발음 평가 기준 자산은 범위 밖이다. 콘텐츠 조회는 가능하지만 발음 평가 기능은 별도 자산 준비가 필요하다.
- 전체 프로젝트 검사는 통과했으나, 격리된 대상 테이블 fixture 검증만으로 운영 환경과 전체 migration 이력의 차이까지 보장하지 않는다.

브랜치명 지정 후 문서를 `docs/tasks/LAN-610/`으로 이동하고 이미지 매핑 테스트의 입력 경로만 변경했다. migration·검수 데이터·이미지 매핑 내용은 동일하다. 변경 후 `./gradlew spotlessApply check --offline --no-daemon --console=plain`은 1분 33초에 통과했다. 테스트 1,948건 통과, 21건 제외, 실패·오류 0건이며 신규 13건도 모두 실행됐다. 경로가 짧아지면서 생긴 초기 줄바꿈 검사 오류는 공식 포맷터로 수정한 뒤 재검증했다.
