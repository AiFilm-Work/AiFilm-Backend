-- 영화를 출처와 무관한 공통 정보(movie)와 출처별 정보(youtube_movie_source, 이후 uploaded_movie_source)로 나누기 위한 첫 단계.
-- 공통 정보에 availability_status를 추가한다.
-- - availability_status: 출처와 무관하게 지금 재생할 수 있는지. 홈·목록이 출처 테이블을 조인하지 않고 노출 여부를 판단하게 한다.
--   한 영화에 출처가 여러 개 붙을 수 있으므로(YouTube + 업로드 원본), 출처들의 상태를 종합한 결과를 서비스가 갱신한다.
--
-- 출처 구분 컬럼(source_type)은 두지 않는다. 한 영화가 출처를 여러 개 가질 수 있어 값 하나로 표현할 수 없고,
-- 어떤 출처가 있는지는 출처 테이블의 행 유무로 판단한다.
--
-- 롤백 호환: DEFAULT가 있어 이 컬럼을 모르는 이전 버전 앱의 INSERT도 실패하지 않는다.
-- 운영 데이터를 SQL로 직접 넣는 동안 오타(예: 값 끝 공백)를 막기 위해 허용 값을 CHECK로 건다.
-- 기존 행의 값은 V7에서 youtube_availability_status 값으로 채운다.

ALTER TABLE movie
    ADD COLUMN availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE, UNAVAILABLE' AFTER screening_status,
    ADD CONSTRAINT ck_movie_availability_status CHECK (availability_status IN ('AVAILABLE', 'UNAVAILABLE'));
