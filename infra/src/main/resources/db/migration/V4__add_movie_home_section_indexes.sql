-- 홈 섹션(최신 등록작, 평점 TOP) 조회용 인덱스.
-- 노출 조건(=) 컬럼 뒤에 정렬 컬럼을 둬서, 조건에 맞는 영화를 정렬된 순서대로 읽다가 LIMIT만큼 읽으면 멈추게 한다.
-- InnoDB는 보조 인덱스 끝에 PK(id)를 붙이므로, ORDER BY 마지막의 id DESC도 따로 넣지 않고 인덱스로 처리된다.
-- 평점 인덱스는 리뷰가 달릴 때마다 average_rating, review_count가 바뀌어 갱신 비용이 있지만, 홈 조회가 리뷰 작성보다 훨씬 잦아 둔다.

CREATE INDEX idx_movie_screening_availability_approved_at
    ON movie (screening_status, youtube_availability_status, approved_at);

CREATE INDEX idx_movie_screening_availability_rating
    ON movie (screening_status, youtube_availability_status, average_rating, review_count, approved_at);
