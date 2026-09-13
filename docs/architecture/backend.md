# 백엔드 아키텍처

이 문서는 Landit 백엔드에 기능을 추가할 때 따를 아키텍처 기준입니다.
현재 코드는 초기 골격 단계이므로, 아래 내용은 이미 모두 구현된 구조가 아니라 앞으로의 개발 기준입니다.

## 한 줄 결정

초기 프로덕션은 `ECS Fargate API 서버 + ECS Fargate Worker + Supabase Postgres + SQS + S3` 구조로 시작합니다.
서비스 내부 코드는 모듈러 모놀리스와 가벼운 헥사고날 아키텍처를 함께 사용합니다.

이 문서는 프로덕션 구성 기준을 설명합니다.
현재 `landit-be` 저장소의 배포 워크플로우는 API 서버만 다룹니다.
Worker 구현과 배포 소유 경계는 `landit-ai` 또는 별도 Worker 저장소와의 역할을 확인한 뒤 문서화합니다.

## 인프라 구성

```text
Mobile App
  -> ALB
  -> ECS API Server
       -> Supabase Postgres
       -> S3
       -> SQS
       -> Sentry, CloudWatch

SQS
  -> ECS Worker
       -> Supabase Postgres
       -> S3
       -> AI Provider
       -> Push Provider
       -> Sentry, CloudWatch

EventBridge Scheduler
  -> SQS
```

| 구성 | 역할 |
| --- | --- |
| ECS API Server | 앱 요청 처리, 인증, 세션 진행, 빠르게 응답해야 하는 API 처리 |
| ECS Worker | 오래 걸리는 작업, 실패 가능성이 높은 작업, 예약 작업 처리 |
| Supabase Postgres | 서비스 메인 DB |
| SQS | API와 Worker 사이의 비동기 작업 큐 |
| S3 | 음성 파일, AI raw payload 같은 파일성 데이터 저장 |
| EventBridge Scheduler | 매일 복습 생성 같은 예약 작업 트리거 |
| Sentry | API와 Worker 예외 추적 |
| CloudWatch | 로그, ECS, ALB, SQS 기본 모니터링 |

## API와 Worker 분리 기준

API 서버는 사용자가 기다리는 요청만 처리합니다.
AI 호출, 피드백 생성, 복습 생성, 푸시 발송처럼 오래 걸리거나 재시도가 필요한 작업은 SQS에 넣고 Worker가 처리합니다.

예를 들어 세션 종료 요청은 API에서 세션 종료 상태를 저장하고 피드백 생성 작업을 SQS에 등록한 뒤 바로 응답합니다.
Worker는 SQS에서 작업을 가져와 세션 메시지를 조회하고, AI Provider를 호출한 뒤 결과를 DB에 저장합니다.

Worker가 어느 저장소에서 구현되고 배포되는지는 기능 착수 전에 먼저 확인합니다.

## 서비스 내부 구조

Landit 백엔드는 하나의 코드베이스에서 시작합니다.
기능 경계는 테이블명이 아니라 사용자 기능과 비즈니스 흐름 기준으로 나눕니다.

큰 기능은 업무별 하위 패키지로 묶고, 각 업무 안에서 필요한 역할만 분리합니다.
작은 기능에는 빈 레이어나 단순 위임 Facade를 만들지 않습니다.

```text
com.landit.landitbe
├── feature
│   ├── content
│   │   ├── scenario
│   │   ├── expression
│   │   └── tutor
│   ├── learning
│   │   ├── expression       # 표현 학습 요청 조율
│   │   ├── progress         # 누적 학습 진행과 완료 이력
│   │   └── access           # 시나리오 복습 권한
│   ├── session
│   │   ├── scenario
│   │   ├── freetalk
│   │   │   ├── expression
│   │   │   ├── memory
│   │   │   └── history
│   │   ├── feedback
│   │   └── history
│   ├── notification
│   │   ├── token
│   │   ├── delivery
│   │   └── scheduled
│   ├── memory
│   ├── profile
│   ├── admin
│   └── audit
├── config
└── shared
```

각 업무 패키지에는 Controller와 필요한 `service`, `repository`, `domain`, `dto`,
`docs`, `client`, `exception`을 둡니다. 같은 업무의 변경 파일을 함께 찾을 수 있게 합니다.
`session` 내부의 하위 패키지는 분류 단위이며 각각 독립 배포 모듈이라는 뜻은 아닙니다.
`learning.expression`은 요청 조율, `learning.progress/access`는 상태 소유 역할을 구분합니다.

## 패키지 역할

| 패키지 | 역할 |
| --- | --- |
| 업무 패키지 루트 | Controller를 두어 HTTP 진입점을 노출 |
| `docs` | Swagger 문서 인터페이스 |
| `dto` | HTTP 요청·응답 및 다른 업무에 공개하는 값 record |
| `domain` | 핵심 비즈니스 규칙 |
| `repository` | JPA Repository와 조회 Projection |
| `service` | 요청 흐름, 트랜잭션과 기능 동작 |
| `client` | AI, OAuth, 큐, 파일 저장소 같은 외부 연동 |
| `exception` | 기능별 예외와 오류 코드 |
| `config` | Spring Bean과 Configuration Properties |
| `shared` | 여러 기능이 실제로 공유하는 기능 독립 코드 |

의존성 방향은 아래 기준을 따릅니다.

```text
Controller
  -> 요청 흐름 Service
    -> Repository 소유 Service
      -> Repository
      -> Entity
    -> 외부 Client

다른 feature
  -> 공개 Service
  -> 공개 record

feature -> shared
config -> feature/shared
```

핵심 규칙은 단순합니다.

- Controller는 Service만 의존합니다.
- Repository와 Entity는 하나의 업무 모듈이 소유하며 해당 모듈의 Service만 직접 접근합니다.
- Service는 다른 기능의 Repository와 Entity를 직접 사용하지 않습니다.
- 다른 기능과는 공개 Service와 record로 통신합니다. Entity와 Repository projection을 넘기지 않습니다.
- 같은 기능 내부의 Service를 Repository마다 한 개로 강제하지 않습니다. 의미 없는 위임 Service나 거대 Service를 만들지 않습니다.
- `shared`는 어떤 `feature`에도 의존하지 않습니다.
- 순수 Entity·Projection 변환은 응답 record의 `from()`이 담당합니다.
- 요청 record에서 Entity를 만들 때는 `toEntity()`를 사용합니다.

## 상태 변경과 조회 결합의 경계

- 콘텐츠는 표현 본문·난이도·활성 상태·시나리오 시작 콘텐츠를 제공합니다.
- 표현 완료 흐름은 `learning.expression`에서 콘텐츠 잠금, 세션 표현 완료, 누적 진행 저장을 조율합니다.
- `session`이 세션 소유권과 완료 상태를 검증하고 세션 Entity를 변경합니다.
- `session → memory`로 기억 생성을 요청합니다. `memory`는 session 타입이나 Repository를 참조하지 않습니다.
- 기억 추출·판정은 트랜잭션 밖에서 수행하고, `persistAndComplete`의 외부 프록시 트랜잭션에서
  사용자 잠금 → 기억 snapshot 재검증·저장 → 세션 잠금·READY 전환을 수행합니다.
- 프로필 조회는 `UserLearningProfile` 등 불변 값을 제공합니다. 잠금 조회는 호출 트랜잭션 종료까지 잠금을 유지합니다.
- 인증 사용자 식별 정보는 `shared.security.AuthUserPrincipal`, 기능 간 감사 기록은 `audit`가 소유합니다.

DB는 아직 하나를 공유합니다. 다음 교차 조회는 명시적으로 허용하지만, 다른 기능의 쓰기 책임은 넘기지 않습니다.

| 조회 경계 | 남아 있는 결합과 이유 |
| --- | --- |
| content의 시나리오/표현 조회 Repository | 사용자 언어·학습 진행을 함께 조회하는 JPQL/SQL. 기존 정렬·필터와 일괄 조회를 유지합니다. |
| session의 메시지 컨텍스트 조회 Repository | 세션에 연결된 시나리오 콘텐츠를 조회합니다. |
| learning.access의 UserScenarioAccessRepository | 과거 미완료 세션 조회에서 session/content를 JOIN합니다. |
| notification.scheduled의 NotificationTargetQueryRepository | 사용자·콘텐츠·진행·세션·스트릭을 페이지 단위로 읽습니다. 사용자별 N+1 조회로 바꾸지 않습니다. |
| memory의 검색/원본 계보 저장 | 공유 DB의 기억 원본 메시지·세션 FK 관계를 유지합니다. |

이 예외는 공유 DB 조회 결합의 목록이며 MSA 분리 완료를 뜻하지 않습니다.
새로운 교차 조회는 이 목록과 소유 경계를 함께 검토합니다.
`FeatureBoundaryTest`는 타 기능 Repository/Entity의 Java import,
content/memory의 session 역참조, shared의 feature 참조를 검사합니다.
SQL 문자열과 모든 런타임 의존성을 검증하는 테스트는 아니므로 JOIN은 별도로 리뷰합니다.

## Service 기준

모든 공개 비즈니스 로직 클래스는 `Service`로 끝냅니다.
`UseCase`, `UseCaseService`, `Finder` 접미사는 사용하지 않습니다.

| Service 종류 | 사용 기준 | 예시 |
| --- | --- | --- |
| 요청 흐름 Service | 사용자 행동과 여러 기능의 협력 순서를 조율 | `ScenarioSessionStartService`, `SessionMessageSubmitService` |
| Repository 소유 Service | 하나의 기능에서 Repository 조회와 상태 변경을 제공 | `UserProfileService`, `LearningSessionService` |

단순 기능은 하나의 Service가 요청 처리와 Repository 소유를 함께 담당할 수 있습니다.
다른 기능의 호출자는 공개 Service를 사용합니다. 같은 업무 내부에서 조회와 변경 Service가 Repository를 함께 쓰는 것은 허용합니다.

## Port와 Adapter 기준

Port와 Adapter는 외부 시스템과 연결되는 부분을 분리하기 위한 장치입니다.
Landit은 AI Provider, SQS, S3, Push Provider, OAuth Provider 같은 외부 의존성이 있으므로 이 부분만 가볍게 감쌉니다.

| 외부 의존성 | Port | Adapter |
| --- | --- | --- |
| 파일 저장소 | `FileStorage` | `S3FileStorage` |
| 메시지 큐 | `MessagePublisher` | `SqsMessagePublisher` |
| AI Provider | `FeedbackGenerator` | `OpenAiFeedbackGenerator` |
| Push Provider | `NotificationSender` | `FcmNotificationSender` |
| OAuth Provider | `OAuthClient` | `KakaoOAuthClient`, `AppleOAuthClient` |

단순 DB Repository를 처음부터 전부 Port로 감싸지 않습니다.
DB는 애플리케이션의 기본 저장소이고, 초기 단계에서 DB 구현체를 자주 바꿀 가능성이 낮기 때문입니다.
나중에 특정 저장 로직이 복잡해지거나 테스트가 어려워지면 그때 Port로 분리합니다.

## 배포와 버전 기록

- 프로덕션 배포 workflow는 `MAJOR.MINOR.PATCH` 버전을 입력받아 Flyway migration을 실행한 뒤 이미지를 ECR에 push하고 ECS service를 갱신합니다.
- ECS service가 안정화된 뒤 workflow는 배포 커밋에 `be-v{버전}` annotated tag와 GitHub Release를 생성합니다.
- 배포가 실패한 커밋에는 태그와 GitHub Release를 생성하지 않습니다.

## 하지 않는 것

- 모든 Repository를 기계적으로 인터페이스로 감싸지 않습니다.
- `UseCase`, `Finder`, `Recorder`, `Loader`처럼 공개 비즈니스 진입점을 여러 접미사로 나누지 않습니다.
- 아직 필요하지 않은 Gradle 멀티 모듈 구조를 먼저 만들지 않습니다.
- AI 호출이나 파일 처리처럼 오래 걸리는 작업을 사용자 API 응답 경로에 직접 묶지 않습니다.
- 현재 ERD가 바뀔 수 있으므로 테이블 구조를 기준으로 아키텍처를 고정하지 않습니다.
