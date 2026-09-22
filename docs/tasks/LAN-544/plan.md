# LAN-544 페이월 이탈 할인과 결제 금액 조회

## 확정한 계약

- 계정별 최초 페이월 이탈에서만 5분 할인을 부여한다. 구독 조회는 상태를 변경하지 않는다.
- `newUser`는 사용자 확인에 따라 부여 당시 가입 후 7일 미만이면 true로 저장한다. 가입일은 할인 자격 조건이 아니다.
- `user_profile.discount_offer_expires_at`과 `discount_offer_new_user`는 함께 기록한다. 기록이 있으면 만료 후에도 재설정하지 않는다.
- `POST /api/v1/me/paywall/dismiss`는 인증 필수이며 요청 바디가 없다. 기존 `ApiResponse`의 `data.promo`로 응답한다.
- `GET /api/v1/me/subscription`도 동일한 `promo` 객체를 반환한다. 미부여·만료·현재 프리미엄이면 null이다.
- `remainingSeconds`는 서버 기준 남은 시간을 초 단위로 올림한다. 실제 만료 시각 전에는 최소 1초이며 만료 시각부터 promo가 null이다. `expiresAt`은 기존 API와 같은 서울 시간대 LocalDateTime이다.
- 무료 체험·프로모션·해지 예약을 포함한 현재 프리미엄 사용자는 할인 기회를 소진하지 않는다. 프리미엄 여부는 상태와 실제 구독 만료 시각을 함께 확인한다.
- 프로필은 `@DynamicUpdate`로 변경한 컬럼만 갱신한다. 할인 부여 전에 프로필을 읽은 억양 변경이 나중에 커밋되어도 할인 기록을 덮어쓰지 않는다.
- 할인 부여와 구독 웹훅은 같은 프로필 행을 잠근다. 시각은 잠금 획득 후 읽으며 DB의 마이크로초 정밀도로 저장한다.
- `price`·`currency`는 기존 이벤트 중 INITIAL_PURCHASE·RENEWAL·PRODUCT_CHANGE, 양수 금액, TRIAL·PROMOTIONAL 제외 조건으로 최신 건을 선택한다. occurred_at 내림차순, 같은 시각이면 id 내림차순이다. 최근 50건 제한 없이 조회한다.
- 결제 이력이 없으면 금액·통화 모두 null이며, 선택된 결제의 통화가 없으면 통화만 null이다. 다음 갱신 예상액이나 상품 정가를 의미하지 않는다.
- Play 상품 ID의 콜론 뒤 베이스 플랜까지 그대로 유지한다. 웹훅·앱 UI·스토어 상품 설정은 변경하지 않는다.

## 구현과 검증

1. 프로필 할인 상태와 전용 Service, V116 마이그레이션 추가.
2. 이탈 API 및 구독 조회의 promo·price·currency와 OpenAPI 계약 추가.
3. 시간 경계 단위 테스트, 실제 H2 트랜잭션 동시 요청, 인증·재로그인·결제 웹훅 API 통합 테스트.
4. Spotless·Checkstyle·전체 테스트 및 변경 diff 확인 후 커밋.

## 마이그레이션 순서

2026-09-21 확인한 origin/develop의 최대 버전은 V111이다. 열린 PR #204·#205·#206·#207은 각각 V112·V113·V114·V115를 사용하므로 V116을 사용한다. 해당 PR의 마이그레이션이 먼저 반영되어야 하며, 병합·배포 전 열린 PR 및 대상 DB 이력을 다시 확인해야 한다. 이번 작업에서는 운영 DB에 접속하거나 마이그레이션을 적용하지 않는다.

## 검증 결과

- `./gradlew spotlessApply check --console=plain` 통과. 총 1365개 테스트, 실패 0개·오류 0개·환경 조건 생략 9개. Spotless·Checkstyle 포함.
- LAN-544 신규 테스트 12건: API 통합 8건과 시간 경계 4건. 기존 인증·OpenAPI 테스트에도 새 경로와 필드 검증 추가.
- 실제 H2 트랜잭션으로 동시 이탈 요청의 만료 시각 일치와 DB 저장값 일치를 확인했다. 최초 부여·재로그인·신규 라벨 고정·만료 재부여 방지·프리미엄 미소진·50건을 넘는 결제 이력·동일 시각 결제 정렬·웹훅 금액·Play 베이스 플랜 보존을 검증했다.
- 비로그인 요청 테스트에서 발견한 신규 경로 인증 매핑 누락을 수정하고 401 응답을 확인했다.
- `git diff --check` 통과. 운영 PostgreSQL·스토어 결제·실기기·CI·배포는 검증하지 않았다.

- 리뷰에서 확인한 억양 변경과 할인 부여의 갱신 유실을 수정했다. 실제 H2의 별도 트랜잭션에서 할인 기록·신규 혜택·억양 설정 보존과 재부여 방지를 검증했다. 수정 후 전체 테스트가 통과했고, 테스트 변수의 final 표기 보완 후 `./gradlew check --console=plain`도 통과했다.

## 리뷰 수정: PostgreSQL 제약 검증 분리

- V116을 H2·PostgreSQL 런타임 경로로 분리했다. H2는 기존 SQL 그대로이며 PostgreSQL은 CHECK를 `NOT VALID`로 추가한다. 새 쓰기는 즉시 검사하지만 기존 행 스캔은 별도 V121에서 수행한다.
- 2026-09-22 develop V111 및 열린 PR의 세 런타임 migration 경로를 재확인했다. #213의 V120 다음 번호 V121을 사용하며 #212의 별도 검증은 V122다. V121 적용 전 V112~V120을 모두 포함하거나 이미 적용해야 한다. 병합·배포 직전 번호와 대상 DB 이력을 다시 확인한다.
- Flyway `group` 기본값 false를 유지해 V116과 V121 사이에 커밋한다. 미배포 V116의 PostgreSQL SQL을 수정했으므로 이전 PR 버전을 적용한 개발 DB는 체크섬 차이를 별도로 처리해야 한다. 운영 이력은 조회하거나 변경하지 않았다.
- PostgreSQL 15.18에서 [재실행 SQL](verify-discount-postgres.sql)을 통과했다. 기존 사용자 보존, 검증 전 잘못된 null 조합 2건 거부, 정상 상태 저장, V121 이후 `convalidated=true`, 검증 트랜잭션의 `ShareUpdateExclusiveLock`과 `AccessExclusiveLock` 부재를 확인했다. 전용 schema는 삭제했다.
- 이 SQL 검증은 최소 사용자 테이블을 사용하는 제약 검증이며 운영 DB·전체 PostgreSQL Flyway 이력·JPA/API 동작을 증명하지 않는다.

- 2026-09-22 리뷰 수정 후 `./gradlew check --offline --no-daemon --console=plain` 통과. Spotless·Checkstyle 포함, JUnit 1,365개, 실패·오류 0개, 환경 조건 생략 9개. `git diff --check` 통과. 운영 배포와 실기기 검증은 수행하지 않았다.
