# LAN-176 CodeRabbit Configuration Implementation Plan

**Goal:** 세 Landit 저장소에 저장소별 CodeRabbit 리뷰 지침과 필요한 교차 저장소 연결을 적용한다.

**Architecture:** 각 저장소 루트의 `.coderabbit.yaml`을 독립 설정으로 유지한다. 공통 자동 리뷰 정책은 동일하게 두고, `path_instructions`와 `knowledge_base.linked_repositories`만 저장소 책임에 맞게 다르게 구성한다.

**Tech Stack:** CodeRabbit schema v2 YAML, Git, Gradle, Python unittest, pnpm/Turbo.

---

### Task 1: 설계와 현재 저장소 규칙 확정

- [x] 세 저장소의 최신 `origin/develop`, `AGENTS.md`, 디렉터리 구조를 확인한다.
- [x] 무료 Open Source 플랜에서 사용할 공통 정책과 연결 방향을 설계 문서에 기록한다.

### Task 2: 저장소별 CodeRabbit 설정 적용

- [x] `landit-be/.coderabbit.yaml`에 BE 의미 규칙과 FE·AI 연결을 추가한다.
- [x] `landit-ai/.coderabbit.yaml`에 AI 의미 규칙과 BE 연결을 추가한다.
- [x] `landit-fe/.coderabbit.yaml`에 웹·모바일·bridge 의미 규칙과 BE 연결을 추가한다.

### Task 3: 설정과 저장소 검증

- [x] 세 YAML 파일을 파싱하고 핵심 필드와 연결 방향을 검사한다.
- [x] 세 저장소에서 `git diff --check`와 변경 범위를 확인한다.
- [x] BE `./gradlew check`, AI unittest, FE 포맷·lint·typecheck·test·build를 실행한다.

### Task 4: 커밋과 인계

- [x] 저장소별로 하나의 의미 단위 커밋을 만든다.
- [x] CodeRabbit 앱의 FE 저장소 설치 여부와 첫 PR에서 확인할 항목을 정리한다.

## 검증 결과

- 공식 CodeRabbit `schema.v2.json` 검증을 세 저장소 모두 통과했다.
- BE `./gradlew check`가 통과했다.
- AI unittest 164개가 통과했다.
- FE는 CI와 같은 Node 22, pnpm 10.14.0으로 포맷·lint·typecheck·test·build를 통과했다.

## 활성화 확인

- CodeRabbit GitHub App의 repository access에 `landit-fe`가 포함되어 있어야 한다.
- 첫 PR에서 Configuration check와 자동 리뷰가 실행되는지 확인한다.
- 연결 저장소를 읽지 못하면 CodeRabbit 조직 설정에서 세 저장소 접근 권한을 다시 확인한다.
