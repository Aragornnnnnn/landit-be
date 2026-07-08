# LAN-91 시나리오 세션 시작/종료 체크리스트

- [x] LAN-91 요구사항과 SayNow BE 세션 시작/중도 종료 흐름 확인.
- [x] 구현 전 판단 사항 정리.
- [x] 실행 계획 문서 작성.
- [x] 세션 시작 통합 테스트를 먼저 추가하고 실패 확인.
- [x] 세션 중도 종료 통합 테스트를 먼저 추가하고 실패 확인.
- [x] `POST /api/v1/scenarios/{scenarioId}/sessions` API 구현.
- [x] AI first 시작 메시지 저장과 응답 구현.
- [x] USER first 시작 안내 응답 구현.
- [x] `totalQuestionCount` 기반 세션 진행도 응답 구현.
- [x] 시나리오 없음, 카테고리 잠금, 시나리오 잠금 에러 처리.
- [x] 시작 시 사용자 시나리오 진행도 보장.
- [x] 같은 사용자 동시 시작 시 진행도 중복 생성 경합 방지.
- [x] `PATCH /api/v1/sessions/{sessionId}/end` API 구현.
- [x] 중도 종료 시 `INTERRUPTED`, `USER`, `USER_ENDED`, `ended_at` 저장.
- [x] 다른 사용자 세션 접근 차단.
- [x] 완료 또는 종료된 세션 중도 종료 요청 409 처리.
- [x] Swagger 문서 annotation 추가.
- [x] 관련 테스트 실행.
- [x] `./gradlew test` 실행.
- [x] 논리 단위 커밋 생성.

## 리뷰 반영

- [x] 활성 사용자 잠금 조회 메서드를 `ACTIVE` 전용 의미로 정리.
- [x] Repository 직접 의존 이유를 UseCase에 짧게 기록.
- [x] 시나리오 세션 시작 흐름과 AI 메시지 저장 메서드 분리.
- [x] 시나리오 세션 시작 UseCase와 세션 중도 종료 UseCase 분리.
- [x] Swagger Docs Interface 분리는 별도 이슈 범위로 분리.
- [x] 리뷰 반영 후 관련 테스트 재실행.
- [x] API 리소스 경계에 맞춰 시나리오 세션 시작 Controller와 세션 종료 Controller를 분리.

## 2026-07-09 후속 PR rebase

- [x] `feat/LAN-91-base`를 PR #3 merge 이후 최신 `origin/develop` 위로 rebase한다.
- [ ] `feat/LAN-91-start`를 갱신된 `feat/LAN-91-base` 위로 rebase한다.
- [ ] `feat/LAN-91-end`를 갱신된 `feat/LAN-91-start` 위로 rebase한다.
- [ ] PR #5 base를 `develop`으로 변경한다.
- [ ] `git diff --check`를 실행한다.
- [ ] LAN-91 관련 테스트와 전체 테스트를 실행한다.
- [ ] 세 LAN-91 브랜치를 `--force-with-lease`로 push한다.
- [ ] PR #5, #6, #7 mergeable 상태를 재확인한다.
