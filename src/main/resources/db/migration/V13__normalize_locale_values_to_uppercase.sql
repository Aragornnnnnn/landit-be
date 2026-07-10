-- locale 값 표기를 콘텐츠 시딩 규칙('EN'/'KR' 대문자)으로 통일한다.
-- 앱이 소문자 기본값('en'/'ko')으로 저장한 행들이 시딩 데이터와 대소문자가 달라
-- user_profile locale 기준 조인/필터(시나리오 목록, 표현 목록 등)가 0건이 되는 문제를 막는다.

UPDATE user_profile SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE user_profile SET base_locale = 'KR' WHERE base_locale = 'ko';

UPDATE ai_tutor SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE ai_tutor_language_variant SET base_locale = 'KR' WHERE base_locale = 'ko';

UPDATE category_language_variant SET base_locale = 'KR' WHERE base_locale = 'ko';
UPDATE character_stage_language_variant SET base_locale = 'KR' WHERE base_locale = 'ko';
UPDATE quest_template_language_variant SET base_locale = 'KR' WHERE base_locale = 'ko';

UPDATE scenario_language_variant SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE scenario_language_variant SET base_locale = 'KR' WHERE base_locale = 'ko';
UPDATE scenario_question_language_variant SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE scenario_question_language_variant SET base_locale = 'KR' WHERE base_locale = 'ko';

UPDATE writing_expression SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE writing_expression SET base_locale = 'KR' WHERE base_locale = 'ko';

UPDATE learning_session SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE learning_session SET base_locale = 'KR' WHERE base_locale = 'ko';
UPDATE session_history SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE session_history SET base_locale = 'KR' WHERE base_locale = 'ko';
UPDATE session_history_message_feedback SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE session_history_message_feedback SET base_locale = 'KR' WHERE base_locale = 'ko';

UPDATE user_scenario_progress SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE user_learning_expression SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE user_learning_expression SET base_locale = 'KR' WHERE base_locale = 'ko';

UPDATE review_item SET target_locale = 'EN' WHERE target_locale = 'en';
UPDATE review_item SET base_locale = 'KR' WHERE base_locale = 'ko';
