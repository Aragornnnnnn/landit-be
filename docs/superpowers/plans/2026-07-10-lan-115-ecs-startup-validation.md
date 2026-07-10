# LAN-115 ECS Startup Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 원격 AI 모드의 Spring 기동 실패를 제거하고 ECS deploy 검증이 태스크 기동 실패를 즉시 실패 처리한다.

**Architecture:** Spring Boot 4의 Jackson 3 자동 구성과 별도로, 기존 AI 클라이언트가 사용하는 Jackson 2 `ObjectMapper`를 좁은 설정 Bean으로 제공한다. 배포 워크플로우는 ECS의 nullable `failedTasks`를 안전한 정수로 바꾼 후 PRIMARY rollout 실패와 실패 태스크 상태를 즉시 판정한다.

**Tech Stack:** Java 21, Spring Boot 4, JUnit 5, Gradle, GitHub Actions, AWS CLI, Bash.

## Global Constraints

- `com.fasterxml.jackson.databind.ObjectMapper`를 사용하는 기존 AI 클라이언트 API는 바꾸지 않는다.
- workflow 변경 범위는 develop 배포의 `Verify ECS service` 단계로 제한한다.
- 상태를 출력한 뒤 실패할 때 최근 ECS 이벤트도 함께 출력한다.

---

### Task 1: 원격 AI 모드 기동 검증과 ObjectMapper Bean 등록

**Files:**

- Create: `src/main/java/com/landit/landitbe/common/config/JacksonConfiguration.java`
- Modify: `src/test/java/com/landit/landitbe/LanditBeApplicationTests.java`

- [ ] 원격 AI 모드의 ApplicationContext가 `RemoteAiConversationClient`를 생성하는 실패 테스트를 작성한다.
- [ ] 해당 테스트를 실행해 `com.fasterxml.jackson.databind.ObjectMapper` Bean 누락으로 실패함을 확인한다.
- [ ] Jackson 2 `ObjectMapper` Bean을 최소 설정으로 등록한다.
- [ ] 대상 테스트와 전체 Gradle 테스트를 실행한다.

### Task 2: ECS 실패 태스크 즉시 감지

**Files:**

- Modify: `.github/workflows/deploy-dev.yml`

- [ ] `failedTasks`가 빈 값, `None`, `null`, 비숫자이면 0으로 정규화한다.
- [ ] PRIMARY rollout 실패 또는 실패 태스크와 실행·대기 태스크 부재를 감지하면 서비스 상태와 최근 이벤트를 출력하고 종료한다.
- [ ] `git diff --check`와 워크플로우 diff로 변경을 검토한다.

### Task 3: 작업 기록과 커밋

**Files:**

- Modify: `docs/tasks/LAN-115/checklist.md`
- Modify: `docs/tasks/LAN-115/context-notes.md`

- [ ] 검증 결과와 설계 결정을 기록한다.
- [ ] 하나의 논리적 변경으로 커밋한다.
