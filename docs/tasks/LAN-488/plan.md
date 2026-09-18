# LAN-488 작업 기록

## 2026-09-18 전체 테스트의 한국어 표시 이름 보완

사용자가 테스트 코드를 읽거나 실행 결과를 볼 때 검증 의도를 알 수 있도록, 빠진 `@DisplayName`을 모두 추가하도록 요청했다. 기준은 LAN-488 `661dfa26`이다.

- [x] JDK 구문 트리로 일반·파라미터 테스트 메서드를 조사했다. 179개 파일의 메서드 1,157개 중 기존 설명은 81개이고 누락은 1,076개였다.
- [x] 누락된 1,076개에 검증 조건과 기대 결과를 한국어로 추가했다. 기존 Javadoc은 본문과 대조해 활용하고, 긴 설명이나 실제 assert 범위를 넘는 설명은 간결하게 조정했다.
- [x] 작문 연습 테스트의 오래된 주석을 실제 응답인 예문 2개·작문 문제 2개로 수정하고 Java 스타일 문서에 표시 이름 작성 기준을 기록했다.
- [x] 변경 후 전체 1,157개 메서드에 한국어 표시 이름이 있으며, 같은 클래스 안의 중복과 누락은 없다. 기존 81개 표시 이름과 테스트 메서드명·본문을 보존했다.
- [x] `./gradlew spotlessApply check` 통과. 파라미터별 실행을 포함해 총 1,244개 중 성공 1,238개, 실패·오류 0개, 환경 조건 생략 6개다. 생략은 PostgreSQL 관리자 SQL 4개, 실제 AI 1개, FE·BE·AI 교차 검사 1개다.
- [x] 167개 테스트 파일의 변경은 표시 이름·import와 주석 1곳뿐임을 비교했고 `git diff --check`가 통과했다. 운영 소스·테스트 입력·assert·DB 스키마는 변경하지 않았다.

## 2026-09-18 Controller와 하위 업무 패키지 배치 보완

사용자가 패키지·Controller 배치 설명에서 확인한 불일치 수정을 승인했다. 기준은 LAN-488 `3cc752c0`이다.
업무 → 역할 순서를 유지하고, 함께 담당하는 업무의 범위와 파일 위치를 맞춘다.

- [x] freetalk 상위 service의 시작 조율·시작 저장 Service와 시작 요청·응답·중간 값 DTO를 `start`로 이동한다. 대응 단위 테스트도 같은 위치로 옮긴다.
- [x] 종료 선택 요청 DTO는 `message.dto`, 주제·발화 시간 메인 응답은 `topic.dto`, 주제 Repository는 `topic.repository`에 둔다.
- [x] session·feedback·assessment를 함께 담당하는 SessionController와 문서는 `learning.scenario` 및 `docs`로 옮긴다. 선택·시작·관리 Controller는 담당 업무에 유지한다.
- [x] 이동 후 빈 디렉터리를 정리하고, Controller 배치와 DTO/record의 의미를 아키텍처 문서에 명시한다.
- [x] 전체 `./gradlew spotlessApply check` 통과. 총 1,244개 중 성공 1,238개, 실패·오류 0개, 환경 조건 생략 6개다. 생략 범위는 PostgreSQL 관리자 SQL 4개, 실제 AI 1개, FE·BE·AI 교차 검사 1개다.
- [x] 운영·테스트 Java 864개를 기준과 비교했다. 12개 파일 이동, package/import·타입 경로·공백 및 Controller 역할 주석을 제외한 본문은 동일하다. 이전 FQCN 참조와 resources·빌드 변경이 없고 `git diff --check`가 통과했다.

class 이름·메서드·HTTP 경로·JSON·오류 코드·DB 스키마·트랜잭션은 유지한다. 프리톡 하위 패키지를 별도 모듈로 나누거나 새로운 위임 Service를 추가하지 않는다.
빈 디렉터리는 로컬에서 정리했으며 Git이 추적하지 않으므로 별도 삭제 커밋은 없다. 기존 `scripts/__pycache__`는 변경하지 않았다.

## 2026-09-17 승인된 학습 모듈 통합 계획

사용자가 전체 구조 재검수와 실행 계획을 확인한 뒤 구현을 승인했다. 기준은 LAN-488 `fc349c2b`, origin/develop `017bd552`다. 아래 항목은 이번 통합 작업이며 이후 과거 완료 기록과 구분한다.

콘텐츠 정의는 content, 학습 실행과 결과는 learning이 소유한다. feature/config/shared, API 경로·응답·오류 동작, DB 스키마, 트랜잭션 원자성과 잠금 순서는 유지한다. learning 내부의 상태 소유 업무와 조율 업무를 구분하고 폴더 통합으로 순환 검사를 약화하지 않는다.

- [x] 진도 책임을 시나리오 진행과 표현 완료 이력으로 분리한다. Entity·Repository·공개 값 계약·호출부를 함께 정렬한다.
- [x] 사용자별 콘텐츠 수준 결정과 학습 이력 조회를 learning으로 옮긴다. content는 수준·언어 값을 받아 콘텐츠를 제공하고 사용자 조율을 역참조하지 않는다.
- [x] 공통 세션·메시지·히스토리는 conversation이 소유한다. 기능의 직접 Entity·Repository 접근을 명시적 조회·변경 Service와 값 계약으로 바꾸고 기능별 상태 판단과 공통 저장을 구분한다.
- [x] 시나리오 선택·접근·진도·세션·피드백·평가, 프리톡, 표현, 공통 대화를 learning 아래에 배치한다. 관리자 진입점은 소유 업무의 admin에 둔다.
- [x] 하위 업무 경계 검사, 기존 HTTP·학습·피드백·프리톡·표현 회귀 검사와 전체 check를 실행한다. 공유 DB 조회와 기능별 칼럼의 남은 결합을 명시한다.
- [x] 아키텍처 문서를 실제 최종 구조로 갱신하고 논리 단위로 커밋한다. LAN-461 → LAN-491 → LAN-494와 LAN-488에 직접 의존하는 LAN-300을 갱신하며 각 추가 코드의 경계를 별도 검수한다.

구현은 IntelliJ가 사용 중인 `/Users/sangmin8817/Soma/landit-be`의 feat/LAN-488에서 진행한다. 시작 시 추적 파일은 깨끗하며 기존 scripts/__pycache__는 제외한다. 백업 refs를 보존하고 후속 브랜치의 외부 갱신을 확인한 뒤 원격을 갱신한다.

### 구현 결과와 검증

- 최종 구조·Entity 위치·변경 전후 비교는 [아키텍처 문서](../../architecture/backend.md)에 정리했다. 시나리오 세션·피드백·평가는 같은 실행 경계로 검사하고, 선택·접근·진도·수준 결정과 공통 conversation은 각각 별도 경계로 검사한다.
- 새 공통 상태 변경 메서드는 상위 트랜잭션을 필수로 사용한다. 메시지 선점·재전송·피드백의 조건부 SQL 갱신과 기존 잠금 순서를 유지한다. 상태 변경 뒤 응답에 필요한 최신 snapshot을 다시 전달하고, JSON은 복사해 Entity 참조가 경계를 넘어가지 않게 했다.
- 기존 SessionController의 메시지·피드백·수준 평가 API는 시나리오 실행에 배치했다. 공통 종료 API는 LearningSessionController로 분리하되 URL과 OpenAPI 계약을 유지했다.
- LAN-488 소스 최종 커밋 `4b27438f`에서 `./gradlew spotlessApply check` 통과. 총 1,244개, 실패·오류 0개, 환경 조건 생략 6개다. 공통 세션·메시지 반환값이 Entity 자체라고 가정하던 단위 테스트를 값의 보존을 검사하도록 변경했다.
- 기준 `fc349c2b`와 HTTP 매핑 75개, Java text block 113개(SQL/JPQL 및 SQL 조각 111개·이메일 HTML 2개), Entity 55개, 오류 코드 파일 9개를 비교했다. 패키지·import·타입 경로·공백 정규화를 제외한 계약과 정의가 같다. `src/main/resources` diff가 없으며 `git diff --check`도 통과했다.
- 운영 PostgreSQL, 실제 AI·푸시·메일·기기 알림, 배포는 검증하지 않았다. 공유 DB JOIN과 공통 메시지의 시나리오·프리톡 전용 칼럼을 유지하므로 독립 MSA 배포가 가능한 상태는 아니다.

이번 통합은 진도 소유권, 수준 선택, 공통 세션 계약, 이력·메시지 계약, 패키지 이동·경계 검사, 문서의 여섯 논리 단위로 커밋한다. 파일 이동·공개 반환형·호출부·테스트가 함께 바뀌므로 줄 수를 맞추기 위한 분할은 하지 않았다. 기존 LAN-488 커밋 이력은 보존한다.

### 후속 브랜치 검증

| 브랜치 | 직접 부모 | 전체 검사 결과 | 추가 확인 |
| --- | --- | --- | --- |
| LAN-461 | LAN-488 | 1,251개, 실패·오류 0, 생략 6 | profile.alarm 독립 경계와 learning 경계 검사를 함께 보존했다. |
| LAN-491 | LAN-461 | 1,268개, 실패·오류 0, 생략 9 | 복수 정답 처리와 기존 패치 의미를 유지했다. |
| LAN-494 | LAN-491 | 1,286개, 실패·오류 0, 생략 9 | 복습 문제 조립을 learning.review의 값 계약으로 연결하고 무효 후보 건너뛰기와 트랜잭션을 검증했다. |
| LAN-300 | LAN-488 | 1,248개, 실패·오류 0, 생략 6 | 주제 70개·무작위 5개 응답을 learning.freetalk에 유지했다. |

각 브랜치의 `./gradlew check` 결과다. LAN-491/494에서 추가로 생략된 3개는 PostgreSQL 전용 검증이며, 로컬 검사를 운영 DB 적용 증거로 사용하지 않는다. 작업 전 원격 SHA를 `backup/LAN-*-before-learning-20260917`에 보존했다. 최종 문서 반영 후에도 검사한 소스 트리가 같은지 확인하고 명시적 expected SHA lease로 기존 PR 브랜치를 갱신한다.

사용자가 이슈 번호를 LAN-450에서 LAN-488로 정정했다. 아래 LAN-450과 과거 SHA는 이전 실행 기록이며, 현재 브랜치와 재구성 커밋은 LAN-488을 사용한다.

# LAN-450 모듈 경계와 패키지 구조 정리

## 승인된 방향과 완료 기준

사용자는 origin/develop 분석 결과와 수정 순서를 승인했고 LAN-450을 지정했다.
기준 커밋은 50a8e2a03953ef52365d99692cb91438000f9003이다.
API 경로·JSON·오류 코드·DB 스키마·잠금 순서·트랜잭션 원자성을 유지한다.
feature/config/shared를 유지하고 큰 기능은 업무별 하위 패키지로 묶는다.
다른 기능 Repository/Entity 직접 접근을 없애고 공개 Service와 값 계약으로 연결한다.
조회 JOIN은 일괄 조회 성능을 보존하기 위해 명시적인 조회 경계로 관리한다.
MSA/Gradle 멀티모듈/전면 Facade/불필요한 인터페이스는 도입하지 않는다.

## 실행 계획

- [x] 프로필과 표현 경계. UserLearningProfile record로 프로필 읽기/잠금 결과를 제공한다. Entity 변경은 profile 내부에 유지한다. 표현 완료의 세션 검증/변경은 session 소유 Service로 옮기고 기존 외부 트랜잭션과 검증→표현 잠금→세션 변경 순서를 유지한다. 세션 이력에서 표현을 일괄 조회하는 공개 record 계약을 사용한다.
- [x] 기억 경계. session→memory 단방향으로 정리한다. 생성/dispatch 오케스트레이터를 session으로 옮기고 memory AI 포트/계약을 memory가 소유한다. memory 저장은 사용자 잠금과 기억만 소유한다. session ContextService의 트랜잭션에서 memory 저장 후 READY 변경을 수행해 기존 원자성과 사용자→세션 잠금 순서를 보존한다. STALE 시 저장/READY 전환 없음, 실패 보상 동작 유지.
- [x] 패키지 분류. content의 scenario/expression/tutor, session의 scenario/freetalk/feedback/history, notification의 token/delivery/scheduled로 관련 Service·Repository·DTO를 함께 배치한다. 공개 record는 서비스 구현과 분리한다. profile 조회 계약에서 admin 전용 이름을 제거한다. SQL 전용 알림 Service는 조회 Repository로 분리한다.
- [x] 오류와 네이밍. 기능 ErrorCode 소유권을 정리하고 공통 HTTP 변환을 유지한다. 관리자 페이지 Bean Validation과 기존 오류 응답을 함께 검증한다. 행위가 불분명한 관련 메서드 이름을 정리한다.
- [x] 경계 검증과 문서. 타 기능 Repository/Entity 참조와 memory→session 역참조의 회귀를 검사한다. 조회 JOIN과 남은 결합의 허용 범위를 아키텍처 문서에 명시한다. 전체 check와 실제 Spring HTTP/DB 통합 테스트, 독립 Sol 리뷰를 통과한 후 논리 단위로 커밋한다.

## 구현 분담

프로필/표현/패키지 통합은 주 에이전트가 담당한다. 같은 파일을 함께 수정하지 않는다.
기억 구현 단위는 memory 소스/테스트, session AI 타입/구현체, session 기억 Context/호출부에 한정해 별도 구현할 수 있다. 공통 패키지 이동과 오류 체계 변경은 구현 단위 통합 이후 순차 진행한다.

## 검증 기록

- 기준 상태 ./gradlew check 성공(40초). 기본 sandbox는 Gradle cache lock 쓰기가 제한되어 승인된 확장 권한으로 실행했다.
- 이후 검증과 설계 보완은 이 문서에 이어 기록한다.

- 1차 경계 변경 `./gradlew spotlessApply check` 성공(34초), 886 tests / 0 failures / 0 errors / 0 skipped.
- 표현 완료 HTTP 진입점과 조율 Service는 `learning.expression`에 배치했다. content가 session을 다시 참조하지 않도록 콘텐츠 조회·잠금은 ExpressionContentService가 소유한다. learning의 진행/권한과 학습 조율은 별도 하위 책임이다.
- memory는 공개 ConversationMemoryPlanningService로 AI 추출/검증을 제공하고 mapper/resolution 내부 구현은 package-private로 유지했다.
- 메서드 검증의 기존 INVALID_REQUEST 응답과 size/page 경계를 HTTP 통합 테스트로 검증했다.
- 회귀 검사 추가: 외부 Repository/Entity import 금지, content/memory→session 및 shared→feature import 금지. SQL JOIN은 이 Java import 검사의 대상이 아니다.

- 2차 업무 패키지 재배치 `./gradlew spotlessApply check` 성공(34초). main 타입 307개를 업무별로 이동했다. session/scenario/service의 12개가 session의 최대 Service 패키지이며 notification은 token/delivery/scheduled로 분류했다.
- 패키지 분리 과정에서 메시지 피드백의 순수 변환을 AiMessageFeedbackEvaluationContext.from으로 옮겨 package-private 구현 노출을 피했다. 검증 규칙은 동일하다.
- 모듈 내부 Repository 공유는 허용하고 모듈 외부 직접 접근은 금지하도록 문서와 AGENTS를 정렬했다. Repository당 위임 Service를 강제하는 기존 문구는 거대 Service/불필요한 위임을 유발하므로 업무 소유권 기준으로 구체화했다.
- 감사 로그는 audit, 인증 사용자 식별 record는 shared.security에 배치해 admin/auth 화면 계층으로의 역참조를 줄였다.

- 3차 오류 소유권 분리 `./gradlew spotlessApply check` 성공(38초), 886 tests / 0 failures / 0 errors / 0 skipped. auth/content/session/app의 오류를 기능 소유로 옮기고 ApiErrorCode로 HTTP 계약만 공유한다.
- 기준 커밋 대비 Mapping 어노테이션 58개와 오류 코드·HTTP 상태·메시지 31개가 동일함을 소스 비교로 확인했다. Flyway migration 변경은 없다. 이는 소스 호환성 확인이며 운영 배포 검증은 아니다.
- 리뷰 그래프 갱신 결과 1,033 files / 5,641 nodes / 67,088 edges와 현재 HEAD 일치를 확인했다. 실제 판단은 소스·diff·테스트 결과를 함께 사용한다.
- 이번 범위에서는 feature/config/shared, 생성·수정 시간 Base Entity, KST 기준 일별 조회의 날짜 기본값을 유지한다. 폴더 이름이나 상속 자체보다 데이터 소유권과 업무별 탐색성을 우선하고, 날짜 정책·로그 암호화 같은 동작 변경은 섞지 않는다.
- SQL/JPQL text block 82개를 기준 커밋과 비교했다. DTO의 패키지 경로와 공백을 정규화한 조회 문자열 멀티셋이 동일하다. 리소스·빌드 설정에 이동 전 패키지 참조가 남지 않았으며 `git diff --check`도 통과했다.
- 독립 Sol(medium) HIGH 리뷰 완료. 표현 잠금 순서, 기억 저장/READY 원자성과 rollback, profile snapshot, AI memory 계약, 오류 transport 및 패키지 이동을 diff·호출부·테스트 산출물로 검토했고 blocker/actionable finding은 없었다.
- 최종 증거 범위는 로컬 Spring HTTP/H2 통합 테스트와 정적 구조 검사다. 운영 PostgreSQL, 실배포, 외부 AI 실호출은 검증하지 않았다.

## 2026-09-13 최신 develop 반영

현재 기준은 `066764a0a3bd5ad393b9040ff4c51fec43370a91`이다. 기존 기준 이후 130개 커밋의 구독·수준 평가·피드백 복구·관리자 푸시 동작을 보존하면서 기존 LAN-450 세 커밋에 병합한다. 기존 검증 기록은 당시 결과로 유지하며 아래 결과가 최신화 검증이다.

- [x] 최신 기능의 패키지를 통합한다. 관리자 캠페인은 `notification.campaign`, 수준 평가 관련 코드는 `session.assessment`로 분류한다. 공유 피드백 입력 record는 `session.feedback.dto`에서 제공한다.
- [x] 새 경계 위반을 보완한다. 수준 평가의 profile Repository 접근은 프로필 잠금 상태 record와 프로필 적용 메서드로 바꾼다. `applyAssessedLearningLevel`은 기존 상위 트랜잭션을 필수로 사용하고 이미 잠근 영속 Entity에 적용한다. 기존 사용자→세션→평가 저장 순서를 유지한다.
- [x] 복습 수준 조회 SQL을 `content.scenario.repository.ScenarioLearningHistoryQueryRepository`로 옮긴다. content→session Service 역참조를 제거하고 같은 공유 DB 조회·정렬·fallback을 유지한다.
- [x] 구독의 무료 시나리오 예약·표현 학습 시작, 세션 소유 상태 조회는 Entity 대신 값 record로 제공한다. 시도 ID·만료 시각·세션 종류·완료 상태와 최신 develop의 권한 검사를 유지한다.
- [x] 관리자 푸시 오류는 `NotificationErrorCode`로 옮긴다. 공통 오류에는 외부 연동 설정용 SERVICE_UNAVAILABLE을 유지한다.
- [x] 전체 검사와 독립 리뷰를 마친 뒤 통합 커밋을 저장한다.

현재 정적 검증에서 최신 develop의 HTTP Mapping 어노테이션 70개, 오류 코드·상태·메시지 36개, SQL/JPQL text block 100개가 동일하다. SQL 비교는 DTO FQCN과 공백만 정규화했다. `src/main/resources` 전체 diff가 없어 DB 마이그레이션·운영 설정 변경이 없다.

전체 테스트의 첫 실행에서 테스트 스캔 범위/fixture 연결 실패 18건을 확인했다. 푸시 배치 독립 Context는 token/delivery 양쪽 Repository·Entity를 스캔하도록 고쳤고, 변경된 반환 record와 기능 오류 enum을 테스트 fixture에 반영했다. 실제 검사 결과를 확인해 순차 재검증한다.

- 수정 대상 테스트 44개 재검증 성공(5초). 최종 `./gradlew check` 성공(54초). XML 집계 1,191 tests / 0 failures / 0 errors / 6 skipped로 실행된 1,185개가 모두 통과했다.
- 생략된 6개는 기존 환경 조건에 따른 PostgreSQL 관리자 대상 SQL 검사 4개, 실제 AI 호환성 1개, FE·BE·AI 교차 검증 1개다. 운영 PostgreSQL·외부 AI·배포 검증은 이번 증거에 포함하지 않는다.
- 독립 리뷰는 최신 기준과 현재 staged/unstaged 전체 diff, 구독 snapshot, 메시지 피드백 복구, 수준 평가의 잠금과 적용, 복습 SQL, 표현 완료와 기억 저장 원자성을 확인했다. actionable blocker는 없었다.
- 충돌 표식과 미해결 인덱스 항목이 없고 `git diff --check`가 통과했다. 이전 세 커밋은 보존한 채 최신 develop을 통합한다.

## 2026-09-13 전체 패키지 밀집도 재정리

사용자가 최신화 결과 전체에서 클래스가 몰린 패키지를 다시 정리하도록 요청했다. 이번 기준은 `6f69542f`이며 이전 모듈 소유권·외부 계약·트랜잭션은 유지한다. 클래스 개수만을 맞추려고 같은 구현의 package-private helper를 공개하지 않는다.

- [x] main 소스 전체의 실제 파일 수와 같은 패키지 타입 참조를 확인했다. 주요 밀집은 프리톡 AI 21개, 표현/profile DTO 각 16개, mailbox DTO 15개, 시나리오 Service와 공통 session domain 각 13개였다.
- [x] 표현을 pronunciation/practice/recommendation, 시나리오 콘텐츠를 category/question/schedule, 시나리오 세션을 start/message/innerthought와 메시지 feedback으로 묶었다. AI 요청·응답도 각 업무의 client/ai로 이동한다.
- [x] profile은 learning/preference/subscription 값 계약, mailbox는 feedback/letter, memory는 planning/retrieval, Apple 이전 CLI는 client/repository/service/dto/config/domain/exception 역할로 분류한다. 학습 복습 모델은 review, RevenueCat 이벤트는 subscription.event로 묶는다.
- [x] package-private 메시지 처리 helper 5개와 컨텍스트는 메시지 구현과 함께 두고, 기억 후보·매핑·판정 helper도 planning 구현과 함께 둔다. 접근 제한 변경이나 새 Service/인터페이스/DTO 추가는 없다.
- [x] 전체 check와 독립 리뷰, 계약 보존 검증 후 논리 단위로 커밋한다.

214개 main 타입의 위치를 바꿨다. 업무별 이동이며 새로운 배포 모듈이나 트랜잭션 경계 도입이 아니다. 9개 session 공통 domain은 시나리오·프리톡이 함께 쓰는 상태·종료·입력 계약이므로 유지하고, 8개 shared domain도 공통 코드다. 실제 최대 Service 패키지는 메시지 처리의 8개다.

- `./gradlew spotlessApply check` 성공(1분 2초). XML 집계 1,191 tests / 0 failures / 0 errors / 6 skipped로 실행된 1,185개가 통과했다. 생략 범위는 앞선 최신화 검증과 동일한 외부 PostgreSQL·AI 검사다.
- main 타입 617개를 기준과 비교했다. 파일 누락이 없고, package/import·타입 경로·포맷 공백을 정규화한 클래스 본문은 전부 동일하다. 타입명·공개 범위·메서드 로직의 변경이 없음을 함께 확인했다.
- HTTP Mapping 70개와 SQL/JPQL text block 100개가 동일하다. `src/main/resources`, 빌드 파일, 배포 workflow에는 변경이 없다. Apple 이전 CLI의 mainClass도 기존 진입점을 유지한다.
- 독립 리뷰에서 214개 타입의 이동 누락·중복, private 접근성, JPQL 경로, JPA 스캔, CLI 진입점, 테스트 보존을 확인했고 actionable blocker는 없었다. 170개 테스트 파일의 본문을 보존했으며 이 중 18개의 패키지 위치가 변경됐다. staged `git diff --check`도 통과했다.

## 전체 구조 감사 후 책임 분리 보완

사용자가 `75a66e7e` 전체 감사에서 발견한 추가 작업을 승인했다. 앞선 패키지 이동의 회귀 안전성과 구조 정리 완료를 구분하며, 다음 책임을 실제 코드와 검증 규칙에 반영한다. 기존 API·SQL·DB 스키마·오류·시간대·트랜잭션 및 잠금 순서는 보존한다.

- [x] 콘텐츠 조회와 학습 조율을 분리한다. 사용자별 시나리오 선택·조회는 `learning.scenario`, 표현 시작·완료는 `learning.expression`이 소유하고 content는 콘텐츠 값 조회·추천·연습 규칙을 제공한다. `learning.access/progress`는 독립 상태 소유 업무이며 세션이나 학습 조율에 의존하지 않는다.
- [x] 구독의 세션 역참조를 제거한다. 세션 소유권·상태는 session에서 검증하고 구독은 값으로 받은 시작 이력과 자체 권한만 판단한다. HTTP 경로별 보안 조율은 config.security에 두고 공통 실패 응답 작성기는 shared에 둔다.
- [x] 프로필의 인증·학습·설정·구독, 우편함의 편지·문의·답장 책임을 실제 Service로 나누며 같은 소유 Repository와 기존 트랜잭션을 유지한다. 프리톡은 재전송 응답 복원·응답 변환을 분리하고 예약·확정·보상 잠금은 유지한다.
- [x] memory의 AI 구현체를 memory가 소유하게 하고 공통 HTTP 전송만 shared에서 재사용한다. 외부에 사용하는 Service 내부 공개 record 10개를 업무별 DTO로 분리한다.
- [x] 컴파일된 타입 참조로 기능 경계·허용 업무 의존·순환·Controller 저장소 접근을 검사한다. 기존 import 정규식 검사보다 FQCN과 메서드 시그니처를 포함하는 검증을 사용한다.
- [x] 관련 회귀 검사, 전체 `./gradlew check`, 독립 리뷰 후 논리 단위로 커밋한다.


### 구현과 검증 결과

- 콘텐츠 조회는 `ScenarioCatalogService`, 표현 추천·연습·음성 조회는 각각의 content Service가 소유한다. 개인화 시나리오 화면은 `learning.scenario`, 표현 목록·시작·완료 조율은 `learning.expression`에 둔다. 관리자와 학습 화면이 함께 쓰는 `OpeningPreviewResponse`는 콘텐츠 값으로 별도 분리했다.
- `learning` 전체를 독립 모듈 하나로 취급하지 않는다. access/progress/review의 상태·값 계약과 expression/scenario 조율을 별도 업무 단위로 명시하고 실제 타입 의존 순환을 검사한다. 상위 learning/session 폴더 사이에는 양쪽 참조가 보이지만 업무 단위의 순환은 없다.
- Profile 인증·학습·설정·구독의 기존 구현을 각각 옮겼다. UserProfileService는 462줄에서 147줄, ExpressionQueryService는 475줄에서 99줄로 줄었다. AdminMailboxService 589줄의 업무는 편지 265줄·문의 조회 186줄·답장 185줄로 분리했다. FreeTalkSubmittedMessageService는 902줄에서 602줄로 줄었고 재전송 복원 196줄, 응답 조립과 내부 잠금 helper를 따로 둔다. 파일 수를 줄이는 목적이 아니라 변경 책임을 찾는 기준이다.
- Service 내부 공개 record 10개를 업무별 DTO로 옮겼다. main 파일 수는 617개에서 651개가 됐다. 기존 예약·확정·보상, memory 저장/READY 원자성, 프로필 잠금과 평가 적용의 트랜잭션은 유지한다.
- memory local/remote 어댑터를 memory가 소유한다. HTTP 계약 테스트를 해당 패키지로 옮겼고, session 구성을 전혀 등록하지 않은 Spring Context에서 기본/local/remote 모드의 단일 Bean 구성을 확인했다. 공통 전송의 파싱·오류·제한 시간은 기존 계약을 유지한다.
- 구독은 사용자·세션 종류가 확인된 시작 시각을 값으로 받는다. 도입 전 시작의 24시간 경계, 미검증 시각 거부, 만료된 저장 권한의 유예 우회 거부, 세션 소유자·종류 불일치를 회귀 테스트로 검증했다.
- `FeatureBoundaryTest`는 JDK `jdeps`의 컴파일 결과를 사용한다. FQCN으로 서로 참조하는 fixture를 실제 컴파일해 검출했고, 업무 간 Repository/Entity 접근, 같은 업무의 Controller 저장소 접근, 역참조, 순환, Service 공개 내부 record, memory 어댑터 소유를 검사한다.
- 기존 추천·연습·학습 흐름 및 memory AI 테스트는 새 책임 패키지로 분류하면서 검증 사례를 보존했다. 최종 `./gradlew spotlessApply check` 성공(55초): 1,202 tests / 0 failures / 0 errors / 6 skipped. 실행된 1,196개가 모두 통과했다. 생략된 6개는 기존 PostgreSQL·실제 AI·교차 저장소 환경 검사다.
- 독립 소스 리뷰에서 실제 동작 회귀를 발견하지 못했다. 주 세션에서도 HTTP 매핑 70개와 SQL/JPQL 100개를 포함한 Java text block 102개가 이름·패키지·공백 정규화 후 기준과 같은지 재확인했다. resources·Gradle·배포 workflow 변경이 없고 `git diff --check`가 통과했다.
- 물리 Gradle 모듈 분리나 MSA 전환은 하지 않았다. 공유 DB 교차 JOIN과 내부 Repository 공유의 허용 경계는 `docs/architecture/backend.md`에 유지한다. 로컬 검증은 운영 PostgreSQL·실제 외부 AI·배포 증거를 대신하지 않는다.

## 관리자 기능 분류와 커밋 재구성

- 관리자 Controller·docs·DTO·전용 Service를 소유 업무의 admin 하위 패키지로 모았다. 우편함은 admin 아래 편지와 문의/답장을 나눴다.
- 앱 버전 공개 확인과 관리자 정책 관리, NPS 사용자 제출과 관리자 조회를 실제 Service로 분리했다. 기존 조회·수정·감사 기록 트랜잭션과 Repository 소유권을 유지했다.
- 시나리오 테스트 시작 Service는 비공개 진행 제한 우회 메서드와의 결합 때문에 start.service에 남긴다. Controller는 scenario.admin으로 이동하고 develop 프로필 제한은 유지했다.
- 캠페인 발송 처리·스케줄러·외부 클라이언트, 감사 기록, Repository/Entity는 기존 업무 소유 패키지를 유지한다.
- 기존 이력과 완성 트리를 백업한 뒤 origin/develop을 기준으로 논리적 변경 단위로 재구성한다. 각 커밋의 컴파일과 최종 전체 check, 트리 일치를 확인한다.

### 최종 검증과 커밋 구성

- 관리자 추가 분리 후 `./gradlew check` 성공(51초). XML 집계 1,204 tests / 0 failures / 0 errors / 6 skipped이며 실행된 1,198개가 통과했다. 생략 범위는 기존 외부 PostgreSQL·실제 AI·교차 저장소 검사다.
- 관리자 분리 전 `b46fab45`와 비교해 HTTP Mapping 70개와 Java text block 102개(기존 SQL/JPQL 포함)가 동일하다. 패키지·타입 경로와 공백만 정규화했다. main Java 타입은 651개에서 653개로 늘었다.
- 재구성 후 `src` 전체는 관리자 변경을 검증한 백업 `87ba665a`와 byte 단위로 동일하다. DB 스키마·리소스·배포 설정 변경은 없다.
- 새로 분리한 책임 변경 9개와 관리자 변경 8개는 각각 `spotlessApply compileTestJava`를 통과했다. 경계 검사 변경은 전체 check로 검증했다. 기존 첫 5개 커밋은 검증된 트리를 보존하고 이슈 번호와 변경 타입을 정렬했다.
- 현재 브랜치는 `feat/LAN-488`이다. 기존 `feat/LAN-450`과 검증 완료 트리 `backup/LAN-488-complete-tree`를 보존했다. push·PR·merge·배포는 수행하지 않았다.

| 구분 | 개수 | 분리 기준 |
| --- | --- | --- |
| 기존 소유권·업무 패키지·오류 정리 | 4 | 기존 검증 트리를 보존했다. 순수 경로 이동은 rename, 책임 변경을 포함한 재배치는 refactor다. |
| develop 통합 | 1 | 개발 기준 갱신의 merge 이력이며 독립 기능 커밋으로 세지 않는다. |
| 추가 책임 분리 | 9 | 공개 값 계약, 프로필, 우편함, memory AI, 개인화 시나리오, 표현 학습, 프리톡 재전송, 공통 보안 응답, 구독/세션 경계. |
| 관리자 기능 분리 | 8 | 앱 버전, NPS, 시나리오 콘텐츠, 발음 자산, 이미지 업로드, 우편함, 푸시 캠페인, 시나리오 테스트 진입점. |
| 경계 검사 | 2 | 컴파일된 모듈 의존과 관리자 진입점/사용자 Controller 참조. |
| 문서 | 1 | 관리자 패키지 기준, LAN-488 작업 경로, 최종 검증과 커밋 구성. |
| 합계 | 25 | 작업 커밋 24개와 develop 통합 merge 1개. |

30줄은 권장 크기이며 이번 작업에서는 파일 이동과 필수 import 변경, 큰 기존 구현의 추출로 초과한다. 줄 수만 맞추려고 호출부·계약·검증을 떨어뜨리지 않았다. 이력 분리는 전체 변경량을 줄이지 않으므로 PR을 만들 때는 별도로 리뷰 범위를 나눌 필요가 있다.


## 2026-09-16 develop rebase

- 기준을 `origin/develop`의 `8941e7e8`로 갱신했다. 기존 통합 merge의 해결 내용을 보존하고 이후 업무·책임·관리자 분리 커밋을 다시 적용했다. 아래 검증은 최종 브랜치 기준이며 이전 중간 커밋 검증 기록은 당시 결과다.
- LAN-484의 RevenueCat 이벤트 검증·로그·정규화와 동일 계정/만료 구독 이전 보호를 보존했다. 프로필 구독 변경은 `ProfileSubscriptionService`가 담당한다.
- LAN-499의 무료 시나리오 무제한 시작·전송, 완료 세션 재개 방지, 첫 시나리오 최초 완료 세션의 상세 피드백 공개를 보존했다. 새 정책 조율은 `ScenarioFeedbackAccessService`에 배치해 subscription → session 역참조를 제거했다.
- 최신 develop의 피드백 정책 단위 테스트 3개를 session의 새 Service 테스트로 옮기고 소유 세션 누락 검사를 1개 추가했다. 실제 HTTP 피드백·구독 회귀 테스트와 기능 경계 검사도 함께 실행했다.
- `./gradlew spotlessApply check` 통과: 총 1,216개, 성공 1,210개, 실패·오류 0개, 환경 조건 생략 6개. 운영 DB·외부 AI·배포 검증은 수행하지 않았다.
- develop 마이그레이션은 V104까지이며 후속 LAN-461/V105, LAN-491/V106, LAN-494/V107과 충돌하지 않는다. LAN-488의 리소스·DB 마이그레이션은 변경하지 않았다.
- 원격 갱신 전 HEAD는 `backup/LAN-488-before-develop-20260916`에 보존했다. 후속 PR은 LAN-488 → LAN-461 → LAN-491 → LAN-494 순서로 각 직전 브랜치를 기준으로 유지한다.


## 2026-09-17 develop rebase와 새 알림 경계

- 기준은 `origin/develop`의 `017bd552`다. 프로모션 구독 처리, `isTrial`, 체험 종료 알림·관리자 이메일, 이메일 배너·관리 링크, HTTP 지표 histogram 축소를 보존했다.
- 사용자 승인에 따라 새 알림 코드를 `notification.job`, `notification.email`, `notification.job.admin`으로 배치했다. 관리자 테스트 접수·설정 변경은 `AdminNotificationJobService`, 일반 예약·선점·발송은 job의 Service가 소유한다.
- 발송 수신자 검증은 email의 `EmailRecipient`를 사용해 관리자 요청 DTO 의존을 제거했다. 기존 검증 어노테이션은 동일하다. 알림 대상 조회는 `ProfileSubscriptionService`로 옮겼다.
- 기존 job/admin 메서드 14개의 서명과 본문을 비교해 오류 enum 소유권을 제외한 동작이 같음을 확인했다. 저장소·예약 Service·이메일 템플릿·SES 구현·관리자 API 문서·구독 응답·이벤트 enum도 package/import/공백 외 내용이 동일하다.
- `./gradlew spotlessApply check` 통과: 총 1,243개, 성공 1,237개, 실패·오류 0개, 환경 조건 생략 6개. 기능 경계와 새 알림·관리자 HTTP·구독·지표 회귀 테스트를 포함한다.
- build.gradle과 src/main/resources 전체가 최신 develop과 동일하다. 배포된 체험 알림 V105~V107과 원격에서 이미 조정된 후속 V108·V109·V110을 보존한다. 운영 DB·SES·EventBridge 실호출·서버 배포 검증은 수행하지 않았다.
- 시작 시점의 네 원격 HEAD를 `backup/LAN-{488,461,491,494}-before-develop-20260917`에 보존했다. 후속 PR은 각 직전 브랜치 위로 rebase한다. 이번 검증은 최종 브랜치 기준이며 중간 커밋 전체의 재컴파일은 수행하지 않았다.

- 후속 LAN-494 전체 검증에서 `subscription → notification → learning.review → subscription` 순환 의존을 발견했다. 구독이 `SubscriptionChangedEvent`를 발행하고 알림의 `SubscriptionTrialReminderService`가 동기 수신하도록 변경했다. 발행 위치는 기존 구독 갱신·이전 성공 분기와 같고 알림 예약도 기존 트랜잭션에 참여한다.
- 웹훅 재전송 시 예약 2건만 유지하는 기존 통합 테스트에 트랜잭션 종료·롤백 후 구독 이력과 예약이 함께 사라지는 검증을 추가했다. 후속 브랜치에도 수정한 기반을 다시 반영한다.


## 2026-09-18 시나리오 피드백 테스트 가독성 정리

- 피드백 API 통합 테스트에서 최초 생성, 재조회 재사용, 이력·진행도 보존, 수준 평가 재생성 방지, 수준 평가 결과 저장을 별도 테스트로 분리했다. 반복 HTTP 요청과 DB 조회는 이름 있는 보조 메서드로 추출하고 기대 결과는 테스트 본문에 유지했다.
- 메시지 피드백 실패 4종은 동일한 두 질문 시나리오 준비를 공유한다. 요청 실패, 잘못된 상태, 메시지 ID 불일치, 세션 ID 불일치는 독립된 테스트와 한국어 설명을 유지한다.
- 상세 피드백 공개 정책은 도입 전, 프리미엄, 도입 전 시작 세션, 무료 예약 부재, 첫 완료, 재완료, 다른 시나리오, 소유 세션 누락의 8개 테스트로 구분했다.
- AI 최종 피드백 계약은 요청 필드, 요약·수준 평가 응답, 메시지별 응답 변환으로 분리했다. 메시지 피드백의 HTTP 응답 준비는 공유하고 JSON 계약·오류·시간 제한 검증을 유지했다.
- 가장 길었던 생성·재조회 테스트는 125줄에서 5개 테스트로 분리했고, 기존 이름의 재조회 테스트는 15줄이다. 사용자 선발화 테스트는 95줄에서 피드백과 수준 평가로 분리해 피드백 본문을 33줄로 줄였다. 줄 수는 메서드 선언부터 닫는 괄호까지이며 보조 메서드는 별도다.
- 기존 검증 항목을 옮긴 결과 테스트 메서드가 11개 늘었다. 수준 평가를 삭제한 뒤 재조회하는 조건에서도 AI 재호출·중복 저장·이력 변경 방지 검증을 유지한다. 애플리케이션 코드와 DB 스키마 변경은 없다.
- 수정 전 관련 테스트 132개와 분리 후 143개가 모두 통과했다. 최종 `./gradlew spotlessApply check`는 총 1,255개 중 1,249개 성공, 실패·오류 0개, 기존 환경 조건에 따른 생략 6개로 통과했다. 중간 Checkstyle의 변수 선언 거리 위반 5곳은 재조회 전 상태와 fixture 값을 `final`로 선언해 해결했다.
- 전체 테스트 메서드 1,168개에 한국어 `@DisplayName`이 있고 파일 내 중복 설명은 없다. `git diff --check`도 통과했다. 운영 DB·실제 AI·배포는 검증하지 않았다.
