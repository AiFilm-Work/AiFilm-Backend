-- 이번 배포부터 앱은 youtube 값을 youtube_movie에만 쓴다. movie에 남은 youtube 컬럼 중
-- DEFAULT 없이 NOT NULL인 세 컬럼은 앱의 movie INSERT를 막으므로 NULL을 허용한다.
-- (DEFAULT가 있는 나머지 youtube 컬럼은 값 없이 INSERT해도 실패하지 않아 그대로 둔다)
--
-- 롤백 호환: 이전 버전 앱은 이 세 컬럼에 값을 넣어 INSERT하므로 영향이 없다.
-- ddl-auto=validate는 NULL 허용 여부를 비교하지 않는다.
-- uk_movie_youtube_video_id는 NULL을 여러 개 허용하므로 그대로 둔다.
-- 세 컬럼은 다음 배포에서 다른 youtube 컬럼과 함께 지운다.

ALTER TABLE movie
    MODIFY COLUMN youtube_video_id      VARCHAR(32)  NULL,
    MODIFY COLUMN youtube_channel_id    VARCHAR(64)  NULL,
    MODIFY COLUMN youtube_channel_title VARCHAR(255) NULL;
