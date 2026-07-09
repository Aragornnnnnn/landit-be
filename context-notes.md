# 초기 BE 설정 컨텍스트 노트

## 2026-06-28

- 저장소는 `LICENSE`만 있는 거의 빈 Git 저장소로 확인했다.
- 빌드 도구는 명시되지 않았으므로 Spring Boot 초기 프로젝트의 일반적인 선택지인 Gradle Groovy DSL을 사용한다.
- 기본 패키지는 `com.landit.landitbe`로 정한다. `artifactId`의 하이픈을 Java package에 직접 쓸 수 없기 때문이다.
- Spring Initializr metadata 기준 기본 Boot 4 버전은 `4.1.0.RELEASE`이지만, `springdoc-openapi` dependency id가 `[3.5.0.RELEASE,4.1.0.M1)`까지만 지원되어 Spring Boot는 `4.0.7`로 고정한다.
- Maven metadata 기준 Spring Boot 4 BOM 실제 배포 버전은 `.RELEASE` 접미사 없이 `4.0.7`, `4.1.0` 형식이다.
- Spring Scheduler는 별도 starter가 아니라 Spring Framework의 scheduling 기능이므로 애플리케이션 진입점에 `@EnableScheduling`을 붙여 초기 활성화한다.
- Logback은 Spring Boot starter logging에 포함되는 기본 로깅 구현이라 별도 의존성을 추가하지 않는다.
- Sentry Spring Boot starter `8.16.0`은 Boot 4.0.7에서 제거된 `org.springframework.boot.web.client.RestClientCustomizer`를 참조해 컨텍스트 부팅에 실패한다.
- Boot 4 요구를 유지하기 위해 Sentry는 `io.sentry:sentry-logback:8.16.0`과 `logback-spring.xml` appender로 연동한다.
- Initializr가 Boot 4.0.7 조합에서 springdoc-openapi `3.0.2`를 생성했으므로, 별도 springdoc 버전 override는 추가하지 않는다.
- H2는 사용 목적이 테스트이므로 runtime dependency가 아니라 `testRuntimeOnly`로 제한한다.
- `./gradlew test`는 Sentry를 Logback appender로 바꾼 뒤 통과했다.
- Initializr가 만든 `HELP.md`는 `.gitignore` 대상이어서 커밋되지 않는 생성 문서로 남지 않도록 제거했다.
- 초기 설정은 하나의 논리 단위로 커밋한다.

## 2026-06-28 DB 연결 설정

- 사용자가 이번 작업은 이슈 번호 없이 진행하라고 명시해 AGENTS.md의 이슈 번호 요구는 예외로 처리한다.
- 저장소에는 SSM Parameter Store를 직접 조회하는 코드나 의존성이 없다.
- 기존 `application.yml`은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수를 읽지만 로컬 기본값을 포함하고 있었다.
- SSM 값은 애플리케이션에서 직접 읽지 않고 배포/IaC 단계에서 환경변수로 주입하는 방향을 유지한다.
- `DB_URL`에는 이미 `sslmode=require`와 `prepareThreshold=0`이 포함된 값을 주입받는 전제이므로 애플리케이션에서 JDBC URL을 재조립하지 않는다.
- 시크릿 값은 저장소, 로그, 테스트 출력, 문서에 남기지 않는다.
- `application-local.yml`, `application-develop.yml`, `application-prod.yml`은 모두 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 플레이스홀더만 읽도록 분리한다.
- `.env.example`은 실제 로딩 파일이 아니라 로컬 환경변수 설정을 위한 시크릿 없는 예시로 둔다.
- `./gradlew test`로 profile 설정 placeholder 검증과 기존 컨텍스트 부팅을 확인했다.

## 2026-06-28 시간대 설정

- 저장소에는 기존 timezone 설정이 없었다.
- 서버 내부 기본 시간대와 JSON 직렬화 시간대를 `Asia/Seoul`로 맞춘다.
- `./gradlew test`로 애플리케이션 컨텍스트 부팅 후 JVM 기본 timezone이 `Asia/Seoul`인지 확인했다.

## 2026-07-04 LAN-43 dev 배포 워크플로우

- 사용자가 Notion 이슈 번호 `LAN-43`을 제공해 `feat/LAN-43` 브랜치에서 작업한다.
- 저장소는 Gradle 기반 Spring Boot 서버이며 기존 Dockerfile과 `.github/workflows`는 없었다.
- dev 배포는 개발자가 GitHub Actions 화면에서 직접 실행하도록 `workflow_dispatch`만 둔다.
- AWS 인증은 static key 없이 GitHub OIDC를 사용하고, role ARN은 `develop` GitHub Environment의 variable 또는 secret `AWS_ROLE_ARN`에서 받는다.
- `develop` 워크플로우는 GitHub Environment `develop`에 설정된 `AWS_ACCOUNT_ID`, `AWS_REGION`, `ECR_REGISTRY`, `ECR_REPOSITORY`, `ECS_CLUSTER`, `ECS_SERVICE`, `HEALTH_CHECK_URL`을 읽는다.
- GitHub Environment 변수는 job의 `environment`가 정해진 뒤 읽히도록 job-level `env`에 둔다.
- Terraform task definition이 `latest` 이미지를 보므로 워크플로우에서는 task definition 재등록 없이 ECR push 후 ECS `update-service --force-new-deployment`만 수행한다.
- 현재 dev ECS desired count가 0일 수 있으므로, desired count가 0이면 service stable wait와 health check는 건너뛴다.
- desired count가 1 이상일 때 health check를 하려면 외부 접근 가능한 URL이 필요하므로 GitHub variable 또는 secret `HEALTH_CHECK_URL`로 받는다.
- SSM parameter 값과 런타임 시크릿은 워크플로우에서 조회하거나 출력하지 않는다.
- 워크플로우 YAML parse, `git diff --check`, `./gradlew test`는 통과했다.
- 로컬 환경에 Docker CLI가 없어 Docker image build는 실행하지 못했다.

## 2026-07-04 LAN-43 prod 배포 워크플로우

- prod 배포도 개발자가 GitHub Actions 화면에서 직접 실행하도록 `workflow_dispatch`만 둔다.
- GitHub Actions 수동 실행 화면에서 브랜치를 선택할 수 있으므로, prod 워크플로우는 첫 step에서 `GITHUB_REF=refs/heads/main`을 확인하고 아니면 즉시 실패시킨다.
- AWS 조회 결과 현재 계정에는 `develop-landit-cluster`, `develop-landit-api`, `develop-landit-worker`만 보이고 prod ECS/ECR 리소스는 아직 보이지 않았다.
- prod 워크플로우는 하드코딩된 리소스명 대신 GitHub Environment `prod`의 `AWS_ACCOUNT_ID`, `AWS_REGION`, `ECR_REGISTRY`, `ECR_REPOSITORY`, `ECS_CLUSTER`, `ECS_SERVICE`, `HEALTH_CHECK_URL`을 읽는다.
- prod role ARN은 `prod` GitHub Environment의 variable 또는 secret `AWS_ROLE_ARN`에서 받는다.
- develop/prod 워크플로우 YAML parse, `git diff --check`, `./gradlew test`는 통과했다.
- develop 워크플로우 재실행에서 Docker image push는 성공했지만 GitHub OIDC role에 `ecr:DescribeImages` 권한이 없어 ECR 조회 검증 step이 실패했다.
- 이미지 push 자체가 실패를 반환하므로, 배포 진행을 막는 별도 ECR 조회 검증 step은 제거한다.
- SSM parameter 값과 런타임 시크릿은 워크플로우에서 조회하거나 출력하지 않는다.

## 2026-07-06 LAN-66 Agent 개발용 아키텍처 문서화

- 사용자가 Notion 이슈 번호 `LAN-66`을 제공해 `feat/LAN-66` 브랜치에서 작업한다.
- 에이전트가 매번 참고해야 하는 구현 규칙은 `AGENTS.md`에 짧게 둔다.
- 아키텍처 선택 배경, 흐름, 역할 설명은 `docs/architecture/backend.md`로 분리한다.
- `README.md`는 실행 문서 성격을 유지하고 상세 문서 링크만 추가한다.
- 이번 작업은 문서 변경이므로 검증은 테스트 대신 링크와 Git diff 검토로 진행한다.

## 2026-07-06 LAN-66 문서 점검 후속 수정

- 문서 감사에서 health check 변수명과 실제 워크플로우 설정이 어긋난 것을 확인했다.
- `README.md`에서 주요 문서와 작업 로그를 바로 찾을 수 있도록 링크를 보강한다.
- `landit-be`의 현재 배포 워크플로우는 API 서버만 다루므로, Worker 구현과 배포 소유 경계를 아키텍처 문서와 `AGENTS.md`에 명시한다.
- 문서만 변경하므로 검증은 링크, YAML 문법, Git diff, 표기 검색으로 진행한다.

## 2026-07-06 LAN-58 공통 응답과 예외 처리 체계

- 사용자가 Notion 이슈 번호 `LAN-58`에 해당하는 작업 브랜치 생성을 요청해 `origin/develop` 기준 `feat/LAN-58`에서 작업한다.
- SayNow BE의 핵심 응답 체계는 `ApiResponse<T>`와 `ErrorResponse`이고, 성공 응답은 `success=true`, 실패 응답은 `success=false`와 `error.code/message`를 사용한다.
- SayNow BE의 예외 처리 핵심은 `ApiException`, `ErrorCode`, `GlobalExceptionHandler`다.
- Landit BE는 아직 컨트롤러와 공통 응답/예외 코드가 없는 초기 골격 상태다.
- 이번 변경은 응답/예외 처리 체계만 가져오고, SayNow의 인증/AI/시나리오/NPS 도메인별 `ErrorCode`와 Sentry reporter 추상화는 가져오지 않는다.
- Landit은 이미 Logback Sentry appender를 사용하므로, 별도 Sentry SDK reporter 포트는 실제 필요가 생길 때 추가한다.
- 새 테스트는 구현 전 `ErrorCode`, `ApiResponse`, `GlobalExceptionHandler` 부재로 컴파일 실패했고, 최소 구현 후 관련 테스트와 `./gradlew test`가 통과했다.

## 2026-07-06 LAN-58 Sentry 예외 전송 정책

- 사용자가 Sentry 전송 방향을 승인해, SayNow처럼 reporter 경계는 두되 Landit 정책은 더 좁게 잡는다.
- 클라이언트 흐름에 해당하는 4xx `ApiException`과 검증 예외는 Sentry event로 보내지 않는다.
- 5xx `ApiException`과 예상하지 못한 예외만 Sentry event로 보낸다.
- 새 환경변수는 필요 없다. 기존 `SENTRY_DSN`, `SENTRY_ENVIRONMENT` 설정을 그대로 사용한다.
- `SENTRY_TRACES_SAMPLE_RATE`는 현재 `sentry-logback` 기반 예외 전송 경로에서 사용하지 않으므로 활성 설정과 `.env.example`에서 제거한다.
- 핸들러에서 직접 capture하는 예외는 `log.error`를 함께 남기지 않아 Logback Sentry appender와의 중복 전송 가능성을 줄인다.
- `GlobalExceptionHandlerTests`는 구현 전 `SentryEventReporter` 부재로 컴파일 실패했고, reporter 경계와 기본 구현을 추가한 뒤 관련 테스트가 통과했다.
- 전체 검증으로 `./gradlew test`를 실행해 새 Sentry reporter bean이 애플리케이션 컨텍스트에 등록되는 것까지 확인했다.

## 2026-07-06 LAN-55 OIDC 소셜 로그인

- 사용자가 Notion 이슈 번호 `LAN-55`에 해당하는 작업 브랜치 생성을 요청해 `origin/develop` 기준 `feat/LAN-55`에서 작업한다.
- SayNow BE의 `POST /api/v1/auth/social-login`은 `provider`, `idToken`, `nonce`를 받고 OIDC ID Token 검증 후 자체 access/refresh token을 발급한다.
- Landit BE에는 아직 인증 도메인, 사용자 테이블, token 설정, SecurityFilterChain이 없다.
- 이번 범위는 소셜 로그인 API 1개와 그 API가 발급하는 access/refresh token 저장 경계까지로 제한한다. refresh, logout, withdraw API는 별도 이슈가 생기면 추가한다.
- nonce는 선택값으로 두지 않는다. 요청 nonce가 없거나 ID Token claim nonce와 일치하지 않으면 `OIDC_NONCE_MISMATCH`로 거부한다.
- 실제 런타임에는 `LANDIT_AUTH_TOKEN_SECRET`, `LANDIT_AUTH_OIDC_GOOGLE_AUDIENCES`, `LANDIT_AUTH_OIDC_KAKAO_AUDIENCES`가 필요하다. 테스트는 fake OIDC verifier를 사용하므로 실제 값이 필요하지 않다.
- 자체 refresh token은 난수 기반 opaque token으로 발급하고 원문을 저장하지 않으며 SHA-256 해시만 저장한다.
- access token에는 같은 초 안에 재로그인이 발생해도 토큰 값이 고유하도록 JWT payload에 `jti`를 포함한다.
- 원격 OIDC 검증은 Google과 Kakao JWKS를 사용하고, Google issuer 별칭과 5분 clock skew, JWKS 캐시를 적용한다.
- `./gradlew test --tests 'com.landit.landitbe.auth.SocialAuthApiIntegrationTests'`로 소셜 로그인, 신규 사용자 생성, 기존 사용자 재로그인, nonce 누락과 불일치 거부, 미지원 provider 거부를 확인했다.
- SSM `/landit/develop`, `/landit/prod`에는 `LANDIT_AUTH_OIDC_APPLE_AUDIENCES`, `LANDIT_AUTH_OIDC_GOOGLE_AUDIENCES`, `LANDIT_AUTH_OIDC_KAKAO_AUDIENCES`가 생성되어 있다.
- Apple도 같은 소셜 로그인 API에서 지원한다. Apple ID Token 검증은 issuer `https://appleid.apple.com`, JWKS `https://appleid.apple.com/auth/keys`, configured audience, nonce 필수 검증을 사용한다.
- 현재 `RemoteOidcTokenVerifier`는 provider별 switch가 `jwksUri`, issuer, audience, nickname claim에 흩어져 있어 SayNow 구현보다 덜 깔끔하다.
- 리팩터링은 동작 변경 없이 SayNow처럼 provider별 issuer, JWKS URI, audience, nickname claim을 `ProviderSettings` 한 곳에 모으는 방향으로 진행한다.
- 리팩터링 후 `./gradlew test --tests 'com.landit.landitbe.auth.SocialAuthApiIntegrationTests'`, `./gradlew test`, enum completeness 검색과 diff 리뷰를 실행했다.
- 사용자 피드백에 따라 LAN-55 범위를 소셜 로그인 API 1개에서 SayNow의 인증 생명주기 전체로 확장한다.
- SayNow 기준으로 refresh는 refresh token을 한 번 쓰면 폐기하고 새 access/refresh token을 발급하는 rotation 방식이다.
- logout은 전달받은 refresh token을 찾으면 폐기하고, 찾지 못해도 성공 응답을 반환하는 멱등 방식이다.
- withdraw는 현재 access token으로 인증된 사용자를 탈퇴 처리하고 해당 사용자의 활성 refresh token을 모두 폐기한다.
- SayNow의 withdraw는 `provider`, `sub`, `nickname`, `email`을 비우고 `deletedAt`을 남겨 unique 제약과 개인정보 보관 범위를 함께 정리한다.
- Landit도 기존 `uk_users_provider_sub` unique 제약을 유지하므로 탈퇴 시 소셜 연결 값을 비우는 방식으로 맞춘다.
- access token 인증은 controller가 직접 토큰을 파싱하지 않고 `OncePerRequestFilter`에서 Bearer token을 검증해 `AuthUserPrincipal`을 SecurityContext에 넣는 방식으로 구현한다.
- 리뷰 중 refresh와 withdraw가 엇갈릴 수 있는 경계를 확인해 refresh token 조회에는 `PESSIMISTIC_WRITE` 잠금을 걸고, refresh 발급 전에 사용자 `deletedAt`도 확인한다.
- 탈퇴 사용자에게 남은 refresh token row가 있어도 재발급을 거부하는 회귀 테스트를 추가했다.
- 최종 검증으로 `./gradlew test --tests 'com.landit.landitbe.auth.SocialAuthApiIntegrationTests'`, `git diff --check`, `./gradlew test`를 실행했다.

## 2026-07-07 LAN-55 DBML Entity와 Flyway 전환

- 사용자가 DBML 구조를 유지하기로 결정해 기존 `users/refresh_tokens` 인증 저장 구조를 `user_profile/oauth_identity/refresh_token` 구조로 전환한다.
- `oauth_identity`는 탈퇴나 연결 해제 row를 보존하기 위해 일반 unique 대신 ACTIVE 상태 row에만 partial unique index를 둔다.
- `session_history_message_feedback`에서 `user_learning_expression_id`를 제거하고, `user_learning_expression.session_history_message_feedback_id` 단방향 참조만 유지한다.
- DBML이 직접 표현하지 못하는 check constraint와 partial unique index는 Flyway 마이그레이션에서 관리한다.
- `jsonb` 컬럼은 Entity에서 Jackson `JsonNode`와 Hibernate `@JdbcTypeCode(SqlTypes.JSON)`로 매핑한다.
- 기존 마이그레이션은 이미 논리 이력이 있으므로 수정하지 않고 새 `V4__apply_dbml_schema.sql`에서 DBML 스키마로 전환한다.
- 인증 흐름에서 실제 탐색이 필요한 `UserProfile`, `OauthIdentity`, `RefreshToken`만 JPA 객체 관계로 두고, 나머지 도메인 Entity는 FK를 `Long` 컬럼으로 매핑한다.
- H2는 PostgreSQL partial unique index 문법을 지원하지 않아 공통 Flyway 위치에는 일반 조회 index만 두고, PostgreSQL 전용 partial unique index는 `db/postgresql` 위치로 분리한다.
- `spring.flyway.locations`는 `classpath:db/migration,classpath:db/{vendor}`로 설정해 H2 테스트에서는 PostgreSQL 전용 migration이 실행되지 않게 한다.
- `./gradlew test --tests 'com.landit.landitbe.DatabaseSchemaIntegrationTests'`로 DBML 핵심 테이블, 조회 index, feedback-expression 역참조 제거, PostgreSQL 전용 partial unique migration 파일 존재를 확인했다.
- `./gradlew test --tests 'com.landit.landitbe.auth.SocialAuthApiIntegrationTests'`로 기존 소셜 로그인, refresh, logout, withdraw API 동작이 새 저장 구조에서도 유지되는 것을 확인했다.
- DBML의 나머지 테이블은 서비스 로직이 아직 없으므로 JPA 객체 관계를 무리하게 만들지 않고 FK ID를 `Long`으로 매핑했다.
- 콘텐츠, 세션, 복습, 퀘스트, 캐릭터, 푸시, 앱 버전 Entity와 enum을 추가했다.
- `jsonb` 컬럼은 `JsonNode`와 `@JdbcTypeCode(SqlTypes.JSON)` 조합으로 매핑했고, H2 테스트의 Hibernate validate도 통과했다.
- Entity 추가 후 `./gradlew test --tests 'com.landit.landitbe.DatabaseSchemaIntegrationTests'`와 `./gradlew test --tests 'com.landit.landitbe.auth.SocialAuthApiIntegrationTests'`가 통과했다.
- 최종 검증으로 `git diff --check`와 `./gradlew test`를 실행했고 둘 다 통과했다.
- 작업은 `feat: DBML 사용자 스키마와 인증 저장 구조 전환`, `feat: DBML 도메인 엔티티 매핑 추가`로 나누어 커밋했다.

## 2026-07-07 LAN-66 NPS 응답 Entity 수정

- 사용자가 NPS를 세션 종속 기능이 아닌 별도 기능으로 변경하기로 했다.
- API 경로는 `POST /api/v1/nps` 기준으로 보며, NPS 응답은 `learning_session_id`를 받지 않는다.
- 같은 사용자의 중복 제출은 허용하므로 `user_profile_id` unique 제약은 두지 않는다.
- 기존 `session_nps_response`는 `nps_response`로 바꾸고 `user_profile_id`, `score`, `opinion_text`, `created_at`만 둔다.
- `DatabaseSchemaIntegrationTests.npsResponseIsUserBoundAndAllowsDuplicateSubmissions`는 구현 전 `nps_response` 테이블 부재로 실패했고, Entity와 Flyway 수정 후 통과했다.
- 최종 검증으로 `git diff --check`와 `./gradlew test`를 실행했고 둘 다 통과했다.

## 2026-07-07 LAN-66 Flyway V4 checksum 복구

- `d732b34`에서 이미 DB에 적용된 `V4__apply_dbml_schema.sql`이 직접 수정되어 ECS 새 task가 Flyway checksum mismatch로 부팅 실패했다.
- 이미 적용된 migration은 수정하지 않는 원칙을 적용해 V4의 NPS 테이블 정의는 기존 `session_nps_response`로 되돌린다.
- 세션 종속 NPS 응답을 사용자 기준 `nps_response`로 바꾸는 작업은 새 `V6__replace_session_nps_response.sql`에서 처리한다.
- V6는 기존 `session_nps_response` 데이터를 `learning_session.user_profile_id`로 매핑해 `nps_response`로 이전한 뒤 기존 테이블을 제거한다.

## 2026-07-07 ECS 배포 검증 fail-fast 개선

- 사용자가 `origin/develop` 직접 수정을 요청해 별도 이슈 브랜치 없이 `develop`에서 작업한다.
- 기존 `Verify ECS service`는 `aws ecs wait services-stable`이 완료될 때까지 중간 상태와 ECS 이벤트를 충분히 보여주지 못했다.
- `Verify ECS service`는 최대 10분 동안 15초 간격으로 service 상태를 출력하고, PRIMARY deployment가 `FAILED`가 되면 최근 ECS 이벤트를 출력한 뒤 즉시 실패한다.
- step-level `timeout-minutes`는 12분으로 둔다. 루프가 직접 10분 실패를 반환하고 이벤트를 출력할 시간을 남기기 위해서다.
- API 서버 workflow의 `HEALTH_CHECK_URL` 검증과 curl health check는 ECS service 안정화 뒤 그대로 유지한다.
- workflow만 변경했으므로 애플리케이션 테스트 대신 GitHub Actions YAML parse와 `git diff --check`로 검증한다.

## 2026-07-07 ERD 기준 Entity 동기화

- 사용자가 이슈 번호 없이 바로 시작하라고 명시해 이번 작업도 이슈 번호 요구를 예외 처리한다.
- 작업 기준은 `origin/develop`이고, 로컬 `develop`을 직접 오염시키지 않기 위해 `feat/erd-entity-sync` 브랜치를 생성한다.
- 붙여넣은 ERD와 현재 Entity 차이는 `scenario.total_question_count`, `writing_expression.representative_sentence_translation_highlight_text`, `user_writing_expression_completion.scenario_id`다.
- `writing_expression.representative_sentence_text`와 `representative_sentence_translation`은 사용자가 요청한 대로 `text` 매핑을 유지한다.
- 이미 적용된 `V4__apply_dbml_schema.sql`은 checksum 문제를 피하기 위해 수정하지 않고, 새 migration으로 차이를 반영한다.
- `DatabaseSchemaIntegrationTests.erdV2ColumnChangesAreAppliedByLatestMigration`는 구현 전 `scenario.total_question_count` 부재로 실패했고, `V7__sync_erd_v2_columns.sql`과 Entity 수정 후 통과했다.
- 기존 schema 검증은 `./gradlew test --tests 'com.landit.landitbe.DatabaseSchemaIntegrationTests'`로 확인했다.

## 2026-07-07 Swagger 한글 설명 인코딩 수정

- `https://api-develop.landit.im/v3/api-docs`의 `@Schema` 한글 설명이 `怨듯넻 API ...`처럼 깨져 내려왔다.
- 소스 파일의 `ApiResponse`, `ErrorResponse` 한글 설명은 정상 UTF-8이므로 Swagger 설정 문제가 아니라 Java 컴파일 인코딩 경로 문제로 판단한다.
- `build.gradle`에는 JavaCompile `options.encoding` 설정이 없어 빌드 환경 기본 charset이 UTF-8이 아니면 annotation 문자열이 깨져 class 파일에 들어갈 수 있다.
- 해결은 Swagger 설정 추가가 아니라 JavaCompile 인코딩을 `UTF-8`로 고정하는 최소 변경으로 한다.
- develop 배포 워크플로우 실행은 실제 환경 재배포라 명시 승인 없이 진행하지 않는다.
- `https://api-develop.landit.im/v3/api-docs`를 다시 확인했을 때 응답은 `공통 API 응답 객체`, `요청 처리 성공 여부` 등 정상 한글로 내려왔다.

## 2026-07-07 Scenario completion_criteria 제거

- 사용자가 `Scenario` Entity에서 `completion_criteria` 제거를 요청했다.
- `completion_criteria`는 현재 `Scenario` Entity와 이미 적용된 `V4__apply_dbml_schema.sql`에만 남아 있다.
- Entity만 제거하면 DB의 NOT NULL 컬럼 때문에 신규 scenario insert가 깨질 수 있으므로, 기존 V4는 유지하고 새 migration으로 `scenario.completion_criteria`를 제거한다.
- `DatabaseSchemaIntegrationTests.erdV2ColumnChangesAreAppliedByLatestMigration`는 구현 전 `scenario.completion_criteria`가 남아 있어 실패했고, `V8__drop_scenario_completion_criteria.sql`과 Entity 수정 후 통과했다.

## 2026-07-07 OpenAPI JSON charset 명시

- Safari에서 `https://api-develop.landit.im/v3/api-docs`를 직접 열면 한글이 깨져 보였다.
- live 응답 바이트는 UTF-8이지만 응답 헤더가 `content-type: application/json`만 내려와 브라우저 원문 렌더링에서 charset 추론이 틀어질 수 있다.
- JavaCompile UTF-8 고정은 class 파일의 annotation 문자열 보존에는 필요하지만, 브라우저 원문 JSON 표시 문제에는 응답 `Content-Type` charset 명시가 추가로 필요하다.
- `/v3/api-docs`에만 `application/json;charset=UTF-8`을 명시하는 `ResponseBodyAdvice`를 추가한다.

## 2026-07-07 CONTRIBUTING 분리

- 사용자가 별도 이슈 번호 없이 `origin/develop`에서 바로 작업하라고 명시해 이번 문서 정리는 이슈 번호 요구를 예외 처리한다.
- 사람에게 필요한 협업 규칙은 `CONTRIBUTING.md`로 옮기고, `AGENTS.md`에는 에이전트가 작업 중 즉시 따라야 하는 실행 규칙과 참조 링크만 남긴다.
- 이번 변경에서는 스킬을 새로 만들지 않는다. 반복 실행 절차가 실제로 쌓이면 커밋이나 배포 같은 좁은 주제만 별도 스킬로 분리한다.
- 문서 변경만 있으므로 애플리케이션 테스트 대신 `git diff --check`와 변경 파일 diff를 검토한다.

## 2026-07-07 V8 migration 배포 실패 수정

- GitHub Actions run `28850557666` job `85564534014`는 `Verify ECS service` step timeout으로 실패했다.
- ECS task 자체는 `exitCode 1`로 종료됐고, CloudWatch `/landit/develop/api` 로그의 직접 원인은 Flyway `V8__drop_scenario_completion_criteria.sql` 실행 실패다.
- develop DB에는 `scenario.completion_criteria` 컬럼이 이미 없어 `ALTER TABLE scenario DROP COLUMN completion_criteria`가 PostgreSQL `42703`으로 실패했다.
- `V8`은 아직 실제 DB에 성공 적용되지 못했으므로 checksum 문제 없이 `DROP COLUMN IF EXISTS`로 수정한다.
- PRIMARY deployment의 failed task가 있고 running/pending replacement가 없으면 workflow가 timeout까지 기다리지 않고 ECS 이벤트를 출력한 뒤 실패하게 한다.

## 2026-07-07 Flyway와 ECS 배포 분리

- ECS task 부팅 중 Flyway가 실패하면 DB migration 문제가 ECS 배포 안정화 실패처럼 보인다.
- `flyway-migration.yml`을 별도 reusable workflow로 두고 deploy workflow의 선행 job에서 호출한다.
- 새 Gradle task는 Spring context를 띄우지 않고 Flyway API만 직접 호출한다. 따라서 DB 접속 정보 외의 앱 런타임 secret이 필요 없다.
- develop/prod profile에서는 `spring.flyway.enabled=false`로 앱 런타임 migration을 끈다.
- DB 접속 정보는 GitHub Secrets로 복사하지 않고 workflow가 AWS SSM `/landit/{environment}`에서 직접 읽는다.
- live IAM에서 `landit-github-actions-develop-deploy` role에 `/landit/develop/DB_*` `ssm:GetParameter` inline policy를 추가했다.

## 2026-07-07 LAN-59 PR 충돌 정리

- PR #1의 `feat/LAN-59`에는 오래된 Flyway 분리 커밋이 포함되어 현재 `develop`의 `flyway-migration.yml` 선행 job 구조와 충돌했다.
- `origin/develop` 위로 rebase하면서 중복 Flyway 커밋은 건너뛰고, `develop`의 Flyway workflow 구조를 그대로 유지한다.
- `src/main/java/com/landit/landitbe/FlywayMigrationRunner.java`, `.github/workflows/deploy-dev.yml`, `.github/workflows/deploy-prod.yml`는 rebase 후 `origin/develop`과 차이가 없어야 한다.

## 2026-07-07 LAN-79 PR 충돌 정리

- PR #2의 base는 `feat/LAN-59`이고, head `feat/LAN-79`는 rebase 전 LAN-59 히스토리 위에 만들어져 있었다.
- PR #1에서 `feat/LAN-59`를 현재 `origin/develop` 위로 rebase하면서, PR #2에는 예전 LAN-59 커밋과 오래된 Flyway workflow 커밋이 남아 충돌했다.
- `feat/LAN-79`는 LAN-79 전용 커밋 4개만 현재 `origin/feat/LAN-59` 위로 다시 얹는다.
