# LAN-593 회원 탈퇴 개인정보 파기

## 상태와 승인 범위

- 사용자 승인: LAN-593 착수 및 #227(LAN-591) 기반 작업.
- 기준: `feat/LAN-591`의 `8a18137a17e88d8e1b7b360866ce16aa47dddb8e`.
- 작업 브랜치: `feat/LAN-593`. 선행 PR 병합 전에는 LAN-591을 비교 기준으로 사용한다.
- 요구사항은 개인정보 파기, 법정 보관 분리, 기존 탈퇴자 정리, 외부 서비스·백업 처리 확인이다.
- 이 문서는 저장 위치 조사, 승인된 처리 기준, 구현 상태와 남은 결정을 관리하는 단일 문서다. 법정 보관 세부 분류와 외부 파기까지 구현 완료한 것으로 취급하지 않는다.
- 추가 사용자 결정: 문의·첨부파일을 삭제하지 않고 분류 라벨도 추가하지 않는다. 회원 행은 유지하며 닉네임 원본을 `탈퇴한 사용자`로 덮어쓴다. 화면 표시만 가리거나 원본 닉네임 사본을 남기는 방식은 사용하지 않는다.
- 운영 데이터 삭제, 배포, 외부 사업자 데이터 삭제는 아직 실행하지 않았다.

## 기준과 현재 차이

공개 개인정보처리방침 v1.1(2026-09-11 시행)의 1·3·4·5·7항이 기준이다.

- https://www.landit.im/privacy
- 개인정보 보호법 제21조: https://www.law.go.kr/lsLinkCommonInfo.do?chrClsCd=010202&lsJoLnkSeq=1034516739
- 파기 방법: https://law.go.kr/lsLinkCommonInfo.do?lsJoLnkSeq=1025293445
- 법정 거래기록: https://law.go.kr/lumLsLinkPop.do?lspttninfSeq=63460

작업 기준 커밋의 `AuthService.withdraw`는 프로필 잠금·탈퇴 상태 전환 → 장기기억 삭제 → 모든 소유 푸시 토큰 비활성화 → Refresh Token 폐기 → OAuth 연결 상태 변경 순이다. 프로필 이메일·닉네임, OAuth 식별자·이메일, 토큰 값과 대화 이력이 남는다. 푸시 비활성화는 LAN-591에서 추가됐지만 개인정보 파기까지 수행하지 않는다.

화면 마스킹이나 사용자 ID만 제거한 자유서술 데이터는 파기 또는 익명화 완료로 판단하지 않는다. 이번 작업은 개인별 데이터를 삭제하는 방향을 우선하며, 익명 통계 생성은 별도 필요성이 확인되기 전까지 추가하지 않는다.

## 저장 위치 및 처리 후보

아래는 기준 커밋의 소스·Flyway 조사 결과다. 운영 DB의 존재·건수·배포 상태를 증명하지 않는다. PostgreSQL 전용 마이그레이션, 반복 마이그레이션, 과거 스키마의 미사용 테이블도 포함한다.

| 데이터 | 저장 위치 | 처리 설계안 |
| --- | --- | --- |
| 계정 | `user_profile` | 회원 ID·행·탈퇴 상태는 유지. 닉네임 원본은 `탈퇴한 사용자`, 이메일·프로필 이미지는 `NULL`로 덮어쓴다. |
| 소셜 계정·Apple 이전 | `oauth_identity`, `apple_user_migration` | 과거 연결 해제 행까지 provider user ID를 `withdrawn`, 이메일을 `NULL`, 상태를 `UNLINKED`로 덮어쓴다. Apple 이전 식별자 사본은 제거한다. |
| 인증 | `refresh_token` | 즉시 인증 무효화 후 토큰 해시·계정 연결 제거. JWT 사용 가능 여부도 검증한다. |
| 기기 | `user_push_token` | 현재 소유자 기준으로 모든 설치·구형 토큰을 파기. 다른 계정으로 이미 이전된 토큰은 보존한다. |
| 발송·예약 | `push_delivery`, `notification_job`, `user_notification_state`, `admin_push_target`, `admin_push_campaign_user`, `learning_notification_slot` | 예약 취소·후속 발송 차단 후 대상 이메일·토큰·개인 연결·개별 payload 제거. 큐에 이미 전달된 작업도 사용자 상태를 다시 검사한다. |
| 공통 대화 | `learning_session`, `session_history`, `session_history_message`, `session_history_artifact` | 원문·번역·속마음·음성 분석 JSON 및 개인별 세션 제거. artifact의 파일 삭제 대상을 먼저 영속 기록한다. |
| 시나리오 피드백 | `scenario_session`, `session_history_summary_feedback`, `session_history_message_feedback`, `message_feedback_work`, `user_level_assessment` | 피드백·평가·작업 상태 제거. #228의 추가 JSON도 포함되도록 행 단위 삭제를 우선한다. |
| 프리톡 파생 정보 | `free_talk_session`, `free_talk_context_summary`, `free_talk_session_summary`, `free_talk_message_feedback`, `free_talk_follow_up`, `free_talk_expression_reuse`, `free_talk_pattern_usage`, `free_talk_daily_speaking_usage` | 원문을 재구성할 수 있는 요약·인용·후속 질문·개인별 이용 기록 제거. |
| 기억 | `conversation_memory`, `conversation_memory_source`, `free_talk_memory_retrieval` | 본문·임베딩·검색 기록 제거. 기억 교체의 자기 참조 FK를 PostgreSQL에서 검증한다. |
| 개인 생성 표현 | `writing_expression.owner_user_profile_id`, `free_talk_session_expression` | 해당 사용자가 소유한 생성 표현과 연결만 제거. 공용 콘텐츠는 보존한다. |
| 진도·복습 | `user_scenario_progress`, `user_scenario_access`, `user_writing_expression_completion`, `user_learning_expression`, `review_item`, `review_item_result`, `expression_review`, `expression_review_question`, `expression_review_submission` | 개인별 기록·답안 제거. 공유 표현·시나리오 FK는 삭제 전파 대상에서 제외한다. |
| 개인 설정·기타 이력 | `user_alarm`, `user_quest`, `user_character`, `user_learning_activity_summary`, `user_daily_activity`, `nps_response`, `learning_access_grant`, `free_scenario_reservation` | 개인별 데이터 제거. 미사용 테이블의 잔존 데이터도 누락하지 않는다. |
| 문의·이미지·편지 | `mailbox_feedback`, `mailbox_feedback_attachment`, `mailbox_letter_recipient`, `mailbox_letter_read`, 개별 `mailbox_letter` | 사용자 결정에 따라 기존 내용·첨부파일·연결을 유지한다. 보관 분류나 라벨을 추가하지 않는다. 보관기간 정책 검토는 별도 과제로 남는다. |
| 결제 | `subscription_event`, 프로필 구독 필드 | 최소 거래기록을 별도 보관. 탈퇴 뒤 환불·만료·TRANSFER 이벤트를 일반 프로필로 되살리지 않는다. |
| 관리자 기록 | `admin_audit_log`, `admin_push_campaign.created_by`, `app_version.updated_by_user_profile_id` | 서비스 설정·다른 회원 발송 캠페인을 삭제하지 않는다. 관리자 탈퇴와 감사 기록 보존 근거를 별도 분류하고 식별 연결·본문 포함 여부를 확인한다. |

## 삭제 경계와 동시성

1. 프로필을 먼저 잠가 탈퇴 의사를 확정한다. LAN-591의 프로필 → 토큰 ID 오름차순 잠금 규칙을 유지한다.
2. 법정 보관 스냅샷과 외부 삭제 작업을 같은 DB 트랜잭션에 기록한다. 삭제 후 외부 식별자나 파일 경로를 복원하려고 하지 않는다.
3. 각 업무의 공개 Service로 해당 업무 데이터 파기를 요청한다. 다른 업무 Entity·Repository를 직접 참조하지 않는다.
4. 외래키 삭제 전파를 채택할 경우 명시적인 소유 관계만 변경하고, 공유 콘텐츠·편지·관리자 설정의 역방향 삭제 전파는 금지한다. 스키마 전체에 CASCADE를 일괄 적용하지 않는다.
5. 프로필 행은 삭제하지 않는다. 문의 FK와 탈퇴 후 결제 이벤트 라우팅을 유지한다. 현재 `RevenueCatWebhookService.resolveUserId`는 기존 회원 ID를 사용하므로 구독 연속성 변경은 별도 보관 설계와 함께 검토한다.
6. 식별 원본을 덮어쓴 프로필 행이 남으므로 탈퇴 후 저장 직전 활성 사용자 확인·잠금이 필요하다. 현재 일부 프리톡·평가·메시지 경로에는 이 잠금이 있지만 전체 경로에 있다고 가정하지 않는다.
7. 외부 파일·사업자 삭제는 DB 트랜잭션 밖에서 실행하고 실패·재시도·완료를 영속 추적한다. 요청 전송과 파기 완료를 구분한다.

## 법정 보관에서 확정할 항목

보관 기간을 탈퇴 시점부터 일괄 재계산하지 않는다. 거래·공급·분쟁 기록별 기산점과 종료일을 정하며, 승인된 법정 보관 목록만 별도 저장소로 이동한다.

| 기록 | 요구사항의 기간 | 구현 전 확인 |
| --- | --- | --- |
| 계약·청약철회, 결제·공급 | 5년 | 원거래를 특정할 최소 항목, 기산점, 기존 자료의 복구 가능 범위. |
| 소비자 불만·분쟁 | 3년 | 일반 의견과의 분류, 미해결 분쟁의 보관 종료 판단, 기존 미분류 데이터 처리. |
| 표시·광고 | 6개월 | 광고 문서 자체의 보관과 회원별 수신 이력을 구분. 모든 발송 이력을 자동으로 보관 대상으로 삼지 않는다. |

### 문의 보존 및 원본 덮어쓰기 결정

문의·첨부파일은 탈퇴 시 삭제하지 않는다. 분류 라벨도 추가하지 않는다. 문의 작성자와의 FK를 유지하기 위해 회원 행을 남기고 닉네임 원본을 `탈퇴한 사용자`로 바꾼다. 이메일·이미지·소셜 식별 원본도 저장 값 자체를 정리한다.

이 결정은 문의의 무기한 보관 근거를 확정한 것이 아니다. 문의 본문·첨부파일과 결제 이력이 회원 ID에 연결된 채 남으므로 전체 데이터를 익명화했다고 표현하지 않는다. 보관기간 및 법정 보관 분리는 남은 검토 범위다.

### 결제 기록에서 발견한 추가 공백

현재 웹훅 DTO와 `subscription_event`는 RevenueCat 이벤트 ID·상품·금액·통화·발생 시각을 저장하지만 `transaction_id`, `original_transaction_id`를 바인딩·저장하지 않는다. 이벤트 ID를 스토어 거래 ID로 취급하지 않는다. 스토어 환불·거래 확인에 필요한 식별자와 기존 자료 보완 경로를 설계에 포함해야 한다.

## 외부·백업 처리

- 확인 대상: Amplitude, Sentry, Supabase 설문, RevenueCat, Deepgram, OpenRouter 및 실제 하위 처리자, Vercel, Expo, S3, DB 백업·로그.
- 서비스별 저장 항목, 사용자 식별 방식, 계약·보유 설정, 삭제 API 유무, 완료 확인 방법을 조사한다. API 호출이 없는 현재 상태를 자동으로 미보관으로 판단하지 않는다.
- 삭제 작업의 상태는 미확인·대기·진행·실패·완료를 구분한다. 수동 처리 필요 건은 완료로 자동 전환하지 않는다.
- DB에는 원본 음성 미보관 정책과 별개로 artifact 경로를 저장하는 구조가 있다. 실제 사용 여부와 저장소 소유권을 확인한다.
- 백업 보유기간·접근제한·만료 파기와 복원 후 재삭제 절차를 runbook에 반영한다. 백업이라는 이유만으로 법정 보관 예외를 적용하지 않는다.

## 구현 순서

1. 승인된 회원 식별 원본 덮어쓰기·문의 보존을 반영하고, 법정 보관 필드·기산점 및 외부 삭제 범위 조사를 이어간다.
2. 보관 저장소·파기 작업 상태·명시적 삭제 계약 추가.
3. 탈퇴 흐름에 보관·삭제·외부 작업 등록을 연결하고 늦은 쓰기와 결제 이벤트 라우팅 수정.
4. 기간 만료 파기, 실패 재시도, 기존 탈퇴자 dry-run·계정별 재실행 가능한 정리 명령 구현.
5. PostgreSQL 관계·동시성 및 API 회귀 검증, 문서와 운영 검증 절차 반영.

현재 열린 PR에는 V132까지 있으므로 새 버전은 작성 직전에 다시 확인한다. V126~V132를 포함하지 않은 상태에서 높은 버전만 운영 DB에 먼저 적용하지 않는다. 이 문서에서 마이그레이션 번호를 예약하지 않는다.

## 현재 구현 상태

- 신규 탈퇴 요청에서 회원 행·문의·첨부파일을 유지하며 닉네임·이메일·이미지 원본을 덮어쓴다.
- 모든 OAuth 행의 원본 식별자를 덮어쓰고 Apple 이전 식별자 사본 및 만료·폐기된 항목을 포함한 Refresh token 해시를 제거한다. 기존 프로필 잠금과 단일 트랜잭션을 유지한다.
- 독립 리뷰에서 확인한 Apple 이전 배치 중단 문제를 보완했다. Apple 응답 대기 중 탈퇴해 이전 행이 제거된 대상은 실패 기록 후처리에서 연결 해제와 행 소멸을 확인하고 건너뛴다. 활성 계정의 행 소실과 DB 오류는 정상 취소로 취급하지 않는다.
- 장기기억 삭제와 소유 푸시 토큰 비활성화는 기존 동작을 유지한다.
- 이번 변경은 스키마 변경이 없다. 기존 탈퇴 회원 소급 정리, 대화·피드백 및 푸시 토큰 원본 파기, 법정 보관 분리·기간 만료 처리, 외부 사업자·파일·백업 처리는 아직 구현하지 않았다.

## 검증 계획과 결과

- [x] 기준 커밋에서 `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew check --offline --console=plain` 통과(2026-09-29).
- [x] 회원 식별 원본 덮어쓰기·Apple 이전 사본 및 Refresh token 제거·문의와 첨부파일 보존·다른 회원 이메일 보존·동일 소셜 계정 재가입을 통합 검증.
- [x] Apple 이전 준비·완료 단계와 API 성공·실패 응답을 조합한 탈퇴 경쟁 조건 4건을 수정 전 실패로 재현했다. 활성 계정의 이전 행만 사라진 경우에는 계속 오류를 반환하는 경계 사례 2건도 회귀 테스트에 추가했다.
- [ ] 두 계정과 공유 콘텐츠를 함께 생성해 나머지 데이터 파기까지 검증.
- [ ] 계정 전환된 기기 토큰, 다른 회원 편지, 공용 표현·시나리오·앱 버전 정책 보존 검증.
- [ ] 개인정보 파기·법정 보관·외부 작업 등록 중 실패 시 트랜잭션 원자성 검증.
- [ ] 메시지·요약·기억·평가·푸시 등록과 탈퇴의 동시 실행 및 늦은 작업 완료 검증.
- [ ] 탈퇴 후 결제 이벤트와 재가입 계정을 혼동하지 않는지 검증.
- [ ] 실제 PostgreSQL의 FK·JSONB·vector·잠금 동작 검증.
- [ ] 법정 보관 기한 경계, 재시도, 기존 탈퇴자 재실행 및 dry-run 무변경 검증.
- [x] 계정 원본 덮어쓰기 구현 후 `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew spotlessApply check --offline --console=plain` 통과(2026-09-29): 1,806건, 실패·오류 0건, 건너뜀 12건. 실제 PostgreSQL 검증과 운영 적용은 포함하지 않는다.
- [x] 독립 리뷰 P2 수정 후 같은 전체 검사 통과: 1,812건, 실패·오류 0건, 건너뜀 12건. 독립 리뷰어의 기존 H2 재현기에서도 다음 정상 회원의 이전 완료를 확인했으며 재리뷰 추가 지적은 없다. PostgreSQL 실제 동시성과 운영 적용은 미검증이다.
- [ ] 배포된 버전·운영 삭제 결과·외부 사업자 완료 상태 별도 확인.

기준 검사 로그는 작업 환경의 `/tmp/lan593-baseline.log`에 있으며 영구 증빙 파일이 아니다. 운영 검증과 외부 파기 완료 기준은 아직 달성하지 않았다.
