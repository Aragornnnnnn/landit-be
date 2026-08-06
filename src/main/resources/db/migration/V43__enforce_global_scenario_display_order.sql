-- 시나리오 노출 순서를 카테고리별 기준에서 전체(글로벌) 기준 unique로 강제한다.
ALTER TABLE scenario DROP CONSTRAINT uk_scenario_category_order;
ALTER TABLE scenario ADD CONSTRAINT uk_scenario_display_order UNIQUE (display_order);
