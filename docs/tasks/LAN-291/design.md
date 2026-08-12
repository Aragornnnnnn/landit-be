# LAN-291 프리톡 표현 임베딩 기반 설계

## 목표

프리톡 완료 후 LLM이 새 표현을 생성하지 않고, 미리 적재한 공용 `FREE_TALK` 표현의 ID만 추천하도록 변경한다. 818개 표현의 임베딩은 배포 전에 생성해 데이터 migration에 고정하며, 벡터 유사도 검색은 후속 이슈에서 연결한다.

## 데이터 모델

- PostgreSQL에 `vector` 확장을 활성화한다.
- Supabase 기본 확장 스키마를 명시해 `writing_expression.embedding extensions.vector(1536)` nullable 컬럼을 추가한다.
- 임베딩 모델은 OpenRouter의 `openai/text-embedding-3-small`로 고정한다.
- 표현 임베딩 입력은 아래 형식으로 고정한다.

```text
expression: {trim하고 연속 공백을 하나로 줄인 target_expression_text}
usage: {trim하고 연속 공백을 하나로 줄인 usage_summary}
```

- `owner_user_profile_id`가 채워진 기존 사용자 전용 표현은 자식 연결과 완료 이력을 먼저 삭제한 뒤 제거한다.
- `owner_user_profile_id`, `fk_writing_expression_owner_user_profile_id`, 기존 `chk_writing_expression_source`를 제거한다.
- owner 상호배타 조건 대신 `SCENARIO`는 `scenario_id`가 있고 `FREE_TALK`은 `scenario_id`가 없는 조건만 `chk_writing_expression_scenario_source`로 유지한다.

## 현재 추천 흐름

1. BE가 활성 공용 `FREE_TALK` 표현 전체를 locale 기준으로 조회한다.
2. 기존 표현 추천 LLM은 전달된 후보 중 한 개에서 세 개의 기존 표현 ID만 반환한다.
3. 응답은 기존 표현 ID만 포함하며 `NEW` 출처와 학습 콘텐츠 생성 호출은 제거한다.

대화 임베딩, cosine 거리 조회, 후보 수 제한, HNSW·IVFFlat 인덱스는 후속 벡터 검색 이슈 범위다.

## 사전 생성 임베딩

818개 표현의 `target_expression_text`와 `usage_summary`를 고정 규칙으로 결합해 OpenRouter에서 한 번 임베딩한다. 생성한 1,536차원 벡터는 V52 데이터 migration의 각 INSERT 행에 포함한다. BE와 AI 런타임에는 임베딩 생성 API나 backfill 경로를 두지 않는다.

## 호환성과 오류 처리

- PostgreSQL 전용 migration은 V51을 사용한다. 2026-08-12 기준 열린 PR 최고 예약 번호는 PR #96의 V50이다.
- H2 테스트 migration은 vector 확장 없이 호환 컬럼만 추가한다.
- V52는 이미지 URL과 일치하는 표현 ID `164~981`, 818개 행과 818개 `extensions.vector` 값을 함께 적재하고 identity sequence를 최신 ID로 동기화한다.
- 후보가 없거나 추천 결과가 기존 표현을 참조하지 않으면 기존 AI 응답 오류 경로로 실패 처리한다.

## 검증

- migration 파일과 H2 최종 스키마 검증.
- owner 컬럼·FK·기존 CHECK 제거 및 새 source/scenario CHECK 검증.
- V52의 ID `164~981`, 818개 행, 818개 벡터 캐스팅, 1,536차원 생성 결과를 검증한다.
- 프리톡 추천이 사용자 전용 표현을 만들지 않고 기존 ID만 연결하는 통합 테스트.
