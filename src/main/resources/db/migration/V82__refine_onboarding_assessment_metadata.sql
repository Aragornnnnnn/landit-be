-- 시나리오 1의 질문 문구를 유지하며 명시적으로 요구하는 응답 요소만 등록한다.
UPDATE scenario_question
SET response_demand = CASE
    WHEN id IN (1, 124, 125) THEN 'MEDIUM'
    WHEN id = 2 THEN 'HIGH'
    ELSE 'LOW'
END
WHERE scenario_id = 1 AND id IN (1, 2, 3, 121, 122, 123, 124, 125, 126);

UPDATE scenario_question_language_variant
SET required_response_element = CASE scenario_question_id
    WHEN 1 THEN 'State your name.
Share a personal introduction detail.'
    WHEN 2 THEN 'State a hobby or leisure activity.
Explain how you became interested in it.'
    WHEN 3 THEN 'Recommend a first place to visit in Korea.'
    WHEN 121 THEN 'State your name.'
    WHEN 122 THEN 'State a hobby or leisure activity.'
    WHEN 123 THEN 'Name a favorite place in Korea.'
    WHEN 124 THEN 'Share a personal introduction detail.'
    WHEN 125 THEN 'State a hobby or leisure activity.
Describe what you like about it.'
    WHEN 126 THEN 'Recommend a first place to visit in Korea.'
END
WHERE target_locale = 'EN' AND base_locale = 'KR'
    AND scenario_question_id IN (
        SELECT id FROM scenario_question
        WHERE scenario_id = 1 AND id IN (1, 2, 3, 121, 122, 123, 124, 125, 126)
    );
