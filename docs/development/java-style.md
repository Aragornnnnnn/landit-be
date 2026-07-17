# Java 코드 스타일

이 프로젝트의 Java 스타일 기준은 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)다. 포맷은 `google-java-format`으로, 자동 판별 가능한 명명·Javadoc·구조 규칙은 Checkstyle로 검사한다.

## 자동 포맷과 검사

다음 Gradle 명령을 사용한다.

```bash
./gradlew spotlessCheck
./gradlew spotlessApply
./gradlew checkstyleMain checkstyleTest
```

Spotless는 `src/main/java`와 `src/test/java`의 Java 파일을 검사한다. `spotlessCheck`는 포맷 위반이 있으면 실패하고, `spotlessApply`는 같은 규칙을 자동 적용한다. `checkstyleMain`과 `checkstyleTest`는 같은 소스 집합의 Google Java Style 위반을 검사한다. PR CI는 테스트 전에 두 검사를 모두 실행한다.

## 자동 검사 범위

`google-java-format`은 공백, 들여쓰기, 줄바꿈, import 정렬 등 Java 포맷을 통일한다. Checkstyle은 줄 길이, 명명, Javadoc 형식, 텍스트 블록, 변수 선언 위치, 파일당 최상위 타입 수처럼 정적으로 판별 가능한 Google Java Style 규칙을 검사한다. 의미가 적절한 Javadoc인지와 메서드 책임 분리는 코드 리뷰로 확인한다.

## 저장소 로컬 규칙

새 Java 소스 파일은 첫 줄에 파일 역할을 설명하는 한국어 `//` 주석을 둔다. 이 규칙은 Google Java Style의 일반 파일 순서에 대한 저장소 로컬 예외다. 역할 주석 다음 줄을 비우고 `package` 선언을 둬 Checkstyle의 파일 순서 규칙도 함께 만족한다. 역할 주석의 존재 여부는 Spotless나 Checkstyle이 검사하지 않으므로 코드 리뷰에서 확인한다.
