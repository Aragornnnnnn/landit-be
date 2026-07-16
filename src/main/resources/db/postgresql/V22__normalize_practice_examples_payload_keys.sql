-- 추가 예문 payload(practice_examples_payload)의 JSON 키를 시딩 파이프라인이 넣은 스네이크 케이스에서
-- 파서(ExpressionQueryService.REQUIRED_PRACTICE_SENTENCE_KEYS)가 읽는 카멜 케이스로 정규화한다.
-- 키 불일치로 모든 예문이 불량 처리되어 추가 예문 API가 404를 반환하던 문제의 데이터 교정.
-- jsonb 함수를 쓰므로 PostgreSQL 전용 폴더에 둔다. (H2 테스트 DB는 행이 없어 영향 없음)
-- develop/prod DB는 수동 UPDATE로 이미 교정했으므로(2026-07-15) 이 migration은 해당 DB에서 0행으로 지나간다.

UPDATE writing_expression
SET practice_examples_payload = (
        SELECT jsonb_agg(
                   jsonb_build_object(
                       'sentenceText',                elem->'sentence_text',
                       'highlightingPart',            elem->'sentence_highlight_text',
                       'sentenceTranslation',         elem->'sentence_translation',
                       'practiceQuestion',            elem->'question_text',
                       'practiceQuestionTranslation', elem->'question_translation',
                       'imageUrl',                    elem->'image_url'
                   ) ORDER BY ord
               )
        FROM jsonb_array_elements(practice_examples_payload) WITH ORDINALITY AS t(elem, ord)
    ),
    updated_at = now()
WHERE EXISTS (
    SELECT 1 FROM jsonb_array_elements(practice_examples_payload) AS e
    WHERE e ? 'sentence_text'
);
