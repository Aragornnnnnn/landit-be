# LAN-103 Java 코드 포맷 자동화 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Google Java Format 기반의 Java 포맷 검사와 자동 적용을 Gradle 및 PR CI에 추가한다.

**Architecture:** Spotless가 `src/main/java`와 `src/test/java`의 Java 파일을 `google-java-format` 1.35.0으로 처리한다. PR CI는 애플리케이션 테스트 전에 동일한 Gradle 검사 태스크를 실행한다. 자동 포맷이 다루지 않는 규칙은 별도 문서와 기존 리뷰 규칙으로 유지한다.

**Tech Stack:** Gradle 9.5.1, Spotless 8.8.0, google-java-format 1.35.0, GitHub Actions, Java 21.

## Global Constraints

- Spotless 플러그인은 `com.diffplug.spotless` 8.8.0을 사용한다.
- 포매터는 `google-java-format` 1.35.0을 사용한다.
- 검사 범위는 `src/main/java/**/*.java`와 `src/test/java/**/*.java`로 한정한다.
- 기존 Java 전체 포맷은 설정·문서 변경과 분리된 style-only 커밋으로 남긴다.
- 새 Java 파일 첫 줄의 한국어 역할 주석은 유지하며, 존재 여부의 자동 검사는 추가하지 않는다.
- 코드 변경 후 `./gradlew spotlessCheck`와 `./gradlew test`를 모두 실행한다.

---

### Task 1: Spotless Gradle 및 PR CI 설정

**Files:**

- Modify: `build.gradle`
- Modify: `.github/workflows/ci.yml`

**Interfaces:**

- Produces: `spotlessCheck`와 `spotlessApply` Gradle 태스크.
- Consumes: GitHub Actions의 Java 21 Gradle 환경.

- [x] **Step 1: Spotless 플러그인과 Java 포맷 대상을 설정한다.**

`build.gradle`의 `plugins` 블록에 다음 플러그인을 추가한다.

```groovy
id 'com.diffplug.spotless' version '8.8.0'
```

`repositories` 블록 뒤에 다음 구성을 추가한다.

```groovy
spotless {
    java {
        target 'src/main/java/**/*.java', 'src/test/java/**/*.java'
        googleJavaFormat('1.35.0')
    }
}
```

- [x] **Step 2: 포맷 검사가 기존 코드에서 실패하는지 확인한다.**

Run: `./gradlew spotlessCheck`

Expected: 기존 포맷 차이가 있으면 `spotlessJavaCheck` 실패와 `spotlessApply` 안내가 출력된다.

- [x] **Step 3: PR CI에 명시적인 포맷 검사 단계를 추가한다.**

`.github/workflows/ci.yml`에서 `Run application tests` 앞에 다음 단계를 추가한다.

```yaml
      - name: Check Java code style
        run: ./gradlew spotlessCheck --no-daemon
```

- [x] **Step 4: 변경 파일의 공백 오류를 확인한다.**

Run: `git diff --check`

Expected: 출력 없음.

### Task 2: 기존 Java 전체 포맷 기준선 적용

**Files:**

- Modify: `src/main/java/**/*.java`
- Modify: `src/test/java/**/*.java`

**Interfaces:**

- Consumes: Task 1이 제공한 `spotlessApply` 태스크.
- Produces: `spotlessCheck`를 통과하는 전체 Java 포맷 기준선.

- [x] **Step 1: 자동 포맷을 적용한다.**

Run: `./gradlew spotlessApply`

Expected: 대상 Java 파일만 `google-java-format` 규칙에 맞게 변경된다.

- [x] **Step 2: 포맷 검사가 통과하는지 확인한다.**

Run: `./gradlew spotlessCheck`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 3: 포맷 diff를 확인한다.**

Run: `git diff --check`

Expected: 출력 없음.

Run: `git diff --stat -- src/main/java src/test/java`

Expected: Java 파일만 표시되고, 코드 의미 변경이나 파일 추가·삭제가 없다.

- [x] **Step 4: 포맷 기준선을 독립 커밋으로 남긴다.**

```bash
git add src/main/java src/test/java
git commit -m "style: 기존 Java 소스에 Google Java Format을 적용한다"
```

Expected: Java 소스 변경만 포함한 style-only 커밋이 생성된다.

### Task 3: 코드 스타일 문서와 저장소 규칙 연결

**Files:**

- Create: `docs/development/java-style.md`
- Modify: `AGENTS.md`
- Modify: `CONTRIBUTING.md`
- Modify: `docs/tasks/LAN-103/plan.md`

**Interfaces:**

- Consumes: Task 1의 `spotlessCheck`와 `spotlessApply` 명령.
- Produces: 사람과 Agents가 참조하는 Java 코드 스타일 문서.

- [x] **Step 1: 프로젝트 코드 스타일 문서를 작성한다.**

`docs/development/java-style.md`에 다음 내용을 포함한다.

```markdown
# Java 코드 스타일

이 프로젝트의 Java 포맷 기준은 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)와 `google-java-format`이다.

## 자동 포맷과 검사

```bash
./gradlew spotlessCheck
./gradlew spotlessApply
```

Spotless는 `src/main/java`와 `src/test/java`의 Java 파일을 검사한다. `spotlessCheck`는 포맷 위반이 있으면 실패하고, `spotlessApply`는 같은 규칙을 자동 적용한다.

## 자동 검사 범위

`google-java-format`은 공백, 들여쓰기, 줄바꿈, import 정렬 등 Java 포맷을 통일한다. 명명, Javadoc 내용, 코드 구조는 자동으로 검사하지 않으며 기존 코드 리뷰 규칙을 따른다.

## 저장소 로컬 규칙

새 Java 소스 파일은 첫 줄에 파일 역할을 설명하는 한국어 `//` 주석을 둔다. 이 규칙은 `google-java-format`과 함께 유지하지만 Spotless가 주석 존재 여부를 검사하지는 않는다.
```

- [x] **Step 2: Agents와 기여자 문서에서 스타일 문서를 참조한다.**

`AGENTS.md`의 Java 코드 규칙에 `docs/development/java-style.md`를 기준 문서로 명시하고, 자동 포맷과 자동 검사 범위의 경계를 한 문장으로 추가한다.

`CONTRIBUTING.md`의 코드 컨벤션에 같은 문서 링크와 다음 명령을 추가한다.

```bash
./gradlew spotlessCheck
./gradlew spotlessApply
```

- [x] **Step 3: 문서 링크와 명령을 확인한다.**

Run: `rg -n 'java-style\.md|spotless(Check|Apply)' AGENTS.md CONTRIBUTING.md docs/development/java-style.md`

Expected: 세 파일에 스타일 문서 참조 또는 두 Gradle 명령이 표시된다.

- [x] **Step 4: 계획 문서에 실제 검증 결과를 기록한다.**

`docs/tasks/LAN-103/plan.md`의 각 완료 항목을 체크하고, 마지막에 실행한 명령과 결과를 적는다.

- [x] **Step 5: 설정·CI·문서 변경을 커밋한다.**

```bash
git add build.gradle .github/workflows/ci.yml AGENTS.md CONTRIBUTING.md docs/development/java-style.md docs/tasks/LAN-103/plan.md
git commit -m "chore: Java 코드 포맷 검사를 자동화한다"
```

Expected: Java 소스 포맷 변경 없이 Gradle 설정, CI, 문서만 포함한 커밋이 생성된다.

### Task 4: 최종 검증

**Files:**

- Verify: `build.gradle`
- Verify: `.github/workflows/ci.yml`
- Verify: `src/main/java/**/*.java`
- Verify: `src/test/java/**/*.java`
- Verify: `docs/development/java-style.md`

**Interfaces:**

- Consumes: Task 1부터 Task 3까지의 Gradle 설정, CI 설정, 포맷 기준선, 문서.
- Produces: 완료 기준을 만족하는 검증 증거.

- [x] **Step 1: 포맷 검사를 실행한다.**

Run: `./gradlew spotlessCheck --no-daemon`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 2: 전체 테스트를 실행한다.**

Run: `./gradlew test --no-daemon`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 3: 최종 diff와 작업 트리를 확인한다.**

Run: `git diff origin/develop --check`

Expected: 출력 없음.

Run: `git status --short --branch`

Expected: 작업 트리가 깨끗하고 `feat/LAN-103`이 원격 기준보다 커밋만 앞선 상태다.

## 검증 결과

- [x] `./gradlew spotlessCheck --no-daemon` 성공
- [x] `./gradlew test --no-daemon` 성공
- [x] `git diff origin/develop --check` 출력 없음

## Checkstyle 확장 작업

### Task 5: Google Checkstyle 설정과 CI 연결

**Files:**

- Modify: `build.gradle`
- Modify: `.github/workflows/ci.yml`
- Modify: `docs/development/java-style.md`
- Modify: `AGENTS.md`
- Modify: `CONTRIBUTING.md`

**Interfaces:**

- Produces: `checkstyleMain`, `checkstyleTest` Gradle 태스크와 PR CI의 Google Java Style 검사.
- Consumes: Checkstyle 13.8.0의 `google_checks.xml`을 기반으로 저장소 로컬 예외를 반영한 설정 파일.

- [x] **Step 1: Google Checkstyle 설정을 추가한다.**

`build.gradle`의 `plugins` 블록에 `id 'checkstyle'`를 추가하고 다음 구성을 추가한다.

```groovy
checkstyle {
    toolVersion = '13.8.0'
    config = resources.text.fromFile('config/checkstyle/google_checks.xml')
    configProperties = ['org.checkstyle.google.severity': 'error']
}
```

`config/checkstyle/google_checks.xml`은 Checkstyle 13.8.0의 공식 Google 구성 사본이다. Gradle의 Checkstyle 의존성 전체를 설정 파일로 해석하는 문제를 피하기 위해 저장소에 둔다.

- [x] **Step 2: 기존 소스에서 Google Checkstyle 위반이 실패로 보고되는지 확인한다.**

Run: `./gradlew checkstyleMain checkstyleTest --no-daemon`

Expected: `MissingJavadocType`, `MissingJavadocMethod`, `EmptyLineSeparator`, `TextBlockGoogleStyleFormatting`, `VariableDeclarationUsageDistance` 등 기존 위반으로 실패한다.

- [x] **Step 3: PR CI에 Google Checkstyle 단계를 추가한다.**

`.github/workflows/ci.yml`에서 테스트 전에 다음 단계를 추가한다.

```yaml
      - name: Check Google Java Style
        run: ./gradlew checkstyleMain checkstyleTest --no-daemon
```

- [x] **Step 4: 스타일 문서와 저장소 규칙을 갱신한다.**

`docs/development/java-style.md`, `AGENTS.md`, `CONTRIBUTING.md`에 `checkstyleMain`, `checkstyleTest` 사용법과 검사 범위를 추가한다. 첫 줄 한국어 역할 주석은 Google 일반 파일 순서에 대한 저장소 로컬 예외이며 Checkstyle의 Google 구성은 존재 여부를 검사하지 않는다고 명시한다.

### Task 6: 기존 Google Checkstyle 위반 정리

**Files:**

- Modify: `src/main/java/**/*.java`
- Modify: `src/test/java/**/*.java`

**Interfaces:**

- Consumes: Task 5의 `checkstyleMain`와 `checkstyleTest`.
- Produces: Google Checkstyle 위반이 없는 Java 소스.

- [x] **Step 1: 파일 역할 주석 뒤에 빈 줄을 추가한다.**

각 Java 파일의 첫 줄 한국어 역할 주석과 `package` 선언 사이에 빈 줄을 둔다. 이 변경은 `EmptyLineSeparator` 220건을 해소하고 저장소 로컬 주석 규칙을 유지한다.

```java
// 사용자 인증 상태를 관리하는 서비스

package com.landit.landitbe.auth.application;
```

- [x] **Step 2: 누락된 타입과 메서드 Javadoc을 역할 기반으로 작성한다.**

`MissingJavadocType`과 `MissingJavadocMethod`가 보고한 선언마다 첫 문장이 역할을 설명하고 마침표로 끝나는 Javadoc을 추가한다. 빈 Javadoc이나 클래스·메서드 이름을 그대로 반복하는 설명은 사용하지 않는다.

```java
/**
 * 사용자 프로필의 locale을 조회한다.
 */
public UserLocale findUserLocale(Long userId) {
```

- [x] **Step 3: 기존 Javadoc 요약 문구를 Google 규칙에 맞춘다.**

`SummaryJavadoc`가 보고한 문구에서 `This`, `Returns`, `Gets`, `Sets` 같은 금지된 영어 시작 문구를 역할을 직접 설명하는 문장으로 바꾼다.

- [x] **Step 4: 텍스트 블록과 변수 선언 위치를 수정한다.**

`TextBlockGoogleStyleFormatting`이 보고한 텍스트 블록의 여는·닫는 큰따옴표 위치를 맞추고, `VariableDeclarationUsageDistance`가 보고한 지역 변수는 변경되지 않는 값을 `final`로 명시했다. 다중 최상위 타입은 별도 파일로 분리하고, 테스트 메서드명은 의미를 유지한 lowerCamelCase로 바꿨다.

- [x] **Step 5: Google Checkstyle 검사를 통과시킨다.**

Run: `./gradlew checkstyleMain checkstyleTest --no-daemon`

Expected: `BUILD SUCCESSFUL`.

### Task 7: 최종 검증과 커밋

**Files:**

- Modify: `docs/tasks/LAN-103/plan.md`

**Interfaces:**

- Consumes: Task 5와 Task 6의 설정 및 Java 소스.
- Produces: 포맷, Google Checkstyle, 애플리케이션 테스트를 모두 통과하는 LAN-103 변경.

- [x] **Step 1: 전체 품질 검사를 실행한다.**

Run: `./gradlew spotlessCheck checkstyleMain checkstyleTest test --no-daemon`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 2: 최종 diff를 점검한다.**

Run: `git diff origin/develop --check`

Expected: 출력 없음.

- [x] **Step 3: 실제 검증 결과를 계획에 기록하고 논리 단위로 커밋한다.**

```bash
git add build.gradle .github/workflows/ci.yml AGENTS.md CONTRIBUTING.md docs/development/java-style.md docs/tasks/LAN-103/plan.md src/main/java src/test/java
git commit -m "chore: Google Java Style 검사를 추가한다"
```

Expected: Checkstyle 설정, 문서, 기존 위반 정리만 포함한 커밋이 생성된다.

## Checkstyle 확장 검증 결과

- [x] `./gradlew spotlessCheck checkstyleMain checkstyleTest test --no-daemon` 성공.
- [x] `git diff origin/develop --check` 출력 없음.
- [x] 첫 줄 한국어 역할 주석 뒤 빈 줄을 추가해 공식 Google Checkstyle 구성과 함께 유지.
- [x] `chore: Google Java Style 검사를 추가한다` 커밋 생성.
