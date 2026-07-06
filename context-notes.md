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

## 2026-07-06 LAN-71 Agent 개발용 아키텍처 문서화

- 사용자가 Notion 이슈 번호 `LAN-71`을 제공해 `feat/LAN-71` 브랜치에서 작업한다.
- 에이전트가 매번 참고해야 하는 구현 규칙은 `AGENTS.md`에 짧게 둔다.
- 아키텍처 선택 배경, 흐름, 역할 설명은 `docs/architecture/backend.md`로 분리한다.
- `README.md`는 실행 문서 성격을 유지하고 상세 문서 링크만 추가한다.
- 이번 작업은 문서 변경이므로 검증은 테스트 대신 링크와 Git diff 검토로 진행한다.

## 2026-07-06 LAN-71 문서 점검 후속 수정

- 문서 감사에서 health check 변수명과 실제 워크플로우 설정이 어긋난 것을 확인했다.
- `README.md`에서 주요 문서와 작업 로그를 바로 찾을 수 있도록 링크를 보강한다.
- `landit-be`의 현재 배포 워크플로우는 API 서버만 다루므로, Worker 구현과 배포 소유 경계를 아키텍처 문서와 `AGENTS.md`에 명시한다.
- 문서만 변경하므로 검증은 링크, YAML 문법, Git diff, 표기 검색으로 진행한다.
