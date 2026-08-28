<!-- LAN-351 시나리오 TTS 비용 절감 설계 문서 -->
# LAN-351 시나리오 TTS 비용 절감 설계

## 목표

- 고정 질문은 미리 생성한 음원을 재사용하고 AI 맞장구만 TTS로 변환한다.
- 기존 전체 메시지 텍스트 계약은 유지한다.
- FE가 맞장구 음성과 고정 질문 음성을 메모리에서 이어 재생할 정보를 제공한다.

## 설계

- `landit-ai`의 다음 메시지 응답은 맞장구와 번역을 별도 필드로 반환한다.
- BE는 `맞장구 + 고정 질문`을 조합해 기존 `content`, `translatedContent`로 저장하고 반환한다.
- 일반 턴은 `ttsText=맞장구`, `questionAudioUrl=고정 질문 음원 URL`을 반환한다.
- 종료 턴은 `ttsText=전체 종료 메시지`, `questionAudioUrl=null`을 반환한다.
- `scenario_question_language_variant.audio_url`에 CloudFront 전체 URL을 `NOT NULL`로 저장하고 기존 질문을 모두 백필한다.
- 기존 `tts_voice` ID와 FK를 유지한다. Chloe는 `deepgram/aura-2`와 `aura-2-luna-en`으로 변경하고 Marco와 Teddy의 기존 Aura 2 매핑을 검증한다.

## API 범위

- `GET /api/v1/scenarios/daily`: AI-first의 `openingPreview.questionAudioUrl`을 반환한다.
- `POST /api/v1/scenarios/{scenarioId}/sessions`: AI-first의 `currentMessage.questionAudioUrl`을 반환한다.
- `POST /api/v1/sessions/{sessionId}/messages`: `nextMessage.ttsText`, `nextMessage.questionAudioUrl`을 반환한다.
- USER-first의 첫 질문 음원은 첫 사용자 발화 이후 `nextMessage.questionAudioUrl`로 제공한다.
- 관리자 세션 시작은 사용자 세션 시작과 같은 응답 DTO를 사용하므로 동일한 additive 필드를 반환하되 별도 관리자 로직은 추가하지 않는다.

## 제외 범위

- FE의 TTS 호출, 음원 캐시, 연속 재생 구현.
- 현재 FE가 사용하지 않는 `GET /api/v1/scenarios`와 관리자 목록 API 변경.
- 합성 음원 생성 및 S3 저장.

## 검증

- AI 다음 메시지 계약과 BE 원격 클라이언트 파싱을 테스트한다.
- 일반 턴, 종료 턴, AI-first, USER-first의 API 응답을 통합 테스트한다.
- Flyway 백필·`NOT NULL`·세 캐릭터 음성 매핑을 DB 통합 테스트한다.
- AI 전체 테스트와 BE `./gradlew check`를 실행한다.
