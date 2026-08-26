# LAN-347-2 장기기억 V1 구현 기록

## 범위

V1은 프리톡 장기기억을 원본 메시지 계보와 함께 저장하고, 세션 시작 시 한 번 사용자·캐릭터 범위의 exact cosine 검색을 제공한다.

V1에 포함한다.

- PostgreSQL/H2 V59의 `conversation_memory`, `conversation_memory_source` 스키마.
- `PROFILE`의 character null, `EVENT`·`EPISODE`의 character 필수 제약.
- `ACTIVE`·`SUPERSEDED`·`INVALIDATED` 상태와 유효 기간·관찰·기록·상태 전환 메타데이터.
- confidence, extractor/model 버전, 1,536차원 vector, USER 메시지 provenance.
- memory와 source를 한 트랜잭션으로 저장하고 FK 실패 시 함께 rollback.
- `userProfileId`, `characterId`, `queryEmbedding`, `limit`만 받는 exact 검색.

V1에서 제외한다.

- 민감도 분류와 관련 필드·제약.
- 검색 결과 사용 이력, 세션별 제외 목록, 런타임 사용 판정. 이 범위는 LAN-347-5에서 이관해 결정한다.
- ANN index, distance threshold, 별도 DB·queue·cache, 추출 pipeline.

## 저장·검색 계약

`NewConversationMemory`는 사용자 ID, 선택적 character ID, memory type, 본문·locale, confidence, `valid_from`과 nullable `valid_to`의 순서, observed/recorded 시각, extractor/model 버전과 1,536차원 finite embedding을 검증한다. `PROFILE`은 character가 없고 나머지 두 type은 character가 있어야 한다. 저장소는 새 row를 `ACTIVE`로 저장하고 supersede·invalidation 상태 전환 필드는 비워 둔다.

`ConversationMemoryRepository.save`는 memory와 `conversation_memory_source`를 단일 transaction으로 저장한다. source ID는 비어 있거나 중복·비양수이면 SQL 전에 거절하고, source FK 오류는 memory INSERT도 rollback한다.

`searchActive(userProfileId, characterId, queryEmbedding, limit)`은 입력을 검증한 뒤 `user_profile_id`, `ACTIVE`, `(character_id IS NULL OR character_id = ?)`를 hard scope로 적용한다. PostgreSQL은 pgvector `<=>` exact cosine과 거리·ID 순 정렬을 사용하고, H2는 동일 scope 후보를 Java exact cosine으로 계산한다.

## LAN-347-5 이관 경계

검색 결과를 실제 대화에서 사용했는지 기록하는 저장 구조, 세션별 반복 검색 시 제외할 ID, 사용 판정과 policy version은 LAN-347-5의 런타임 요구사항으로 이관한다. V1에서는 새 comparable search API를 만들지 않는다.

## 검증

- 대표 domain/schema/migration 테스트는 type·scope·state·source·vector 계약을 검증한다.
- repository integration 테스트는 save/rollback과 exact scope/order를 검증한다.
- 최소 focused test 후 `./gradlew check`, `git diff --check`를 실행한다.
- 줄 수는 `git diff --numstat origin/develop..HEAD`로 production Java, migration, tests/docs를 구분해 기록한다.

PostgreSQL 실 DB에 migration을 적용하거나 운영 pgvector query plan·성능을 측정하는 작업은 포함하지 않는다.

2026-08-26 검증 결과:

- V1 focused test와 `./gradlew check`가 `BUILD SUCCESSFUL`이다.
- `git diff --check`가 통과했다.
- origin/develop 대비 현재 줄 수는 production 501줄(Java 499줄·설정 2줄), migration 170줄, tests 793줄, 이 문서 47줄이다.
