# AGENTS.md

Landit BE 서버에서 Codex와 다른 코딩 에이전트가 지켜야 할 저장소 규칙입니다. 전역 지침과 함께 적용하되, 이 파일의 프로젝트 규칙을 우선 확인합니다.

## Project Context

- 이 저장소는 Java 21, Spring Boot 4 기반 백엔드 서버입니다.
- 빌드 도구는 Gradle입니다.
- 운영 DB는 PostgreSQL, 테스트 DB는 H2를 사용합니다.
- DB 스키마 변경은 Flyway 마이그레이션으로 관리합니다.
- API 문서화는 springdoc-openapi를 사용합니다.
- 에러 추적은 Sentry, 로깅은 Logback을 사용합니다.

## Architecture Rules

- 백엔드는 모듈러 모놀리스로 시작합니다.
- 기능 모듈은 테이블 기준이 아니라 사용자 기능과 비즈니스 흐름 기준으로 나눕니다.
- 각 기능 모듈은 `api`, `application`, `domain`, `infrastructure` 구조를 따릅니다.
- 상태 변경, 여러 단계 조율, 트랜잭션 경계가 중요한 흐름은 UseCase로 둡니다.
- 단순 조회와 관리성 기능은 Service로 충분합니다.
- AI Provider, SQS, S3, Push, OAuth 같은 외부 의존성만 Port/Adapter로 분리합니다.
- 단순 Repository를 처음부터 전부 Port로 감싸지 않습니다.
- API 서버는 사용자 요청을 빠르게 처리하고, 오래 걸리는 작업은 SQS를 통해 Worker가 처리합니다.
- 현재 이 저장소의 배포 워크플로우는 API 서버만 다룹니다.
- Worker 구현이나 배포를 이 저장소에 추가하기 전에는 `landit-ai` 또는 별도 Worker 저장소와 소유 경계를 먼저 확인합니다.
- 자세한 결정 배경은 `docs/architecture/backend.md`를 참고합니다.

## Workflow

- 사람용 협업 규칙은 `CONTRIBUTING.md`를 따릅니다.
- 작업을 시작할 때는 반드시 Notion 이슈 번호가 필요합니다.
- 브랜치 생성 전 Notion 이슈 번호를 확인합니다.
- 개발자가 작업을 시작할 때 이슈 번호가 없거나 현재 브랜치가 작업 브랜치가 아니라면, 에이전트는 개발자에게 이슈 번호를 먼저 요청합니다.
- 이슈 번호가 확인되기 전에는 기능 구현, 리팩터링, API 변경, DB 변경, 배포 작업을 시작하지 않습니다.
- 작업별 기록은 `docs/tasks/{ISSUE_NUMBER}/` 아래에 둡니다.
- 작업 시작 시 `docs/tasks/{ISSUE_NUMBER}/checklist.md`와 `docs/tasks/{ISSUE_NUMBER}/context-notes.md`를 만들거나 갱신합니다.
- 작업 중 결정 사항은 해당 이슈의 `context-notes.md`에 이어서 기록합니다.
- 여러 단계 작업은 해당 이슈의 `checklist.md`를 갱신하며 진행합니다.
- 사용자가 이슈 번호 없이 `origin/develop` 직접 작업을 명시하면 예외로 처리하고 `docs/tasks/direct-{YYYY-MM-DD}-{short-slug}/`에 기록합니다.
- 새 작업은 루트의 `checklist.md`, `context-notes.md`에 기록하지 않습니다. 두 파일은 과거 누적 로그로만 둡니다.

## Backend Code Convention

- Java 코드는 Google Java Style Guide를 기준으로 합니다.
- 와일드카드 import를 사용하지 않습니다.
- 새 소스 파일의 첫 줄은 파일 역할을 설명하는 한국어 한 줄 주석으로 시작합니다.
- 메서드 설명은 Javadoc `/** ... */`를 사용합니다.
- 메서드 내부의 짧은 보조 설명은 `//`를 사용합니다.
- 이름은 길어져도 역할이 분명하게 작성합니다.
- 하나의 메서드는 하나의 책임만 갖도록 작성합니다.
- 메서드가 20줄을 넘거나 파라미터가 4개를 넘으면 분리 가능성을 먼저 검토합니다.
- DTO 검증은 Bean Validation을 우선 사용합니다.
- DB 변경은 JPA 엔티티 변경과 Flyway 마이그레이션을 함께 검토합니다.
- public API 변경은 테스트와 OpenAPI 문서 갱신 여부를 함께 확인합니다.
- 시크릿 값은 로그, 테스트 출력, 문서, 커밋 메시지에 노출하지 않습니다.

## Testing And Verification

- 코드 변경 후 최소 검증 명령은 `./gradlew test`입니다.
- 테스트를 실행하지 못했다면 이유를 최종 응답에 명확히 적습니다.
- 테스트 실패 시 실제 에러와 스택트레이스를 읽고 원인을 확인한 뒤 수정합니다.
- 시크릿이 필요한 테스트는 실제 값을 출력하지 말고 설정 여부나 guarded equality만 검증합니다.
- 문서만 변경한 경우에는 테스트 대신 변경 파일과 Git diff를 검토합니다.
