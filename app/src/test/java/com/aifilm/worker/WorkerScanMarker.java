package com.aifilm.worker;

import org.springframework.stereotype.Component;

// 테스트 전용 마커. com.aifilm.worker 패키지 스캔으로만 등록되므로, 이 빈의 유무로 worker 모듈 스캔이 스위치를 따르는지 확인한다.
// 메인 클래스의 기본 스캔 범위에 com.aifilm.worker가 잘못 포함되면 Worker 스위치를 꺼도 등록되어 실행 모드 테스트가 실패한다.
@Component
public class WorkerScanMarker {
}
