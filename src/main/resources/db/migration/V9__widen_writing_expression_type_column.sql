-- Writing 표현 분류 값 개편으로 길어진 enum 이름을 담기 위해 컬럼 길이를 넓힌다.
ALTER TABLE writing_expression ALTER COLUMN expression_type TYPE VARCHAR(30);
