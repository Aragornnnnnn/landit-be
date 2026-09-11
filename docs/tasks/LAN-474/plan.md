# LAN-474 배포 중 학습 보존.

기준은 origin/develop `36a7136e`와 열린 PR #181·#182다. 해당 PR은 이 작업 브랜치에만 통합했으며 원격 develop에는 병합하지 않았다.

## 구현.

- 첫 무료 시나리오를 시작할 때 사용자 잠금 아래 한 세션에 예약한다. 중도 종료·서버 오류는 같은 세션에서 재개하며 원래 시작 후 24시간을 연장하지 않는다.
- 시나리오·스몰톡·표현 시작 권한을 DB에 저장한다. 구독 변경 뒤에도 유효한 기존 학습은 이어가고, 접수한 작업의 결과 저장은 새 시작 제한과 분리한다.
- AI 메시지 평가 요청·완성 결과·평가 근거를 DB에 보관한다. 기존 백그라운드 호출을 사용하므로 일반 턴의 다음 질문은 기다리지 않는다. 임대 만료 재시도와 이전 응답 차단을 적용한다.
- AI 캐시가 없으면 완료 세션의 저장 발화로 평가를 재예약한다. 최종 AI 통신 실패는 빈 결과로 확정하지 않고 재시도한다. 스몰톡 표현 PREPARING도 만료된 시도만 복구한다.
- 관리자 GET/PUT `/api/v1/admin/subscription-policy`로 `OFF`, `REVIEW`, `ALL`, `effectiveAt`, `reviewUserIds`, `newStartsPaused`를 관리한다. PUT은 GET의 `version`을 `expectedVersion`으로 보낸다. DB 정책 저장 후에는 환경변수보다 DB 정책이 우선한다.
- 실제 구독 만료 시각, 샌드박스 무시, AI 내부 인증 헤더와 정상 종료를 연결했다. 기존 RevenueCat TRANSFER 처리를 재사용한다.

## FE 연동.

- 기존 `/api/v1/me/subscription`의 `premium`을 재사용한다. `paymentEnabled`로 공개 여부, `canStartScenario`로 새 대화, `freeScenarioSessionId`로 기존 무료 대화 재개를 구분한다. `newStartsPaused`는 새 시작만 중지한다.
- 시나리오 메시지에 `clientMessageId` UUID를 보내고 오류·응답 유실 시 같은 ID·내용·입력 방식으로 재전송한다. 저장된 응답은 그대로 재생한다. ID 없는 구버전은 미완료 발화 재시도만 지원하며 성공 응답 유실의 완전한 중복 방지는 보장하지 않는다.
- 표현 learning-start의 `learningAttemptId`, `learningExpiresAt`를 저장한다. 연습·발음·완료 요청에 `X-Learning-Attempt-Id`를 유지한다. 완료 저장 시에도 잠금 안에서 ID를 확인한다. 헤더 없는 구버전은 마지막 소유 시도로 판정한다.
- 앱 복귀·로그인 변경·PREMIUM_REQUIRED 뒤에는 구독 정책을 다시 조회한다. FE 구현과 실기기 검증은 이번 변경에 포함하지 않았다.

## 전환과 검증.

- dev 테스트 → 심사(REVIEW) → 숨김 출시(OFF) → 오픈(ALL)을 유지한다. FE는 서버 공개 정책을 반영한 버전이어야 한다. 기능 롤백은 DB 정책 OFF와 FE 표시를 함께 맞추며 원래 오픈 시각·무료 예약·실결제 권한은 보존한다.
- V100~V104는 열린 PR #182의 V98·V99 다음이다. 구 BE가 남은 첫 롤링 교체 중 만든 오픈 전 세션도 서버 시작 기록으로 24시간 유예를 판정한다. 이미 시작한 구버전 표현에는 시작 기록이 없어 소급 유예가 불가능하므로 최초 전환 때 기존 표현을 마치고 새 learning-start를 거치게 한다.
- AI 응답 fixture를 실제 AI 테스트 엔드포인트에서 생성해 DB 저장·최종 요청 재사용을 검증했다. 동시 무료 시작, 중도 재개, 24시간 경계, 동일 메시지 재시도, 임대 교체·늦은 응답, 정책 버전, 심사 대상, 구독 만료, 샌드박스·내부 인증을 테스트한다.
- `./gradlew check`가 통과했다. 테스트 1,162개 중 실패 0·생략 4개이며 Spotless·Checkstyle도 통과했다. `.github/scripts/test`의 배포 revision Python 테스트 3개와 ECS 검증 shell 테스트도 통과했다.
- 운영 workflow의 digest revision 등록에는 배포 역할의 추가 IAM 권한이 필요하다. 해당 IAM 코드 작성은 자동 승인 검토가 거절해 미포함이며, 권한 준비 전 운영 workflow를 실행하면 안 된다. AWS apply·배포·SSM 변경·실결제 검증은 수행하지 않았다.
