-- 홈 섹션 정렬 인덱스(V4)를 노출 조건 컬럼만 youtube_availability_status → availability_status로 바꿔 새로 만든다.
-- 구조(= 조건 컬럼 → 정렬 컬럼 순서)와 근거는 V4와 같다. V4의 EXPLAIN ANALYZE 측정을 이 인덱스에도 적용할 수 있다.
--
-- V4 인덱스는 롤백된 이전 버전 앱이 쓰므로 이번 배포에서 지우지 않고, youtube 컬럼을 지우는 다음 배포에서 함께 지운다.
-- 그동안 movie 쓰기마다 인덱스 두 벌을 갱신하지만, 영화는 관리자 승인으로만 늘어나 쓰기가 드물다.

CREATE INDEX idx_movie_screening_availability_status_approved_at
    ON movie (screening_status, availability_status, approved_at);

CREATE INDEX idx_movie_screening_availability_status_rating
    ON movie (screening_status, availability_status, average_rating, review_count, approved_at);
