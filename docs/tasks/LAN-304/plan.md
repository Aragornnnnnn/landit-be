<!-- LAN-304 백엔드 구현 계획 문서 -->
# LAN-304 백엔드 구현 계획

- [x] 캐릭터 도메인과 Flyway 컬럼을 추가한다.
- [x] 시작 요청 검증과 캐릭터별 TTS 조회를 실패 테스트부터 구현한다.
- [x] 시작·목록·상세 응답에 `characterId`를 추가한다.
- [x] 모든 대화 생성 AI 요청에 `characterId`를 전파한다.
- [x] OpenAPI 계약과 API·서비스·DB 테스트를 보강한다.
- [x] `./gradlew check`를 통과시킨다.

## 검증 결과

- 2026-08-15: `./gradlew check` 성공.
