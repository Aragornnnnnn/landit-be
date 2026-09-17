# LAN-466 AI·BE 예외 전송 정책 통일

## 승인 범위와 기준

- BE: PR #188 `feat/LAN-488` (`3cc752c0`) 위의 `feat/LAN-466`.
- AI: `develop` (`f247bfe6`) 위의 `feat/LAN-466`. AI 구현·검증은 AI 저장소의 같은 이슈 문서에서 관리한다.
- 예상된 요청 거절(`expected_rejection`), 정상 복구(`recovered`), 최종 실패(`failed`)를 구분한다. 필수 결과 유실, DB·설정·코드 결함은 fallback이 있어도 실패로 보고한다.
- LLM 평가 기준, 기억 후보 제외 정책, 재시도 횟수, API 성공 응답 계약은 변경하지 않는다. HTTP 오류 상태·헤더만 바로잡는다.

## 현재 경로와 적용 정책

| 경로 | 이전 | 변경 기준 |
| --- | --- | --- |
| Logback Sentry appender | ERROR 이벤트, INFO 이상 breadcrumb | 단일 ERROR 전송 경로 유지, 전송 직전 원문·예외 메시지·본문·헤더·breadcrumb 제거, 안전한 태그와 원인 타입·스택 보존 |
| GlobalExceptionHandler | ApiException 5xx만 ERROR, FeatureException 무로그, 405는 500 | 알려진 요청 오류의 상태·헤더 유지, 인증된 앱 계약 위반 보고, 예상된 도메인 거절은 관측만 유지 |
| 메시지 피드백 작업 | 재시도/FAILED/소진 상태의 WARN 또는 무로그 | 재시도 중에는 로그·메트릭, 소진 및 저장 실패는 이벤트 |
| 기억·표현 생성/저장 | WARN 또는 무로그로 실패 종료 | 필수 결과 유실 및 실패 상태 저장 오류 각각 보고 |
| 수준 평가 | fallback 시 WARN, 저장 실패만 ERROR | 평가 실패·만료·등록/조회/저장 결함 보고 |
| 기억 검색 | 모든 RuntimeException을 fallback | 정상 대체와 DB·코드·설정 결함 구분, 결함은 보고 |
| RevenueCat | 인증 설정 누락과 잘못된 외부 인증 모두 401/WARN | 설정 누락은 서버 결함, 외부 인증 불일치는 예상 거절 |

## 구현 순서

1. 안전한 로그·메트릭과 Sentry 최종 필터를 공통화한다.
2. HTTP 오류와 구독 인증을 분류한다.
3. 동기·비동기 기능의 종료/저장 실패 누락을 보완한다. BE→AI 연결 장애는 제외하지 않는다.
4. 외부 전송 없는 이벤트/민감정보/중복/HTTP 계약 테스트와 `./gradlew check`를 실행한다.
5. 독립 리뷰 결과를 반영하고 논리 단위로 커밋한다.

## 검증 기록

- `JAVA_HOME=.../temurin-21.0.7/Contents/Home ./gradlew check --no-daemon`: 성공. 테스트 1,256개, 실패 0, 기존 skip 6개. Spotless·Checkstyle 포함. skip은 PostgreSQL 전용 4개와 실제 FE/AI 연결 2개다.
- 메모리 transport로 실제 Sentry SDK의 이벤트 생성, 예외 원인·스택 보존, 원문·시크릿 제거, 동일 예외 중복 방지 및 독립 연결 장애 전송을 확인했다.
- 피드백 FAILED의 재시도/소진, 이전 시도의 뒤늦은 실패, 코드·설정 결함, 저장 실패, 이미 terminal인 복구 작업을 회귀 테스트했다. HTTP 405 Allow와 인증된 계약 위반의 400 이벤트도 확인했다.
- 독립 리뷰의 부모 취소 과잉 전송, 재시도 중 결함 누락, 다중 recovery 중복 보고 지적을 수정했다. 집중 재리뷰에서 추가 확정 결함이 없었다.
- 배포·실제 외부 전송·운영 이벤트 감소 검증은 이 로컬 구현에 포함하지 않는다.

- 2026-09-17 사용자 요청으로 BE 기준을 `fc349c2b`에서 `3cc752c0`으로 rebase. 세션 Entity 직접 변경 대신 새 conversation Service/snapshot 계약을 유지하며 관측 변경을 복원. AI develop은 `f247bfe6`으로 동일. rebase 전 stash와 backup 브랜치 보존.

## 관측 상세와 운영 확인

- `FailureObservation`은 로그와 Micrometer `landit.failure.outcomes`에 workflow/failure_stage/reason/outcome을 기록한다. request_id는 외부 값을 복사하지 않는 UUID이며 비동기 실행기와 BE→AI 호출로 이어진다. 스케줄러 복구처럼 요청 밖에서 시작한 작업에는 request_id가 없을 수 있다.
- `expected_rejection`, `recovered`, `failed`는 종료 분류이며, `retrying`은 결과가 아직 확정되지 않은 중간 관측이다. 재시도 중에도 코드·설정 결함은 즉시 failed로 남긴다.
- BE는 인증된 사용자 호출의 형식 위반을 앱 계약 이상 신호로 보고한다. 인증 여부는 공식 앱을 완벽히 식별하지 못하므로 보수적인 운영 신호이며 사용자 자격/권한 등 예상 도메인 거절은 별도로 제외한다.
- 웹훅 인증 설정 누락은 503 + configuration failure, 외부 인증 불일치는 기존 401이다. 외부 문자열 대신 정적 이유·검증된 enum만 로그에 남긴다. 구독 처리와 저장 자체의 결함은 공통 예외 경계에서 보고한다.
- SafeSentryAppender는 명시적 ERROR와 SDK 자동 오류를 같은 before_send에서 정제한다. 요청·사용자·breadcrumb·예외 메시지·locals/context 원문을 제거하고 원인 타입·스택을 남긴다. 예외 없는 실패는 workflow/stage/reason별 fingerprint를 사용한다.
- 동일 예외의 로컬 재보고는 제외하며 보상 저장 실패처럼 독립 예외는 남긴다. AI 원인 오류와 BE 결과 유실은 서로 다른 서비스의 사건으로 남을 수 있고 request_id로 비교한다. AI 오류를 통째로 제외하지 않아 연결·타임아웃 장애가 사라지지 않는다.
- 이벤트 저장 정책과 알림 정책은 다르다. Sentry 알림 설정은 변경하지 않았다. 배포 후 실패율과 FAILED 상태, 실패 메트릭 대비 이벤트 누락, 정상 복구 비율을 함께 확인한다.
