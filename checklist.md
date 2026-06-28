# 초기 BE 셋팅 체크리스트

- [x] 저장소 상태 확인.
- [x] Spring Boot 4 지원 버전과 dependency id 확인.
- [x] Gradle 기반 Spring Boot 프로젝트 생성.
- [x] 요청된 BE 라이브러리 의존성 반영.
- [x] Java 21 설정 반영.
- [x] Scheduler 활성화.
- [x] PostgreSQL 기본 설정과 H2 테스트 설정 분리.
- [x] Flyway 초기 마이그레이션 추가.
- [x] 애플리케이션 컨텍스트 테스트 구성.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## DB 연결 설정

- [x] 기존 DB/profile/secret 설정 방식 조사.
- [x] SSM 직접 조회 패턴 부재 확인.
- [x] env var 기반 DB 설정 유지 계획 수립.
- [x] local/develop/prod profile 설정 분리.
- [x] secret 없는 `.env.example` 추가.
- [x] 설정 검증 테스트 추가.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## 시간대 설정

- [x] 기존 timezone 설정 부재 확인.
- [x] JVM 기본 timezone을 `Asia/Seoul`로 설정.
- [x] Jackson timezone을 `Asia/Seoul`로 설정.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.
