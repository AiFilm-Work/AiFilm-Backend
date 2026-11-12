-- 기존 영화(모두 YouTube 영화)의 youtube 컬럼 값을 youtube_movie_source로 복사하고,
-- 부모의 availability_status를 기존 youtube_availability_status 값으로 채운다.
--
-- movie의 youtube 컬럼은 이번 배포에서 지우지 않는다. 롤백된 이전 버전 앱이 계속 읽기 때문이다.
-- 전환 기간에 운영 데이터를 SQL로 넣거나 상태를 바꿀 때는 movie의 youtube 컬럼과 youtube_movie_source,
-- youtube_availability_status와 availability_status를 함께 쓴다. 삭제는 다음 배포의 별도 migration에서 한다.
--
-- 정상 배포에서는 V6 바로 뒤라 youtube_movie_source가 비어 있다. 이 마이그레이션이 실패해 일부를 손으로 옮긴 뒤
-- 다시 실행하더라도 중복 키로 실패하지 않도록, 이미 복사된 행은 건너뛴다.

INSERT INTO youtube_movie_source (movie_id, youtube_video_id, youtube_channel_id, youtube_channel_title,
                           youtube_description, privacy_status, embeddable, youtube_deleted,
                           youtube_availability_status, youtube_published_at, youtube_synced_at)
SELECT m.id,
       m.youtube_video_id,
       m.youtube_channel_id,
       m.youtube_channel_title,
       m.youtube_description,
       m.privacy_status,
       m.embeddable,
       m.youtube_deleted,
       m.youtube_availability_status,
       m.youtube_published_at,
       m.youtube_synced_at
FROM movie m
WHERE NOT EXISTS (SELECT 1 FROM youtube_movie_source y WHERE y.movie_id = m.id);

UPDATE movie
SET availability_status = youtube_availability_status
WHERE availability_status <> youtube_availability_status;
