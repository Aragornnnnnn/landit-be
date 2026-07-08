-- 별점 컬럼을 FE 별점 스케일 소수 저장 방식으로 변경한다.
ALTER TABLE user_scenario_progress
    DROP CONSTRAINT chk_user_scenario_progress_score;

ALTER TABLE session_history_summary_feedback
    DROP CONSTRAINT chk_session_history_summary_feedback_score;

ALTER TABLE user_scenario_progress
    ALTER COLUMN best_star_rating TYPE NUMERIC(2, 1);

ALTER TABLE session_history_summary_feedback
    ALTER COLUMN star_rating TYPE NUMERIC(2, 1);

ALTER TABLE user_scenario_progress
    ADD CONSTRAINT chk_user_scenario_progress_score CHECK (
        (best_star_rating IS NULL OR best_star_rating IN (1.0, 1.5, 2.0, 2.5, 3.0))
        AND (best_native_score IS NULL OR best_native_score BETWEEN 0 AND 100)
        AND completed_count >= 0
    );

ALTER TABLE session_history_summary_feedback
    ADD CONSTRAINT chk_session_history_summary_feedback_score CHECK (
        (native_score IS NULL OR native_score BETWEEN 0 AND 100)
        AND (star_rating IS NULL OR star_rating IN (1.0, 1.5, 2.0, 2.5, 3.0))
        AND (total_message_count IS NULL OR total_message_count >= 0)
        AND (native_like_message_count IS NULL OR native_like_message_count >= 0)
        AND (
            total_message_count IS NULL
            OR native_like_message_count IS NULL
            OR native_like_message_count <= total_message_count
        )
    );
