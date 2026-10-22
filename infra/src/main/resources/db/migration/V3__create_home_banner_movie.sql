-- 홈 상단 배너. ERD(2026-09-28 스냅샷)를 기준으로 하되 다음을 추가했다.
-- - PK에 AUTO_INCREMENT를 붙였다.
-- - 한 영화는 배너 하나로만 운영한다(API 명세의 권장 정책). movie_id에 UNIQUE를 걸었다.
-- - movie_id에 movie FK를 걸었다.
-- - display_order >= 1, start_seconds >= 0, preview_duration_seconds > 0, starts_at < ends_at CHECK를 걸었다.
--   관리자 API가 생기기 전에는 운영 배너를 SQL로 직접 넣으므로, 엔티티 검증을 거치지 않는 입력도 DB에서 막는다.
-- 배너 이미지는 movie.thumbnail_url을 쓰므로 이미지 컬럼은 두지 않는다.

CREATE TABLE home_banner_movie
(
    id                       BIGINT      NOT NULL AUTO_INCREMENT,
    movie_id                 BIGINT      NOT NULL,
    display_order            INT         NOT NULL,
    start_seconds            INT         NOT NULL DEFAULT 0 COMMENT '미리보기 시작 위치(초)',
    preview_duration_seconds INT         NOT NULL DEFAULT 10 COMMENT '미리보기 길이(초)',
    active                   BOOLEAN     NOT NULL DEFAULT TRUE,
    starts_at                DATETIME(6) NULL COMMENT 'NULL이면 바로 노출',
    ends_at                  DATETIME(6) NULL COMMENT 'NULL이면 종료 없이 노출',
    created_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_home_banner_movie PRIMARY KEY (id),
    CONSTRAINT uk_home_banner_movie_movie_id UNIQUE (movie_id),
    CONSTRAINT fk_home_banner_movie_movie FOREIGN KEY (movie_id) REFERENCES movie (id),
    CONSTRAINT ck_home_banner_movie_display_order CHECK (display_order >= 1),
    CONSTRAINT ck_home_banner_movie_start_seconds CHECK (start_seconds >= 0),
    CONSTRAINT ck_home_banner_movie_preview_duration_seconds CHECK (preview_duration_seconds > 0),
    CONSTRAINT ck_home_banner_movie_period CHECK (starts_at IS NULL OR ends_at IS NULL OR starts_at < ends_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
