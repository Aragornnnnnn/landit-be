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
- [ ] 오류와 네이밍. 기능 ErrorCode 소유권을 정리하고 공통 HTTP 변환을 유지한다. 관리자 페이지 Bean Validation과 기존 오류 응답을 함께 검증한다. 행위가 불분명한 관련 메서드 이름을 정리한다.
- [ ] 경계 검증과 문서. 타 기능 Repository/Entity 참조와 memory→session 역참조의 회귀를 검사한다. 조회 JOIN과 남은 결합의 허용 범위를 아키텍처 문서에 명시한다. 전체 check와 실제 Spring HTTP/DB 통합 테스트, 독립 Sol 리뷰를 통과한 후 논리 단위로 커밋한다.

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
