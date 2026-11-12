package com.aifilm.domain.movie;

// 출처(YouTube, 업로드 원본)와 상관없이 영화를 지금 재생할 수 있는지. 출처들의 상태를 종합해 서비스가 갱신한다.
// 홈·목록·배너·상세는 출처 테이블을 조인하지 않고 이 값으로 노출 여부를 판단한다.
public enum AvailabilityStatus {
	AVAILABLE,
	UNAVAILABLE
}
