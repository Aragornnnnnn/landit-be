<!-- LAN-351 AI·백엔드 구현 계획 문서 -->
# LAN-351 AI·백엔드 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AI 맞장구만 TTS로 변환하고 고정 질문 음원을 재사용할 수 있는 AI·BE 계약을 제공한다.

**Architecture:** `landit-ai`는 맞장구만 반환하고 BE가 고정 질문과 결합해 기존 메시지를 저장한다. BE는 FE가 합성할 `ttsText`와 `questionAudioUrl`을 세 API에 추가한다.

**Tech Stack:** Python 3.12/FastAPI/Pydantic, Java 21/Spring Boot/JPA/Flyway/H2/PostgreSQL.

**Spec:** `docs/tasks/LAN-351/design.md`

## Global Constraints

- 기존 `content`, `translatedContent`, `tts_voice` PK/FK를 유지한다.
- 최종 CloudFront URL 매핑을 받기 전에는 임시 URL로 Flyway 마이그레이션을 작성하지 않는다.
- FE, 합성 음원 저장, `GET /api/v1/scenarios`는 변경하지 않는다.

---

### Task 1: AI 맞장구 계약 분리

**Files:**
- Modify: `landit-ai/app/models/conversation.py`
- Modify: `landit-ai/app/conversation/application/next_message_service.py`
- Test: `landit-ai/tests/test_conversation_api.py`

**Interfaces:**
- Produces: `NextMessageResponse(acknowledgement, translatedAcknowledgement, goalCompletionStatus)`.

```python
class NextMessageResponse(BaseModel):
    acknowledgement: str
    translatedAcknowledgement: str
    goalCompletionStatus: GoalCompletionStatus
```

- [ ] AI API 테스트를 `acknowledgement`, `translatedAcknowledgement` 계약으로 먼저 변경한다.
- [ ] `python -m unittest tests.test_conversation_api.NextMessageApiTests`를 실행해 기존 구현의 실패를 확인한다.
- [ ] 프롬프트·복구 로직·Pydantic 응답을 맞장구 전용으로 최소 변경한다. 고정 질문은 문맥으로만 유지한다.
- [ ] 대상 테스트와 `python -m unittest discover -s tests`를 통과시킨다.
- [ ] `feat/LAN-351`에 AI 계약 변경을 커밋한다.

### Task 2: 질문 음원과 캐릭터 음성 마이그레이션

**Files:**
- Create: `src/main/resources/db/migration/V58__add_scenario_question_audio_url.sql`
- Modify: `src/main/java/com/landit/landitbe/feature/content/domain/ScenarioQuestionLanguageVariant.java`
- Modify: 질문·날짜별 시나리오·세션 시작 Projection과 Query Repository.
- Test: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`
- Test: 질문 조회·날짜별 시나리오·세션 시작 통합 테스트 fixture.

**Interfaces:**
- Produces: `NextQuestionContext.questionAudioUrl`, 시작·날짜별 조회 Projection의 `questionAudioUrl`.

- [ ] DB 테스트에 `audio_url NOT NULL`, 전체 URL 백필, Chloe/Marco/Teddy 매핑 기대값을 추가한다.
- [ ] `./gradlew test --tests 'com.landit.landitbe.DatabaseSchemaIntegrationTests' --tests 'com.landit.landitbe.feature.content.ScenarioQuestionQueryRepositoryIntegrationTests' --tests 'com.landit.landitbe.feature.content.DailyScenarioApiIntegrationTests' --tests 'com.landit.landitbe.feature.session.ScenarioSessionApiIntegrationTests'`를 실행해 실패를 확인한다.
- [ ] 검증된 URL 매핑으로 V58을 작성하고 엔티티·Projection·조회 쿼리에 `audioUrl`을 전달한다.
- [ ] 관련 DB·조회 테스트를 통과시킨다.
- [ ] 스키마·콘텐츠 조회 변경을 커밋한다.

### Task 3: BE 메시지 조합과 API 계약

**Files:**
- Modify: `src/main/java/com/landit/landitbe/feature/session/client/ai/AiNextMessageResult.java`
- Modify: `RemoteAiConversationClient.java`, `LocalAiConversationClient.java`
- Modify: `SessionMessageAiGenerator.java`, `GeneratedMessageService.java`
- Modify: `SessionMessageSubmitResponse.java`, `SessionStartResponse.java`, `DailyScenarioResponse.java`
- Test: `RemoteAiConversationClientTest.java`, `SessionMessageServiceTest.java`
- Test: `ScenarioSessionApiIntegrationTests.java`, `DailyScenarioApiIntegrationTests.java`

**Interfaces:**
- Consumes: AI 맞장구 계약과 질문 `questionAudioUrl`.
- Produces: 일반 턴의 `ttsText=acknowledgement`, 종료 턴의 `ttsText=content`, 고정 질문이 있을 때만 `questionAudioUrl`.

```java
record NextMessageResponse(
    Long messageId,
    int turnNumber,
    int messageSequence,
    String role,
    String content,
    String translatedContent,
    String ttsText,
    String questionAudioUrl) {}
```

- [ ] 원격 AI 파싱, 전체 메시지 결합, AI-first·USER-first·종료 턴 응답 테스트를 먼저 변경한다.
- [ ] `./gradlew test --tests 'com.landit.landitbe.feature.session.client.ai.RemoteAiConversationClientTest' --tests 'com.landit.landitbe.feature.session.ScenarioSessionApiIntegrationTests' --tests 'com.landit.landitbe.feature.content.DailyScenarioApiIntegrationTests'`를 실행해 실패를 확인한다.
- [ ] BE가 맞장구와 고정 질문을 결합해 저장하고 세 API 응답에 새 필드를 추가한다.
- [ ] 대상 테스트를 통과시킨 뒤 `./gradlew check`를 실행한다.
- [ ] API 계약 변경과 검증 결과를 커밋한다.
