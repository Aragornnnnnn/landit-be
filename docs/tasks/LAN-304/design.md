<!-- LAN-304 프리톡 캐릭터 연동 설계 문서 -->
# LAN-304 프리톡 캐릭터 연동 설계

## 목표

- 세션 시작 요청의 `characterId`를 필수로 검증하고 세션에 저장한다.
- 시작·목록·상세 응답에서 같은 `characterId`를 반환한다.
- 캐릭터에 대응하는 활성 TTS 음성을 시작 응답에 반환한다.
- opening, turn, closing, inner-thought AI 요청에 서버가 저장한 `characterId`를 전달한다.

## 설계

- `FreeTalkCharacter` enum은 프리톡 API가 허용하는 공개 식별자만 소유한다.
- 공용 `conversation_character` 테이블이 캐릭터와 `tts_voice`의 현재 매핑을 소유한다.
- `free_talk_session.character_id`에는 enum 이름이 아니라 FE 계약값인 소문자 식별자를 저장한다.
- `scenario.character_id`와 `free_talk_session.character_id`는 같은 공용 캐릭터를 참조한다.
- 기존 `scenario_language_variant.tts_voice_id`는 캐릭터로 역매핑한 뒤 제거한다.
- 기존 음성이 캐릭터로 역매핑되지 않거나 한 시나리오에 여러 캐릭터 음성이 있으면 마이그레이션을 중단한다.
- 시나리오 API 응답은 기존 `ttsVoice` 계약을 유지하고 내부 조회만 캐릭터를 경유한다.
- TTS 음성은 공용 캐릭터에 연결된 활성 `tts_voice`를 조회한다.
- AI에는 임의 프롬프트가 아닌 `characterId`만 전달해 프롬프트 정책 소유권을 AI 서버에 둔다.
- 기존 세션 데이터는 현재 기본 음성과 일치하는 `chloe`로 마이그레이션한다.

## 검증

- 세 캐릭터의 요청·저장·응답·TTS 매핑을 통합 테스트한다.
- V54의 기존 시나리오 음성 데이터가 V55에서 정확히 백필되고 모호하거나 미매핑된 데이터는 적용을 중단하는지 확인한다.
- 시나리오 목록·오늘의 시나리오·세션 시작의 기존 `ttsVoice` 응답을 유지하는지 확인한다.
- 지원하지 않거나 누락된 캐릭터 요청이 400인지 확인한다.
- AI 요청에 저장된 캐릭터가 전달되는지 서비스 및 클라이언트 테스트로 확인한다.
