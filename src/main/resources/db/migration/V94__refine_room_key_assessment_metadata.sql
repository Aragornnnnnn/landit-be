-- 방 키 시나리오의 단일 선택 질문만 낮은 응답 요구도와 의미 기반 평가 요소로 보정한다.
UPDATE scenario_question_language_variant
SET required_response_element = CASE scenario_question_id
    WHEN 61 THEN 'Choose one available temporary access option.'
    WHEN 62 THEN 'State a preferred pickup time.'
    WHEN 63 THEN 'Choose text or email for the notification.'
END
WHERE target_locale = 'EN' AND base_locale = 'KR'
  AND required_response_element = question_text
  AND scenario_question_id IN (
      SELECT id FROM scenario_question
      WHERE scenario_id = 21 AND question_level_group = 'LEVEL_4_TO_5'
        AND id IN (61, 62, 63)
  )
  AND question_text = CASE scenario_question_id
      WHEN 61 THEN 'No worries, it happens all the time. I can give you a temporary key, or someone can walk up and let you in. Which works better for you?'
      WHEN 62 THEN 'For the replacement, we can have it ready tomorrow morning, or the day after in the afternoon. When would you like to pick it up?'
      WHEN 63 THEN 'And we''ll let you know when it''s ready — would you rather get a text or an email?'
  END;

UPDATE scenario_question
SET response_demand = 'LOW'
WHERE scenario_id = 21 AND question_level_group = 'LEVEL_4_TO_5'
  AND id IN (
      SELECT scenario_question_id FROM scenario_question_language_variant
      WHERE target_locale = 'EN' AND base_locale = 'KR'
        AND required_response_element = CASE scenario_question_id
            WHEN 61 THEN 'Choose one available temporary access option.'
            WHEN 62 THEN 'State a preferred pickup time.'
            WHEN 63 THEN 'Choose text or email for the notification.'
        END
        AND question_text = CASE scenario_question_id
            WHEN 61 THEN 'No worries, it happens all the time. I can give you a temporary key, or someone can walk up and let you in. Which works better for you?'
            WHEN 62 THEN 'For the replacement, we can have it ready tomorrow morning, or the day after in the afternoon. When would you like to pick it up?'
            WHEN 63 THEN 'And we''ll let you know when it''s ready — would you rather get a text or an email?'
        END
  );
