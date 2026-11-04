-- 영화의 YouTube 출처 정보. movie와 PK를 공유한다(movie_id = movie.id). 한 영화에 최대 하나만 붙는다.
-- 컬럼 이름·타입·기본값은 movie에 있던 youtube 컬럼을 그대로 옮겨 값 손실 없이 복사되게 했다.
--
-- 규칙과 다르게 한 점
-- - PK에 AUTO_INCREMENT를 붙이지 않는다. movie가 ID를 만들고 youtube_movie는 같은 값을 쓴다.
-- - created_at, updated_at을 두지 않는다. 생성 시각은 movie와 같고, YouTube 상태를 마지막으로 확인한 시각은 youtube_synced_at에 남긴다.
-- - candidate_id는 V1과 같은 이유로 후보 테이블(youtube_movie_candidate)을 만들 때 이 테이블에 추가한다.
--
-- ERD에 없던 것을 추가했다
-- - youtube_video_id UNIQUE. 같은 영상이 두 번 등록되지 않게 DB에서 막는다. (movie의 같은 UNIQUE를 옮긴 것)
-- - privacy_status, youtube_availability_status CHECK. 운영 데이터를 SQL로 넣는 동안 잘못된 값을 막는다.

CREATE TABLE youtube_movie
(
    movie_id                    BIGINT       NOT NULL,
    youtube_video_id            VARCHAR(32)  NOT NULL,
    youtube_channel_id          VARCHAR(64)  NOT NULL,
    youtube_channel_title       VARCHAR(255) NOT NULL,
    youtube_description         TEXT         NULL COMMENT 'YouTube 원본 영상의 description',
    privacy_status              VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC, PRIVATE, UNLISTED',
    embeddable                  BOOLEAN      NOT NULL DEFAULT TRUE,
    youtube_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    youtube_availability_status VARCHAR(30)  NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE, UNAVAILABLE. 원본 동기화가 확인한 YouTube 쪽 상태',
    youtube_published_at        DATETIME(6)  NULL,
    youtube_synced_at           DATETIME(6)  NULL,
    CONSTRAINT pk_youtube_movie PRIMARY KEY (movie_id),
    CONSTRAINT uk_youtube_movie_youtube_video_id UNIQUE (youtube_video_id),
    CONSTRAINT fk_youtube_movie_movie FOREIGN KEY (movie_id) REFERENCES movie (id),
    CONSTRAINT ck_youtube_movie_privacy_status CHECK (privacy_status IN ('PUBLIC', 'PRIVATE', 'UNLISTED')),
    CONSTRAINT ck_youtube_movie_youtube_availability_status
        CHECK (youtube_availability_status IN ('AVAILABLE', 'UNAVAILABLE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
