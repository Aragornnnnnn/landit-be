-- 표현(writing_expression) 삭제 시 발음 자산이 함께 삭제되도록 FK를 ON DELETE CASCADE로 재생성한다.
-- 자산은 표현에서 파생된 데이터라 표현 없이 존재할 이유가 없고,
-- 표현을 통째로 삭제·재시드하는 기존 테스트들이 FK에 막히지 않게 한다 (LAN-342).

ALTER TABLE expression_pronunciation_asset
    DROP CONSTRAINT fk_expression_pronunciation_asset_writing_expression_id;

ALTER TABLE expression_pronunciation_asset
    ADD CONSTRAINT fk_expression_pronunciation_asset_writing_expression_id
        FOREIGN KEY (writing_expression_id) REFERENCES writing_expression (id) ON DELETE CASCADE;
