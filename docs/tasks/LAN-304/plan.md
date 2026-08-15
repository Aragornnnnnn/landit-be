<!-- LAN-304 백엔드 구현 계획 문서 -->
# LAN-304 백엔드 구현 계획

- [x] 캐릭터 도메인과 Flyway 컬럼을 추가한다.
- [x] 시작 요청 검증과 캐릭터별 TTS 조회를 실패 테스트부터 구현한다.
- [x] 시작·목록·상세 응답에 `characterId`를 추가한다.
- [x] 모든 대화 생성 AI 요청에 `characterId`를 전파한다.
- [x] OpenAPI 계약과 API·서비스·DB 테스트를 보강한다.
- [x] 공용 `conversation_character`와 캐릭터별 TTS FK 매핑을 추가한다.
- [x] 시나리오의 직접 `tts_voice_id` 연결을 `scenario.character_id`로 이전한다.
- [x] 시나리오 API 응답을 변경하지 않고 캐릭터 경유 TTS 조회로 전환한다.
- [x] 프리톡 enum의 음성 식별자 하드코딩을 제거한다.
- [x] `./gradlew check`를 통과시킨다.

## 검증 결과

- 2026-08-15: `./gradlew check` 성공.
- 2026-08-15: 한도 소진 상태에서도 잘못된 `characterId`가 `INVALID_REQUEST`로 우선 처리되는 회귀 테스트와 `./gradlew check` 성공.
- 2026-08-15: 리뷰에서 확인한 기능 간 Repository 직접 참조를 `TtsVoiceService` 공개 경계로 분리하고 `./gradlew check` 성공.
- 2026-08-15: 캐릭터-TTS 매핑을 `conversation_character`로 통합하고 시나리오 API 관련 통합 테스트 94개 성공.
- 2026-08-15: 스키마 백필·FK, 공용 조회 서비스, 전체 회귀를 포함한 `./gradlew check --rerun-tasks --no-daemon` 성공.
- 2026-08-15: 1차 독립 리뷰의 마이그레이션 데이터 유실·API 테스 누락 2건을 반영했다.
- 2026-08-15: V54→V55 백필·충돌·미매핑 테스와 오늘의 시나리오·관리자 목록 TTS 응답 테스를 추가하고 `./gradlew check --rerun-tasks --no-daemon` 성공.
