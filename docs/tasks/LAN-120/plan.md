# LAN-120 NPS 제출 API 구현 계획

## 목표

인증된 사용자가 `POST /api/v1/nps`로 1~5점의 만족도와 선택 의견을 제출하면 매 요청을 별도 `nps_response` 레코드로 저장한다.

## 확정 사항

- `origin/develop` 기준 `feat/LAN-120`에서 작업한다.
- `score`는 `Integer`로 받고 `@NotNull`, `@Min(1)`, `@Max(5)`를 적용한다.
- `opinionText`가 `null`, 빈 문자열 또는 공백 문자열이면 `null`로 저장하고, 그 외 문자열은 그대로 저장한다.
- 동일 사용자의 반복 제출과 동일 내용 제출을 모두 저장한다.
- 기존 `nps_response` 테이블과 `NpsResponse` 엔티티를 사용하며 Flyway 마이그레이션과 유니크 제약은 추가하지 않는다.
- 새 의존성이나 별도 UseCase, Port, Adapter는 추가하지 않는다.

## 작업 순서

### 1. API 통합 테스트

새 파일은 `src/test/java/com/landit/landitbe/nps/NpsApiIntegrationTests.java`다.

- [ ] 정상 요청이 인증 사용자 ID, 점수, 의견으로 저장되는 실패 테스트를 작성한다.
- [ ] 의견 누락, 빈 문자열, 공백 문자열이 `null`로 저장되는지 검증한다.
- [ ] 같은 사용자의 반복 제출과 같은 내용 제출이 모두 별도 레코드로 저장되는지 검증한다.
- [ ] 점수 `1`, `5`는 저장되고 범위 밖 값과 누락은 `400 VALIDATION_FAILED`인지 검증한다.
- [ ] 인증 없는 요청은 `401`인지 검증한다.
- [ ] 성공 응답이 `201 Created`와 `ApiResponse.success(null)`인지 검증한다.

### 2. NPS 저장 API

새 파일은 `nps/api/NpsController.java`, `nps/api/dto/NpsSubmitRequest.java`, `nps/application/NpsService.java`, `nps/infrastructure/NpsResponseRepository.java`다. 기존 `nps/domain/NpsResponse.java`에는 저장용 생성자만 추가한다.

- [ ] Controller에서 `AuthUserPrincipal.userId()`와 검증된 요청을 Service에 전달한다.
- [ ] 트랜잭션 Service에서 `opinionText == null || opinionText.isBlank()`일 때만 `null`로 정규화하고 새 엔티티를 저장한다.
- [ ] 기존 제출 조회나 중복 차단 로직 없이 요청마다 `save`한다.
- [ ] `201 Created`와 빈 공통 응답을 반환한다.
- [ ] 요청 필드, `201`, `400`, `401`, `500` 응답을 OpenAPI에 문서화한다.

### 3. 검증

```bash
./gradlew test --tests com.landit.landitbe.nps.NpsApiIntegrationTests
./gradlew test
git diff --check origin/develop...HEAD
```

기존 V6 마이그레이션의 점수 체크 제약, nullable `opinion_text`, 사용자별 유니크 제약 부재도 통합 테스트와 SQL 검토로 확인한다.

## 커밋 단위

1. `feat: NPS 제출 API 추가`.
