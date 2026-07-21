# LAN-200 백엔드 모듈 경계 및 코드 책임 개선 설계

## 목표

백엔드 패키지와 클래스 이름만으로 API 시작점, 비즈니스 로직, Repository 소유자, 변환 책임과 도메인 간 의존 방향을 파악할 수 있게 구조 규칙을 단순화한다. 이번 이슈에서는 규칙과 목표 구조를 먼저 확정하며 실제 Java 패키지 이동과 리팩터링은 후속 구현 계획에서 단계별로 진행한다.

## 현재 문제

- `api`, `application`, `domain`, `infrastructure` 계층은 존재하지만 실제 코드에서는 Application이 API DTO와 다른 도메인의 Repository·Entity·Projection을 직접 참조한다.
- `session.application`에 `UseCase`, `QueryService`, `Finder`, `Recorder`, `Generator`, `Loader`, `Requester`가 함께 있어 비즈니스 로직 진입점을 이름만으로 찾기 어렵다.
- 같은 Repository 조회, 소유권 확인, 상태 검증과 예외 변환이 여러 클래스에 반복된다.
- Entity와 record 변환이 Service 내부의 `toResponse()`와 생성자 호출로 흩어져 있다.
- 클래스 레벨 `@RequestMapping`과 메서드 Mapping을 함께 봐야 전체 API 경로를 알 수 있다.
- `UserProfile`이 인증과 무관한 학습 설정까지 가지면서 `session`과 `content`가 `auth` 내부 구현에 의존한다.

## 설계 원칙

1. 추상적인 계층 이름보다 `service`, `repository`, `client`, `dto`, `docs`처럼 실제 역할이 드러나는 이름을 사용한다.
2. 모든 공개 비즈니스 로직 진입점은 `Service`로 통일한다.
3. Repository는 해당 도메인의 Service가 소유하며 다른 Service는 Repository를 직접 참조하지 않는다.
4. Entity와 record 사이의 순수 변환은 record가 담당한다.
5. 다른 도메인과는 공개 Service와 record로만 통신한다.
6. Controller에는 전체 API 경로와 HTTP 처리만 남긴다.
7. `common`에는 여러 도메인에서 실제로 공유하는 코드만 둔다.

## 목표 패키지 구조

```text
com.landit.landitbe
├── config
│   ├── ai
│   ├── auth
│   ├── security
│   └── web
├── common
│   ├── domain
│   ├── exception
│   └── response
├── auth
│   ├── AuthController.java
│   ├── docs
│   ├── dto
│   ├── domain
│   ├── repository
│   ├── service
│   ├── security
│   └── exception
├── profile
│   ├── dto
│   ├── domain
│   ├── repository
│   ├── service
│   └── exception
├── content
│   ├── ScenarioController.java
│   ├── ExpressionController.java
│   ├── docs
│   ├── dto
│   ├── domain
│   ├── repository
│   │   └── projection
│   ├── service
│   └── exception
├── learning
│   ├── dto
│   ├── domain
│   ├── repository
│   ├── service
│   └── exception
└── session
    ├── SessionController.java
    ├── ScenarioSessionController.java
    ├── docs
    ├── dto
    ├── domain
    ├── repository
    │   └── projection
    ├── service
    ├── client
    │   └── ai
    └── exception
```

Controller는 기능 패키지 바로 아래에 두어 HTTP 진입점을 노출한다. 기존 `application`의 공개 비즈니스 로직은 `service`, DB 접근은 `repository`, 외부 API 연동은 `client`로 이동한다. 조회 전용 record는 `repository.projection`에 둔다.

## 도메인 소유권

### Auth

- OIDC 검증과 OAuth 연결 정보를 소유한다.
- Access Token과 Refresh Token 발급·폐기를 소유한다.
- Spring Security 필터와 인증 실패 처리를 소유한다.
- 학습 설정과 사용자 프로필은 소유하지 않는다.

### Profile

- `UserProfile`과 활성 상태를 소유한다.
- 닉네임, 이메일, 학습 locale과 선택한 AI 튜터 ID를 소유한다.
- 다른 도메인에는 Entity 대신 목적에 맞는 record를 반환한다.

### Content

- Category, Scenario, ScenarioQuestion, WritingExpression, AiTutor와 TtsVoice를 소유한다.
- 세션이 필요한 콘텐츠는 `ScenarioContentService` 같은 공개 Service로 제공한다.

### Learning

- 시나리오와 표현의 사용자 학습 진행 상태를 소유한다.
- 시작, 재시작, 완료와 잠금 판정은 `LearningProgressService`가 제공한다.

### Session

- LearningSession, ScenarioSession, SessionHistory와 SessionHistoryMessage를 소유한다.
- 메시지별 피드백, 최종 피드백과 AI 대화 요청 흐름을 소유한다.

## Service 규칙

### 명명

- 모든 공개 비즈니스 로직 클래스는 `Service`로 끝낸다.
- `UseCase`, `UseCaseService`, `Finder` 접미사는 사용하지 않는다.
- 사용자 행동을 조율하는 Service는 동작이 드러나는 이름을 사용한다.
  - `ScenarioSessionStartService`
  - `SessionMessageSubmitService`
  - `SessionFeedbackService`
  - `ExpressionLearningCompletionService`
- 공통 도메인 동작을 제공하는 Service는 도메인 명사를 중심으로 이름을 정한다.
  - `UserProfileService`
  - `ScenarioContentService`
  - `LearningProgressService`
  - `LearningSessionService`
  - `SessionHistoryService`
  - `SessionMessageService`

### Repository 소유

- 모든 Repository는 하나의 도메인 Service가 소유한다.
- Controller와 다른 Service는 Repository를 직접 참조하지 않는다.
- Repository가 단순 위임만 제공하더라도 일관된 도메인 접근 경계를 위해 Service를 둔다.
- 여러 Repository가 하나의 Aggregate를 다루면 하나의 Service가 함께 소유할 수 있다.
- 단순 기능은 하나의 Service가 요청 처리와 Repository 소유 역할을 함께 맡을 수 있다.
- 하나의 Service가 관계없는 여러 도메인의 Repository를 소유하지 않는다.

예를 들어 `ScenarioSessionStartService`는 Repository 대신 다음 Service에 의존한다.

```text
ScenarioSessionStartService
├── UserProfileService
├── ScenarioContentService
├── LearningProgressService
├── LearningSessionService
├── ScenarioSessionService
├── SessionHistoryService
└── SessionMessageService
```

Session Repository의 소유자는 다음과 같이 고정한다.

| Repository | 소유 Service |
| --- | --- |
| `LearningSessionRepository` | `LearningSessionService` |
| `ScenarioSessionRepository` | `ScenarioSessionService` |
| `ScenarioSessionStartQueryRepository` | `ScenarioSessionService` |
| `ScenarioSessionMessageQueryRepository` | `ScenarioSessionService` |
| `SessionHistoryRepository` | `SessionHistoryService` |
| `SessionHistoryMessageRepository` | `SessionMessageService` |
| `SessionHistoryMessageFeedbackRepository` | `SessionFeedbackDataService` |
| `SessionHistorySummaryFeedbackRepository` | `SessionFeedbackDataService` |

### 트랜잭션

- 상태 변경 흐름을 시작하는 Service가 트랜잭션 경계를 명확하게 정한다.
- 외부 AI 호출 중에는 DB 트랜잭션과 row lock을 유지하지 않는다.
- 외부 호출 전 조회와 호출 후 저장은 각각 `SessionFeedbackContextService`와 `SessionFeedbackCompletionService`처럼 별도 Service 경계로 분리한다.
- 비동기 결과 저장처럼 독립 트랜잭션이 필요한 동작도 별도 Service 메서드로 분리한다.
- Service를 분리하더라도 기존 메시지 보상 삭제와 처리 상태 전이 계약을 변경하지 않는다.

## Session 클래스 정리 방향

| 현재 클래스 | 목표 클래스 또는 통합 위치 |
| --- | --- |
| `ScenarioSessionStartUseCase` | `ScenarioSessionStartService` |
| `SessionMessageSubmitUseCase` | `SessionMessageSubmitService` |
| `SessionFeedbackUseCase` | `SessionFeedbackService` |
| `SessionEndUseCase` | `LearningSessionService` |
| `LearningSessionFinder` | `LearningSessionService` |
| `SubmittedMessageRecorder` | `SessionMessageService` |
| `GeneratedMessageRecorder` | `SessionMessageService` |
| `SessionInnerThoughtQueryService` | `SessionInnerThoughtService` |
| `SessionInnerThoughtGenerator` | `SessionInnerThoughtService` |
| `SessionInnerThoughtRecorder` | `SessionMessageService` |
| `SessionMessageFeedbackRequester` | `SessionMessageFeedbackService` |
| `SessionMessageFeedbackRecorder` | `SessionMessageService` |
| `SessionFeedbackContextLoader` | `SessionFeedbackContextService` |
| `SessionFeedbackRecorder` | `SessionFeedbackCompletionService` |
| `AiScenarioContextMapper` | 대상 record의 `from()` |

`SessionFeedbackService`는 컨텍스트 조회, AI 호출과 결과 저장 순서만 조율하며 트랜잭션을 시작하지 않는다. `SessionFeedbackContextService`는 각 Repository 소유 Service가 반환한 record를 AI 요청 컨텍스트로 조립한다. `SessionFeedbackCompletionService`는 결과 검증과 상태 변경 트랜잭션을 시작하고, `LearningSessionService`, `SessionHistoryService`, `SessionMessageService`, `SessionFeedbackDataService`, `LearningProgressService`의 상태 변경 메서드를 호출한다.

통합할 때는 클래스 수 감소보다 책임 응집도를 우선한다. 외부 AI 호출, DB 상태 변경과 조회가 하나의 거대한 Service에 섞이면 목적별 Service로 나눈다. 모든 공개 비즈니스 로직 클래스 이름은 `Service`로 통일한다.

## Record 변환 규칙

### 다른 객체에서 record 생성

변환 대상 record에 정적 `from()`을 둔다.

```java
SessionInnerThoughtResponse.from(message);
SessionStartResponse.from(session, scenario, currentMessage);
TtsVoiceResponse.from(projection);
```

### record에서 Entity 생성

입력 record에 `toEntity()`를 둔다.

```java
NpsResponse entity = request.toEntity(userProfileId);
```

### 중첩 record

중첩 record도 자신의 변환을 직접 담당한다.

```java
CurrentMessageResponse.from(message);
SessionProgressResponse.from(session);
MessageFeedbackResponse.from(feedback, context);
```

### 허용 범위

- 필드 선택, 이름 변경, enum 문자열 변환과 nullable 처리를 수행할 수 있다.
- 여러 Entity나 Projection의 값을 조합하는 순수 변환을 수행할 수 있다.
- 변환이 복잡하면 입력들을 Context record로 묶은 뒤 `from(context)`를 사용한다.

### 금지 범위

- Record에서 Repository 또는 외부 API를 호출하지 않는다.
- Record에서 트랜잭션을 시작하지 않는다.
- Record에서 권한과 소유권을 검증하지 않는다.
- Entity에 API DTO를 반환하는 `toResponse()`를 만들지 않는다.
- Service에 필드 매핑을 위한 `toResponse()`와 반복적인 Response 생성자 호출을 두지 않는다.

## Projection 규칙

- JPA 조회 전용 record는 `repository.projection`에 둔다.
- 이름은 `Row` 대신 `Projection`으로 끝낸다.
- Projection은 Entity로 변환하지 않고 조회 결과로만 사용한다.
- 각 필드는 Javadoc으로 의미를 설명한다.

예시는 다음과 같다.

```text
ScenarioSessionStartProjection
ScenarioSessionLockProjection
ScenarioSessionMessageContextProjection
```

## Controller와 API 문서 규칙

- Controller는 기능 패키지 바로 아래에 둔다.
- Controller는 Service만 의존한다.
- 클래스 레벨 `@RequestMapping`은 사용하지 않는다.
- Spring Mapping 애너테이션에는 전체 API 경로를 작성한다.
- Swagger 애너테이션은 `docs` 인터페이스로 분리한다.
- Mapping 애너테이션은 실제 경로가 보이도록 Controller에 유지한다.
- 진행 중인 `feat/LAN-102`의 Swagger 문서 분리 결과를 기준으로 통합한다.

```java
@PostMapping("/api/v1/scenarios/{scenarioId}/sessions")
```

## 설정 규칙

- 설정은 최상위 `config` 아래에서 기능별로 분류한다.
- `config.ai`, `config.auth`, `config.security`, `config.web`처럼 검색 가능한 위치를 사용한다.
- `common`에는 설정 클래스를 두지 않는다.
- 설정 record는 바인딩과 값 정규화만 담당하며 비즈니스 로직을 갖지 않는다.

## 예외 처리 규칙

- 예상 가능한 오류는 실제 실패가 발생한 Service에서 구체적인 도메인 오류로 표현한다.
- 기능별 예외와 오류 코드는 해당 기능의 `exception` 패키지가 소유한다.
- `GlobalExceptionHandler`는 도메인 예외의 HTTP 응답 변환, Spring 요청 검증 예외와 예상하지 못한 예외 처리만 담당한다.
- 같은 예외를 잡아서 그대로 다시 던지는 `try-catch`는 만들지 않는다.
- `RESOURCE_NOT_FOUND`, `INTERNAL_SERVER_ERROR` 같은 범용 오류는 구체적인 오류로 대체할 수 있는 경우 사용하지 않는다.

## 로깅 규칙

- POST, PATCH, DELETE 요청의 상태 변경 결과를 Service 경계에서 기록한다.
- 사용자 ID, 세션 ID와 요청 추적 ID처럼 장애 분석에 필요한 식별 정보를 기록한다.
- Token, 이메일과 사용자 메시지 원문은 그대로 기록하지 않는다.
- Request Body가 필요하면 민감 정보를 제거한 로그용 record 또는 필드만 기록한다.
- 상세 처리 과정은 `DEBUG`, 운영상 주의가 필요한 상태는 `WARN`, 예상하지 못한 장애는 `ERROR`로 기록한다.
- 동일한 실패를 Controller, Service와 Handler에서 중복 기록하지 않는다.

## 테스트 규칙

- Service의 비즈니스 성공·실패 조건은 Mockito 단위 테스트로 검증한다.
- Repository의 JPQL과 Projection은 통합 테스트로 검증한다.
- Controller 통합 테스트는 HTTP 상태, 인증·인가, 요청 검증, 응답 구조와 OpenAPI 계약을 검증한다.
- 모든 비즈니스 테스트를 `@SpringBootTest`로 작성하지 않는다.
- 구조 변경 중 public API, DB 스키마와 AI 요청·응답 계약은 변경하지 않는다.
- 구현 후 최소 검증 명령은 `./gradlew check`다.

## 목표 의존 방향

```text
Controller
  → 요청 흐름 Service
    → 도메인 Service
      → Repository
      → Entity
    → 외부 Client
  → Response record.from(...)

다른 도메인
  → 공개 Service
  → 공개 record
```

다음 의존은 허용하지 않는다.

```text
Controller → Repository
Service → 다른 도메인의 Repository
Service → 다른 도메인의 Entity
Entity → API DTO
Record → Repository
Domain → Controller
```

## 범위 제외

- 이번 설계 문서 작성과 동시에 전체 Java 패키지를 이동하지 않는다.
- Gradle 멀티 모듈과 실제 MSA 분리는 추가하지 않는다.
- DB 스키마, public API 경로와 응답 계약은 변경하지 않는다.
- 새로운 범용 Mapper, Repository Port 또는 추상 인터페이스를 일괄 도입하지 않는다.
- LAN-200과 무관한 기능 로직을 리팩터링하지 않는다.

## 기존 문서와의 정합성

- 이 설계는 기능별 `api/application/domain/infrastructure` 구조와 UseCase·Service 구분을 규정한 현재 `docs/architecture/backend.md`의 해당 결정을 대체한다.
- 구현을 시작하는 첫 변경에서 `docs/architecture/backend.md`와 저장소 `AGENTS.md`의 Architecture Rules를 함께 갱신한다.
- 구현이 완료될 때까지 LAN-200의 구조 변경 범위에서는 이 문서를 기준으로 판단하고, 그 밖의 변경에는 기존 저장소 규칙을 적용한다.

## 완료 판단

- 목표 패키지와 도메인 소유권이 문서에 명확히 정의돼 있다.
- Service 명명과 Repository 소유 규칙에 예외가 없다.
- Record의 `from()`과 `toEntity()` 적용 범위가 구체적으로 정의돼 있다.
- Controller, 설정, 예외, 로깅과 테스트 규칙이 서로 모순되지 않는다.
- 기존 아키텍처 문서와 저장소 지침의 변경 대상이 정의돼 있다.
- 후속 구현 계획에서 기능 단위로 나눌 수 있도록 변경 경계가 정의돼 있다.
