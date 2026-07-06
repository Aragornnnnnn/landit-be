# 백엔드 아키텍처

이 문서는 Landit 백엔드에 기능을 추가할 때 따를 아키텍처 기준입니다.
현재 코드는 초기 골격 단계이므로, 아래 내용은 이미 모두 구현된 구조가 아니라 앞으로의 개발 기준입니다.

## 한 줄 결정

초기 프로덕션은 `ECS Fargate API 서버 + ECS Fargate Worker + Supabase Postgres + SQS + S3` 구조로 시작합니다.
서비스 내부 코드는 모듈러 모놀리스와 가벼운 헥사고날 아키텍처를 함께 사용합니다.

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

## 서비스 내부 구조

Landit 백엔드는 하나의 코드베이스에서 시작합니다.
기능 경계는 테이블명이 아니라 사용자 기능과 비즈니스 흐름 기준으로 나눕니다.

기능 모듈은 아래 패키지 구조를 따릅니다.

```text
com.landit.landitbe
  <feature>
    api
    application
    domain
    infrastructure
  common
```

예시 기능 경계는 인증, 학습 세션, 시나리오, 피드백, 복습, 알림처럼 사용자가 이해할 수 있는 업무 단위입니다.
`user`, `session`, `message` 같은 테이블명만 보고 모듈을 만들지 않습니다.

## 레이어 역할

| 레이어 | 역할 |
| --- | --- |
| `api` | HTTP 요청과 응답 처리 |
| `application` | 유스케이스 실행, 트랜잭션 관리, 외부 의존성 호출 조율 |
| `domain` | 핵심 비즈니스 규칙 |
| `infrastructure` | DB, 큐, 파일 저장소, 외부 API 연동 |

의존성 방향은 아래 기준을 따릅니다.

```text
api -> application -> domain
infrastructure -> application/domain
```

핵심 규칙은 단순합니다.

- `domain`은 DB, 큐, 외부 API를 몰라야 합니다.
- `application`은 비즈니스 흐름을 조율합니다.
- `infrastructure`는 실제 기술 구현을 담당합니다.
- API 응답 DTO와 DB Entity를 직접 섞지 않습니다.

## UseCase와 Service 기준

`UseCase`는 하나의 사용자 행동이나 중요한 업무 흐름을 표현할 때 사용합니다.
상태 변경이 있거나, 여러 단계를 조율하거나, 트랜잭션 경계가 중요한 기능은 UseCase로 둡니다.

| 구분 | 사용 기준 | 예시 |
| --- | --- | --- |
| UseCase | 상태 변경, 여러 단계 조율, 트랜잭션 경계가 중요한 기능 | `StartSessionUseCase`, `SendMessageUseCase`, `CompleteSessionUseCase`, `GenerateFeedbackUseCase` |
| Service | 단순 조회, 관리성 기능, 여러 UseCase에서 공유하는 보조 로직 | `ScenarioQueryService`, `UserProfileService`, `AppVersionService` |

단순 목록 조회까지 전부 UseCase로 만들지 않습니다.
파일 수만 늘어나고 얻는 이점이 작기 때문입니다.

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

## 하지 않는 것

- 모든 Repository를 기계적으로 인터페이스로 감싸지 않습니다.
- 단순 조회 기능까지 억지로 UseCase로 만들지 않습니다.
- 아직 필요하지 않은 Gradle 멀티 모듈 구조를 먼저 만들지 않습니다.
- AI 호출이나 파일 처리처럼 오래 걸리는 작업을 사용자 API 응답 경로에 직접 묶지 않습니다.
- 현재 ERD가 바뀔 수 있으므로 테이블 구조를 기준으로 아키텍처를 고정하지 않습니다.
