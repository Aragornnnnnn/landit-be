# LAN-103 Java 코드 포맷 자동화 설계

## 목표

Java 소스의 포맷을 Google Java Format으로 통일하고, 로컬 Gradle 명령과 PR CI에서 같은 기준을 검사한다. Checkstyle의 Google 규칙 구성으로 명명, Javadoc 형식, 파일 구조 등 자동으로 판별 가능한 Google Java Style 규칙도 함께 검사한다.

## 도구와 적용 범위

- Gradle 플러그인은 `com.diffplug.spotless` 8.8.0을 사용한다.
- Java 포매터는 `google-java-format` 1.35.0을 사용한다.
- Gradle 기본 `checkstyle` 플러그인과 Checkstyle 13.8.0을 사용한다.
- 검사 대상은 `src/main/java/**/*.java`와 `src/test/java/**/*.java`다.
- `./gradlew spotlessCheck`는 포맷 위반이 있으면 실패한다.
- `./gradlew spotlessApply`는 대상 Java 소스를 자동 포맷한다.
- `./gradlew checkstyleMain checkstyleTest`는 Checkstyle이 판별 가능한 Google Java Style 위반이 있으면 실패한다.
- Checkstyle은 13.8.0의 `google_checks.xml`을 복제한 `config/checkstyle/google_checks.xml`을 사용하며, 심각도를 오류로 설정한다.
- 정적 분석으로 판별할 수 없는 규칙은 기존 코드 리뷰 규칙으로 유지한다.

## 저장소 로컬 규칙

새 Java 소스 파일 첫 줄의 한국어 역할 주석은 유지한다. 이 규칙은 Google Java Style Guide의 일반 소스 파일 순서에 대한 저장소 로컬 예외다. 역할 주석 다음 줄을 비운 뒤 `package` 선언을 두어 공식 Google Checkstyle 구성을 그대로 통과시킨다. Checkstyle은 이 주석의 존재 여부를 검사하지 않으며, 별도의 주석 검사 도구도 이번 이슈 범위에 포함하지 않는다.

Google Java Style Guide의 원문을 저장소에 복제하지 않는다. 공식 문서를 기준으로 연결하고, 프로젝트에서 자동으로 검사하는 범위와 추가 로컬 규칙만 별도 문서에 정리한다. `AGENTS.md`와 `CONTRIBUTING.md`는 이 문서를 참조한다.

## CI 흐름

`.github/workflows/ci.yml`의 PR 검증 작업에서 애플리케이션 테스트 전에 `./gradlew spotlessCheck --no-daemon`과 `./gradlew checkstyleMain checkstyleTest --no-daemon`을 실행한다. 포맷 또는 Checkstyle 위반이 있으면 테스트 실행 전 CI가 실패한다.

## 기존 코드 처리

현재 Java 소스 전체에 `spotlessApply`를 적용한다. 기계적인 포맷 변경은 설정, CI, 문서 변경과 구분되는 `style-only` 커밋으로 분리해 리뷰할 수 있게 한다. Checkstyle 위반은 이번 PR에서 모두 수정하며 suppressions 또는 기준선 예외는 추가하지 않는다. 기능 동작은 변경하지 않는다.

## 문서화

별도 코드 스타일 문서에 다음 내용을 기록한다.

- Google Java Style Guide 공식 링크.
- Spotless, Google Java Format, Checkstyle이 자동으로 검사하는 범위.
- `spotlessCheck`, `spotlessApply`, `checkstyleMain`, `checkstyleTest` 사용법.
- 첫 줄 한국어 역할 주석을 포함한 저장소 로컬 규칙.
- 자동 검사 대상이 아닌 규칙은 리뷰로 확인한다는 경계.

## 검증

구현 후 다음 명령을 실행한다.

```bash
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew checkstyleMain checkstyleTest
./gradlew test
```

`spotlessApply` 후 Java 변경은 포맷만 포함하는지 diff로 확인한다. `spotlessCheck`, `checkstyleMain`, `checkstyleTest`, 전체 테스트가 모두 통과해야 완료한다.
