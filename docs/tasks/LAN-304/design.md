<!-- LAN-304 프리톡 캐릭터 연동 설계 문서 -->
# LAN-304 프리톡 캐릭터 연동 설계

## 목표

- 세션 시작 요청의 `characterId`를 필수로 검증하고 세션에 저장한다.
- 시작·목록·상세 응답에서 같은 `characterId`를 반환한다.
- 캐릭터에 대응하는 활성 TTS 음성을 시작 응답에 반환한다.
- opening, turn, closing, inner-thought AI 요청에 서버가 저장한 `characterId`를 전달한다.

## 설계

- `FreeTalkCharacter` enum이 공개 식별자와 TTS provider voice ID의 고정 매핑을 소유한다.
- `free_talk_session.character_id`에는 enum 이름이 아니라 FE 계약값인 소문자 식별자를 저장한다.
- TTS 음성은 DB의 활성 음성을 provider voice ID로 조회한다. 숫자 PK에는 의존하지 않는다.
- AI에는 임의 프롬프트가 아닌 `characterId`만 전달해 프롬프트 정책 소유권을 AI 서버에 둔다.
- 기존 세션 데이터는 현재 기본 음성과 일치하는 `chloe`로 마이그레이션한다.

## 검증

- 세 캐릭터의 요청·저장·응답·TTS 매핑을 통합 테스트한다.
- 지원하지 않거나 누락된 캐릭터 요청이 400인지 확인한다.
- AI 요청에 저장된 캐릭터가 전달되는지 서비스 및 클라이언트 테스트로 확인한다.
