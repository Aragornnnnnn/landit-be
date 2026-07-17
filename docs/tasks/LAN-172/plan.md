# LAN-172 BE 버전 및 릴리즈 관리 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Landit BE 프로덕션 배포에 `MAJOR.MINOR.PATCH` 버전 입력, `be-v{버전}` 태그, GitHub Release 자동 생성을 적용하고 릴리즈·hotfix 운영 규칙을 문서화한다.

**Architecture:** 브랜치·버전·태그 중복을 preflight job에서 먼저 검증하고, 기존 `deploy-prod.yml`의 Flyway, ECR push, ECS 배포, 안정화 검증 순서는 유지한다. 프로덕션 배포가 성공한 뒤에만 workflow가 입력 버전을 `be-v{버전}` annotated tag와 GitHub Release로 기록한다.

**Tech Stack:** GitHub Actions, Bash, Git, GitHub CLI, Markdown.

## Global Constraints

- 애플리케이션 기능 코드와 테스트 코드는 변경하지 않는다.
- AI의 `ai-v{버전}` 대신 BE는 `be-v{버전}` 태그를 사용한다.
- 릴리즈 브랜치는 `release/v{MAJOR}.{MINOR}.{PATCH}` 형식을 사용한다.
- 기존 BE Flyway migration과 ECS 배포·검증 구조를 유지한다.
- 승인된 수동 프로덕션 배포가 성공한 뒤에만 태그와 GitHub Release를 생성한다.

---

### Task 1: BE 브랜치·릴리즈 정책 문서화

**Files:**
- Modify: `AGENTS.md`
- Modify: `CONTRIBUTING.md`
- Modify: `docs/architecture/backend.md`

**Interfaces:**
- Consumes: 현재 `develop` 중심 브랜치 전략과 수동 프로덕션 배포 방식.
- Produces: 에이전트와 개발자가 공통으로 따를 릴리즈·hotfix·버전 승인 규칙.

- [x] `AGENTS.md`에 `fix/*`, `release/v{버전}`, `hotfix/*`의 분기·병합 규칙과 사용자 버전 확인 경계를 추가한다.
- [x] `AGENTS.md`에 프로덕션 workflow 입력, `be-v{버전}` 태그, GitHub Release, 기존 태그 보호 규칙을 추가한다.
- [x] `CONTRIBUTING.md`에 브랜치 표와 Semantic Versioning 기반 릴리즈 절차를 추가한다.
- [x] `docs/architecture/backend.md`의 배포 설명에 버전 입력과 ECS 안정화 이후 릴리즈 기록을 추가한다.
- [x] `git diff --check -- AGENTS.md CONTRIBUTING.md docs/architecture/backend.md`로 문서 diff를 검증한다.

### Task 2: 프로덕션 배포 후 버전 기록 자동화

**Files:**
- Modify: `.github/workflows/deploy-prod.yml`

**Interfaces:**
- Consumes: 필수 `workflow_dispatch.inputs.version` 문자열과 성공한 `main` 배포의 `GITHUB_SHA`.
- Produces: `be-v{MAJOR}.{MINOR}.{PATCH}` annotated tag와 같은 이름의 GitHub Release.

- [x] `workflow_dispatch`에 필수 `version` 입력을 추가한다.
- [x] workflow 권한을 `contents: write`, `id-token: write`로 설정하고 중복 프로덕션 배포를 막는 concurrency를 추가한다.
- [x] checkout을 전체 이력과 태그를 가져오는 `fetch-depth: 0`으로 설정한다.
- [x] 입력값이 `^[0-9]+\.[0-9]+\.[0-9]+$`인지 확인하고 `be-v{버전}` 태그 중복을 배포 전에 거부한다.
- [x] 브랜치·버전·태그 중복 검증이 Flyway migration보다 먼저 완료되도록 preflight job을 둔다.
- [x] 기존 Flyway, ECR, ECS, 안정화 검증 순서는 변경하지 않는다.
- [x] ECS 안정화 검증 뒤에 annotated tag push와 `gh release create --generate-notes`를 추가한다.
- [x] Ruby Psych로 workflow YAML 문법을 확인하고, 가능하면 `actionlint`도 실행한다.

### Task 3: 전체 변경 검증과 기록

**Files:**
- Modify: `docs/tasks/LAN-172/plan.md`

**Interfaces:**
- Consumes: Task 1과 Task 2의 최종 diff와 검증 결과.
- Produces: 재현 가능한 검증 기록과 리뷰 가능한 단일 커밋.

- [x] `git diff --name-only`로 애플리케이션 기능 코드와 테스트 코드가 변경되지 않았는지 확인한다.
- [x] `./gradlew test`로 기존 애플리케이션 회귀가 없는지 확인한다.
- [x] `git diff --check`로 전체 공백 오류를 확인한다.
- [x] workflow에서 태그·Release 생성 단계가 ECS 안정화 검증 뒤에 있는지 확인한다.
- [x] 독립 리뷰에서 요구사항 누락, 과도한 변경, 배포 실패 시 태그 생성 위험을 확인한다.
- [x] 검증 결과를 이 문서에 기록한다.

## 검증 결과

- `ruby -e 'require "psych"; Psych.parse_file(".github/workflows/deploy-prod.yml")'` 통과.
- 임시 Git 저장소에서 유효한 `1.2.4` 허용, 잘못된 버전 형식 거부, 기존 `be-v1.2.3` 태그 중복 감지를 확인.
- `./gradlew test --no-daemon` 통과.
- `git diff --check` 통과.
- 애플리케이션 기능 코드와 테스트 코드 변경 없음.
- `actionlint`는 현재 환경에 설치되어 있지 않아 실행하지 못함.
- 독립 리뷰에서 버전 검증보다 Flyway migration이 먼저 실행되는 문제를 확인하고 `preflight → migrate → deploy → verify → publish` 순서로 수정.
- 수정 후 독립 재리뷰에서 Critical·Important 항목 없이 승인.
- 최종 검증에서 workflow 구문과 실행 순서, 버전 형식, 태그 중복, 애플리케이션·테스트 코드 미변경을 다시 확인하고 `./gradlew test --no-daemon` 통과.
