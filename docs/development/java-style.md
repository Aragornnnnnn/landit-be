# Java 코드 스타일

이 프로젝트의 Java 포맷 기준은 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)와 `google-java-format`이다.

## 자동 포맷과 검사

다음 Gradle 명령을 사용한다.

```bash
./gradlew spotlessCheck
./gradlew spotlessApply
```

Spotless는 `src/main/java`와 `src/test/java`의 Java 파일을 검사한다. `spotlessCheck`는 포맷 위반이 있으면 실패하고, `spotlessApply`는 같은 규칙을 자동 적용한다. PR CI도 테스트 전에 `spotlessCheck`를 실행한다.

## 자동 검사 범위

`google-java-format`은 공백, 들여쓰기, 줄바꿈, import 정렬 등 Java 포맷을 통일한다. 명명, Javadoc 내용, 코드 구조는 자동으로 검사하지 않으며 기존 코드 리뷰 규칙을 따른다.

## 저장소 로컬 규칙

새 Java 소스 파일은 첫 줄에 파일 역할을 설명하는 한국어 `//` 주석을 둔다. 이 규칙은 `google-java-format`과 함께 유지하지만 Spotless가 주석 존재 여부를 검사하지는 않는다.
