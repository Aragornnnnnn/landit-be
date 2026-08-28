-- 발음 자산 임포트가 2단계(기준 데이터 → TTS)로 나뉘어, 기준 데이터만 먼저 저장할 수 있게
-- 음성 URL 컬럼의 NOT NULL을 해제한다 (LAN-342).
-- URL이 비어 있는 자산은 "TTS 미완성" 상태로, 발음 평가 API가 404로 거른다.

ALTER TABLE expression_pronunciation_asset
    ALTER COLUMN expression_audio_url DROP NOT NULL;

ALTER TABLE expression_pronunciation_asset
    ALTER COLUMN sentence_audio_url DROP NOT NULL;
