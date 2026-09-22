-- 두 번 이상 틀리고 미완료로 남은 복습을 실제 두 번째 오답 시각으로 종료한다.

UPDATE expression_review_question q
SET completed_at = (
    SELECT attempts.created_at
    FROM (
        SELECT review_id, question_id, created_at,
               ROW_NUMBER() OVER (
                   PARTITION BY review_id, question_id ORDER BY created_at, submission_id
               ) AS attempt_number
        FROM expression_review_submission
        WHERE correct = FALSE
    ) attempts
    WHERE attempts.review_id = q.review_id
      AND attempts.question_id = q.id
      AND attempts.attempt_number = 2
)
WHERE q.completed_at IS NULL AND q.wrong_count >= 2;

UPDATE expression_review r
SET completed_at = (
    SELECT MAX(q.completed_at) FROM expression_review_question q WHERE q.review_id = r.id
)
WHERE r.completed_at IS NULL AND r.started_at IS NOT NULL
  AND EXISTS (SELECT 1 FROM expression_review_question q WHERE q.review_id = r.id)
  AND NOT EXISTS (
      SELECT 1 FROM expression_review_question q
      WHERE q.review_id = r.id AND q.completed_at IS NULL
  );
