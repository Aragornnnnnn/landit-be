# LAN-426 Apple 개발자 계정 이전 사용자 마이그레이션 설계

## 목표

Apple 개발자 계정 이전 전후에 기존 Apple 로그인 사용자의 식별자를 일괄 변환한다. 사용자의 `user_profile_id`와 서비스 데이터는 유지하고, 같은 `oauth_identity` 행의 이전 Team `provider_user_id`를 새 Team의 `sub`로 교체한다.

이 작업은 운영자가 GitHub Actions의 수동 workflow를 이전 전과 이전 후에 각각 한 번 실행하는 방식으로 제공한다. Landit 관리자 API와 관리자 UI는 만들지 않는다.

## 전제 조건

- 작업 브랜치는 최신 `main`에서 생성한 `hotfix/LAN-426`이다.
- 대상은 운영 DB의 `provider = APPLE`, `status = ACTIVE`인 OAuth identity다.
- Apple 로그인은 `PREPARE` 실행 전에 운영에서 차단하고, `COMPLETE` 검증이 끝난 뒤 다시 연다.
- Apple 로그인 차단 수단 자체는 LAN-426 구현 범위에 포함하지 않는다.
- Apple의 사용자 이전 API 호출에는 이전 단계에 맞는 Team의 `client_id`와 `client_secret`이 필요하다.
- 이전 Team이 생성한 `transfer_sub`를 새 Team이 교환할 수 있는 Apple의 기한 안에 `COMPLETE`를 끝낸다.

Apple API 요청 형식과 60일 기한은 다음 공식 문서를 기준으로 한다.

- [Transferring your apps and users to another team](https://developer.apple.com/documentation/signinwithapple/transferring-your-apps-and-users-to-another-team)
- [Bringing new apps and users into your team](https://developer.apple.com/documentation/signinwithapple/bringing-new-apps-and-users-into-your-team)

## 범위

- 독립 실행형 Apple 사용자 이전 CLI를 추가한다.
- CLI를 실행하는 Gradle task를 추가한다.
- 운영자가 `PREPARE` 또는 `COMPLETE`를 선택해 실행하는 GitHub Actions 수동 workflow를 추가한다.
- 사용자별 진행 상태와 `transfer_sub`를 저장하는 Flyway 마이그레이션을 추가한다.
- 부분 실패 후 성공한 사용자를 다시 처리하지 않고 실패한 사용자만 재시도할 수 있게 한다.
- 대상 수, 성공 수, 실패 수, 미완료 수를 종료 결과로 제공한다.

다음 항목은 범위에서 제외한다.

- 관리자 API와 관리자 UI
- 로그인 요청 중 `transfer_sub`를 사용해 자동 보정하는 fallback
- 사용자의 접속이나 재로그인을 요구하는 처리
- Google 등 Apple 이외 OAuth identity 변경
- 앱 이전과 Apple 로그인 차단·재개 자체의 자동화

## 실행 구조

`AppleUserMigrationRunner`는 웹 애플리케이션을 기동하지 않고 환경 변수로 DB와 Apple API 설정을 받아 실행하는 Java 진입점이다. `migrateAppleUsers` Gradle task가 이 진입점을 실행한다.

`.github/workflows/apple-user-migration.yml`은 `workflow_dispatch`만 허용하고 다음 입력을 받는다.

- `phase`: `PREPARE` 또는 `COMPLETE`
- `confirmation`: 실행 단계와 일치해야 하는 확인 문자열

workflow는 운영 GitHub Environment와 AWS OIDC를 사용하고 `/landit/prod` 아래 SSM Parameter Store에서 DB 접속 정보와 Apple 이전 자격 증명을 읽는다. 같은 단계가 동시에 실행되지 않도록 운영 Apple 이전 작업 전체에 하나의 concurrency group을 적용한다. 로그에는 DB 내부 identity ID와 집계만 남기며 Apple `sub`, `transfer_sub`, 이메일, 토큰, client secret은 출력하지 않는다.

## 저장 설계

공통 Flyway 마이그레이션 `V74__add_apple_user_migration.sql`로 `apple_user_migration` 테이블을 추가한다. `V74`는 `develop`에 존재하는 다른 마이그레이션과 hotfix 역병합 시 버전이 충돌하지 않도록 현재 전체 저장소의 다음 번호를 사용한다.

테이블은 다음 정보를 저장한다.

- `id`: 기본 키
- `oauth_identity_id`: 대상 `oauth_identity.id`, 유일 키와 외래 키
- `transfer_sub`: Apple이 발급한 이전 식별자, 준비 성공 전에는 `NULL`, 발급 후 유일 키
- `status`: `PENDING`, `PREPARED`, `COMPLETED`, `PREPARE_FAILED`, `COMPLETE_FAILED`
- `failure_code`: 비밀값을 포함하지 않는 마지막 실패 분류
- `attempt_count`: 누적 시도 횟수
- `created_at`, `updated_at`, `completed_at`: 진행 시각

기존 Apple `sub`는 별도 테이블에 복제하지 않는다. `PREPARE` 동안에는 `oauth_identity.provider_user_id`가 이전 Team `sub`이므로 이를 Apple API 입력으로 사용한다. `COMPLETE`에서는 저장된 `transfer_sub`를 입력으로 사용하고, 성공한 트랜잭션에서 같은 identity 행을 새 Team `sub`로 갱신한다.

## PREPARE 처리

앱 이전 전에 이전 Team 자격 증명으로 실행한다.

1. 활성 Apple OAuth identity마다 migration 행을 없을 때만 `PENDING`으로 만든다.
2. Apple `/auth/token`에 `client_credentials`와 `scope=user.migration`으로 요청해 실행 단위 access token을 받는다.
3. `PENDING`과 `PREPARE_FAILED` 행을 identity ID 순서로 조회한다.
4. 각 identity의 이전 Team `provider_user_id`와 새 Team ID를 Apple `/auth/usermigrationinfo`에 보내 `transfer_sub`를 발급받는다.
5. 사용자별로 `transfer_sub`와 `PREPARED` 상태를 커밋한다. 실패한 사용자는 `PREPARE_FAILED`와 비식별 실패 코드만 저장하고 다음 사용자를 계속 처리한다.
6. 전체 대상과 상태별 건수를 출력한다. `PREPARED + COMPLETED` 건수가 전체 대상과 다르면 workflow를 실패시킨다.

이미 `PREPARED` 또는 `COMPLETED`인 행은 다시 Apple에 요청하지 않는다. 따라서 같은 단계를 재실행해도 완료된 사용자에게 새 `transfer_sub`를 중복 발급하지 않는다.

## COMPLETE 처리

앱 이전 후 새 Team 자격 증명으로 실행한다.

1. Apple `/auth/token`에서 새 Team의 사용자 이전 access token을 받는다.
2. `PREPARED`와 `COMPLETE_FAILED` 행을 identity ID 순서로 조회한다.
3. 저장한 `transfer_sub`를 Apple `/auth/usermigrationinfo`에 보내 새 Team의 `sub`와 이메일 정보를 받는다.
4. 사용자별 DB 트랜잭션에서 다음 작업을 함께 수행한다.
   - 대상 identity가 여전히 `APPLE`, `ACTIVE`이고 예상한 이전 상태인지 확인한다.
   - 새 `sub`가 다른 활성 Apple identity에 이미 연결돼 있지 않은지 확인한다.
   - 같은 `oauth_identity` 행의 `provider_user_id`를 새 `sub`로 교체한다.
   - Apple 응답에 이메일이 있으면 `provider_email`을 새 값으로 갱신한다.
   - migration 상태를 `COMPLETED`로 바꾸고 `completed_at`을 기록한다.
5. Apple 호출이나 검증이 실패하면 기존 identity 값은 유지하고 `COMPLETE_FAILED`와 실패 코드만 저장한 뒤 다음 사용자를 처리한다.
6. 전체 대상과 상태별 건수를 출력한다. 모든 대상이 `COMPLETED`가 아니면 workflow를 실패시킨다.

`oauth_identity.user_profile_id`는 변경하지 않으므로 기존 사용자 프로필과 연결된 학습 기록, 구독, 편지 등 서비스 데이터는 그대로 유지된다.

## 오류 처리와 재실행

- 필수 환경 변수가 없거나 Apple access token 발급이 실패하면 사용자 처리를 시작하지 않고 즉시 실패한다.
- 사용자 단위 Apple API 오류는 HTTP 상태와 내부 실패 분류만 기록하고 원문 응답의 식별 정보는 로그에 남기지 않는다.
- 사용자 단위 DB 갱신은 트랜잭션으로 묶어 identity 변경과 완료 상태가 서로 어긋나지 않게 한다.
- 새 `sub` 중복, 대상 identity 상태 변경, 저장된 migration 정보 불일치는 자동 병합하지 않고 실패로 남긴다.
- 재실행 시 해당 단계의 실패 상태만 다시 처리한다.
- 프로세스 종료 코드는 미완료 사용자가 한 명이라도 있으면 실패다.

## 운영 순서

1. hotfix를 운영에 배포하고 `V74`가 적용됐는지 확인한다.
2. 운영 Apple 로그인을 차단한다.
3. 이전 Team 자격 증명과 새 Team ID로 `PREPARE` workflow를 실행한다.
4. 모든 대상이 `PREPARED`인지 집계 결과와 DB 상태로 확인한다.
5. Apple Developer에서 앱 이전을 완료한다.
6. SSM의 Apple 이전 자격 증명을 새 Team 값으로 교체한다.
7. `COMPLETE` workflow를 실행한다.
8. 모든 대상이 `COMPLETED`이고 활성 Apple identity 수가 유지되는지 확인한다.
9. 대표 기존 계정으로 Apple 로그인을 확인한 뒤 운영 Apple 로그인을 다시 연다.

## 검증 기준

- `PREPARE`는 활성 Apple identity만 대상으로 `transfer_sub`를 저장한다.
- `PREPARE` 재실행은 이미 준비된 사용자를 다시 호출하지 않는다.
- `COMPLETE`는 같은 `oauth_identity` 행의 `provider_user_id`와 필요한 경우 `provider_email`만 갱신하고 `user_profile_id`는 유지한다.
- `COMPLETE` 재실행은 이미 완료된 사용자를 다시 호출하거나 수정하지 않는다.
- 사용자 한 명의 Apple API 또는 DB 처리가 실패해도 다른 사용자는 계속 처리되고, workflow 결과는 실패한다.
- 새 `sub` 충돌이나 identity 상태 불일치 시 기존 사용자 연결을 덮어쓰지 않는다.
- 로그와 테스트 출력에 Apple 식별자, 이메일, access token, client secret이 노출되지 않는다.
- GitHub Actions에서는 운영 환경만 선택할 수 있고 수동 확인 문자열이 맞아야 실행된다.
- `./gradlew check`가 통과한다.
