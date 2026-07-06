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

## LAN-43 dev 배포 workflow

- [x] repo 구조, build tool, Dockerfile, 기존 workflow, git 상태 확인.
- [x] 기존 Dockerfile과 workflow 부재 확인.
- [x] 최소 Dockerfile 추가.
- [x] dev 배포용 GitHub Actions workflow 추가.
- [x] dev 배포 workflow를 수동 실행으로 제한.
- [x] dev 배포 workflow를 `develop` GitHub Environment 변수 기반으로 변경.
- [x] workflow YAML 문법 확인.
- [x] `./gradlew test` 실행.
- [x] `git diff`와 `git status --short` 확인.
- [x] 논리 단위 커밋 생성.

## LAN-43 prod 배포 workflow

- [x] 현재 AWS ECS/ECR 리소스 이름 확인.
- [x] prod 배포용 GitHub Actions workflow 추가.
- [x] prod 배포 workflow를 수동 실행으로 제한.
- [x] prod 배포 workflow를 `main` 브랜치에서만 진행하도록 제한.
- [x] prod 배포 workflow를 `prod` GitHub Environment 변수 기반으로 변경.
- [x] workflow YAML 문법 확인.
- [x] `./gradlew test` 실행.
- [x] `git diff`와 `git status --short` 확인.
- [x] 논리 단위 커밋 생성.

## LAN-71 Agent 개발용 아키텍처 문서화

- [x] `feat/LAN-71` 브랜치에서 작업.
- [x] 기존 문서 구조와 아키텍처 초안 확인.
- [x] `AGENTS.md`에 에이전트가 따라야 할 아키텍처 규칙 추가.
- [x] 상세 백엔드 아키텍처 문서 추가.
- [x] `README.md`에서 상세 문서로 연결.
- [x] 문서 링크와 diff 검토.
- [x] 논리 단위 커밋 생성.
