-- 로컬 개발용 샘플 영화와 홈 배너. local 프로파일에서만 실행된다. (application-local.yml의 spring.flyway.locations)
-- 운영·테스트 DB에는 들어가지 않는다. YouTube 영상 ID는 실제 영상이 아닌 자리표시자라 플레이어에서는 재생되지 않는다.
-- 반복 실행(R__) migration이라 이 파일을 고치면 다음 실행 때 다시 적용된다. 그래서 몇 번을 실행해도 결과가 같게 쓴다.
-- 샘플 영화를 이 파일에서 빼도 DB에서는 지워지지 않는다. 그때는 docker compose down -v로 로컬 DB를 초기화한다.

INSERT INTO movie (youtube_video_id, youtube_channel_id, youtube_channel_title, thumbnail_url,
                   title, short_description, running_time_seconds, average_rating, review_count, approved_at)
VALUES
       ('local-sample-001', 'local-sample-channel', 'AI FILM 샘플 채널', 'https://picsum.photos/seed/aifilm-001/640/360',
        '네온 메모리', '기억은 사라져도, 사랑은 남는다.', 766, 4.70, 2354, '2026-10-01 10:00:00'),
       ('local-sample-002', 'local-sample-channel', 'AI FILM 샘플 채널', 'https://picsum.photos/seed/aifilm-002/640/360',
        '첫 겨울꽃', '가장 추운 날, 처음 피어난 꽃 이야기.', 512, 4.80, 7321, '2026-10-03 10:00:00'),
       ('local-sample-003', 'local-sample-channel', 'AI FILM 샘플 채널', 'https://picsum.photos/seed/aifilm-003/640/360',
        '오로라 정거장', '궤도 정거장에 홀로 남은 정비 로봇의 마지막 근무.', 1340, 4.60, 3210, '2026-10-05 10:00:00'),
       ('local-sample-004', 'local-sample-channel', 'AI FILM 샘플 채널', 'https://picsum.photos/seed/aifilm-004/640/360',
        '401호의 소리', '매일 밤 같은 시각, 빈 방에서 들리는 발소리.', 421, 4.10, 980, '2026-10-07 10:00:00'),
       ('local-sample-005', 'local-sample-channel', 'AI FILM 샘플 채널', 'https://picsum.photos/seed/aifilm-005/640/360',
        '구름 우체부', '구름 위 마을에 편지를 나르는 소녀의 하루.', 298, 4.40, 1543, '2026-10-09 10:00:00'),
       ('local-sample-006', 'local-sample-channel', 'AI FILM 샘플 채널', 'https://picsum.photos/seed/aifilm-006/640/360',
        '출근길 루프', '같은 출근길을 백 번째 반복하는 회사원.', 655, 3.90, 612, '2026-10-11 10:00:00'),
       ('local-sample-007', 'local-sample-channel', 'AI FILM 샘플 채널', 'https://picsum.photos/seed/aifilm-007/640/360',
        '마지막 상영관', '문을 닫는 극장의 마지막 밤.', 1820, 4.50, 2088, '2026-10-13 10:00:00'),
       ('local-sample-008', 'local-sample-channel', 'AI FILM 샘플 채널', 'https://picsum.photos/seed/aifilm-008/640/360',
        '픽셀 고양이', '화면 밖으로 나가고 싶은 고양이의 모험.', 184, 4.20, 1201, '2026-10-15 10:00:00')
AS sample
ON DUPLICATE KEY UPDATE youtube_channel_title = sample.youtube_channel_title,
                        thumbnail_url         = sample.thumbnail_url,
                        title                 = sample.title,
                        short_description     = sample.short_description,
                        running_time_seconds  = sample.running_time_seconds,
                        average_rating        = sample.average_rating,
                        review_count          = sample.review_count,
                        approved_at           = sample.approved_at;

-- YouTube 값은 youtube_movie_source에도 넣는다. 전환 기간에는 운영 데이터처럼 movie의 youtube 컬럼과 함께 쓴다.
-- (movie의 youtube 컬럼을 지우는 배포에서 이 파일도 youtube_movie_source만 쓰도록 바꾼다)
INSERT INTO youtube_movie_source (movie_id, youtube_video_id, youtube_channel_id, youtube_channel_title)
SELECT *
FROM (SELECT id, youtube_video_id, youtube_channel_id, youtube_channel_title
      FROM movie
      WHERE youtube_video_id LIKE 'local-sample-%') AS sample
ON DUPLICATE KEY UPDATE youtube_channel_id    = sample.youtube_channel_id,
                        youtube_channel_title = sample.youtube_channel_title;

-- 장르 연결은 파일에서 빼거나 바꾼 것이 남지 않도록 샘플 영화의 연결만 지운 뒤 지금 정의대로 다시 넣는다
DELETE
FROM movie_genre
WHERE movie_id IN (SELECT id FROM movie WHERE youtube_video_id LIKE 'local-sample-%');

INSERT INTO movie_genre (movie_id, genre_id)
SELECT id, 1 FROM movie WHERE youtube_video_id = 'local-sample-001'
UNION ALL SELECT id, 5 FROM movie WHERE youtube_video_id = 'local-sample-001'
UNION ALL SELECT id, 3 FROM movie WHERE youtube_video_id = 'local-sample-002'
UNION ALL SELECT id, 5 FROM movie WHERE youtube_video_id = 'local-sample-002'
UNION ALL SELECT id, 1 FROM movie WHERE youtube_video_id = 'local-sample-003'
UNION ALL SELECT id, 2 FROM movie WHERE youtube_video_id = 'local-sample-004'
UNION ALL SELECT id, 4 FROM movie WHERE youtube_video_id = 'local-sample-005'
UNION ALL SELECT id, 7 FROM movie WHERE youtube_video_id = 'local-sample-005'
UNION ALL SELECT id, 6 FROM movie WHERE youtube_video_id = 'local-sample-006'
UNION ALL SELECT id, 1 FROM movie WHERE youtube_video_id = 'local-sample-006'
UNION ALL SELECT id, 3 FROM movie WHERE youtube_video_id = 'local-sample-007'
UNION ALL SELECT id, 7 FROM movie WHERE youtube_video_id = 'local-sample-008'
UNION ALL SELECT id, 6 FROM movie WHERE youtube_video_id = 'local-sample-008';

-- 홈 배너도 장르 연결과 같은 이유로 샘플 영화의 배너만 지운 뒤 다시 넣는다.
-- 반복 실행 migration은 버전 migration(V3) 다음에 적용되므로 home_banner_movie가 이미 있다.
-- 미리보기 구간(시작 + 길이)은 각 영화의 러닝타임 안에 둔다.
DELETE
FROM home_banner_movie
WHERE movie_id IN (SELECT id FROM movie WHERE youtube_video_id LIKE 'local-sample-%');

INSERT INTO home_banner_movie (movie_id, display_order, start_seconds, preview_duration_seconds)
SELECT id, 1, 20, 10 FROM movie WHERE youtube_video_id = 'local-sample-002'
UNION ALL SELECT id, 2, 0, 10 FROM movie WHERE youtube_video_id = 'local-sample-001'
UNION ALL SELECT id, 3, 30, 10 FROM movie WHERE youtube_video_id = 'local-sample-007';
