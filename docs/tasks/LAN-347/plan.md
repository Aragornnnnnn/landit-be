# LAN-347 V1 장기기억 구현 기록

## 범위

V1은 완료된 FreeTalk 세션의 USER 메시지에서 AI 후보를 추출하고, 기존 ACTIVE 기억과 비교해 ADD·SUPERSEDE·IGNORE를 판정한 뒤 세션 상태와 기억·계보를 원자 저장한다.

- V66의 `PREPARING`·`READY`·`FAILED` lifecycle과 `LANDIT_MEMORY_WRITE_ENABLED` feature flag를 유지한다.
- 세션 완료 요청은 dispatcher가 in-process async 작업으로 등록하고 LLM 응답을 기다리지 않는다.
- 후보는 연속 `candidateIndex`, USER provenance, base locale, confidence, validFrom/validTo, 1,536차원 finite embedding과 고정 model을 검증한다.
- 기존 비교 검색은 같은 사용자·캐릭터·memory type 범위에서 최대 3건을 제공한다.
- resolution은 모든 후보를 정확히 한 번 판정하며, SUPERSEDE 대상은 해당 후보 snapshot의 부분집합이고 전체 후보에서 중복되지 않아야 한다.
- snapshot이 변경되면 write service는 zero write로 `STALE`을 반환하고 generation은 재검색·재판정 없이 `FAILED`로 끝낸다.

- PostgreSQL/H2 V65의 `conversation_memory`, `conversation_memory_source` 스키마.
- `PROFILE`의 character null, `EVENT`·`EPISODE`의 character 필수 제약.
- `ACTIVE`·`SUPERSEDED`·`INVALIDATED` 상태와 유효 기간·관찰·기록·상태 전환 메타데이터.
- confidence, extractor/model 버전, 1,536차원 vector, USER 메시지 provenance.
- memory와 source를 한 트랜잭션으로 저장하고 FK 실패 시 함께 rollback.
- `userProfileId`, `characterId`, `queryEmbedding`, `limit`만 받는 exact 검색.

## 저장·검증 경계

`ConversationMemoryWriteService`가 사용자 잠금, snapshot 재검증, ADD·SUPERSEDE·IGNORE 저장, 계보 저장, READY 전환을 한 transaction에서 수행한다. 기억 또는 operation 저장이 실패하면 transaction rollback으로 새 기억·source·기존 상태 변경이 함께 되돌아간다.

Remote AI client는 공통 HTTP envelope와 JSON mapping만 담당하고 `AiMemoryCandidatesResult`·`AiMemoryResolutionResult`로 직접 역직렬화한다. 후보와 판정 응답의 untrusted-response 검증 권위는 `FreeTalkMemoryGenerationService` 한 곳에 둔다.

## V1/V2 경계와 알려진 위험

V1에서는 시작 시 오래된 `PREPARING` 작업을 자동 복구하지 않는다. process crash가 발생하면 `PREPARING`이 잔류할 수 있으며, stale recovery와 재처리 정책은 V2에서 다룬다. V1은 stale write에 대한 재시도도 하지 않는다.

세션별 사용 이력과 별도 queue/cache/ANN index는 V1 범위에서 제외한다.

## 검증 기록

- focused generation test는 STALE 단일 write 호출과 fail-closed를 검증한다.
- remote mapping, lifecycle/context, atomic write, repository scope의 대표 성공·실패 경계를 유지한다.
- `./gradlew check`가 `BUILD SUCCESSFUL`이고 `git diff --check`가 통과했다.
- 줄 수는 `git diff --numstat feat/LAN-347-2..HEAD`와 `git diff --numstat origin/develop..HEAD`의 삽입·삭제 합계로 완료 보고에 기록한다.

2026-08-26 최종 구현·검증 기록:

- recovery service·stale repository API·integration test 삭제.
- Remote client의 기억 전용 중복 검증 삭제.
- 전체 check와 줄 수 측정을 완료했다.
