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
