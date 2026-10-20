-- 영화와 장르. ERD(2026-09-28 스냅샷)를 기준으로 하되 다음을 바꿨다.
-- - 러닝타임은 API와 같은 단위(초)로 running_time_seconds INT에 둔다. (ERD: `running time` TIME)
-- - youtube_availability_status 기본값 끝의 공백을 뺐다.
-- - candidate_id는 후보 테이블(youtube_movie_candidate)을 만들 때 함께 추가한다.
-- - genre.image_url은 장르 이미지가 정해질 때까지 NULL을 허용한다.

CREATE TABLE genre
(
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)   NOT NULL,
    image_url  VARCHAR(2048) NULL,
    created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_genre PRIMARY KEY (id),
    CONSTRAINT uk_genre_name UNIQUE (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE movie
(
    id                          BIGINT         NOT NULL AUTO_INCREMENT,
    youtube_video_id            VARCHAR(32)    NOT NULL,
    youtube_channel_id          VARCHAR(64)    NOT NULL,
    youtube_channel_title       VARCHAR(255)   NOT NULL,
    privacy_status              VARCHAR(20)    NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC, PRIVATE, UNLISTED',
    embeddable                  BOOLEAN        NOT NULL DEFAULT TRUE,
    youtube_deleted             BOOLEAN        NOT NULL DEFAULT FALSE,
    youtube_availability_status VARCHAR(30)    NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE, UNAVAILABLE',
    screening_status            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, UNAVAILABLE, HIDDEN',
    thumbnail_url               VARCHAR(2048)  NOT NULL,
    title                       VARCHAR(500)   NOT NULL,
    youtube_description         TEXT           NULL COMMENT 'YouTube 원본 영상의 description',
    short_description           VARCHAR(255)   NOT NULL COMMENT 'AI FILM 자체 한두 문장 소개',
    running_time_seconds        INT            NOT NULL,
    youtube_published_at        DATETIME(6)    NULL,
    average_rating              DECIMAL(3, 2)  NOT NULL DEFAULT 0.00,
    review_count                BIGINT         NOT NULL DEFAULT 0,
    total_view_count            BIGINT         NOT NULL DEFAULT 0,
    youtube_synced_at           DATETIME(6)    NULL,
    approved_at                 DATETIME(6)    NOT NULL,
    created_at                  DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_movie PRIMARY KEY (id),
    -- 같은 YouTube 영상이 두 번 등록되지 않게 DB에서 막는다
    CONSTRAINT uk_movie_youtube_video_id UNIQUE (youtube_video_id),
    CONSTRAINT ck_movie_running_time_seconds CHECK (running_time_seconds > 0),
    CONSTRAINT ck_movie_average_rating CHECK (average_rating BETWEEN 0 AND 5)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE movie_genre
(
    movie_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    CONSTRAINT pk_movie_genre PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT fk_movie_genre_movie FOREIGN KEY (movie_id) REFERENCES movie (id),
    CONSTRAINT fk_movie_genre_genre FOREIGN KEY (genre_id) REFERENCES genre (id),
    -- PK는 movie_id로 시작하므로, 장르로 영화를 찾는 조회를 위해 반대 순서 인덱스를 둔다
    INDEX idx_movie_genre_genre_id_movie_id (genre_id, movie_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
