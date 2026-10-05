package com.aifilm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.aifilm.api.ApiModuleConfig;
import com.aifilm.worker.WorkerModuleConfig;

// AI FILM의 유일한 실행 진입점. api와 worker를 하나의 JVM에서 실행하고, 각 모듈은 스위치로 켜고 끈다.
// 기본 스캔 범위에서 com.aifilm.api, com.aifilm.worker를 뺀다. 포함하면 스위치와 무관하게 두 모듈의 빈이 모두 등록된다.
// 두 모듈의 패키지는 @Import한 진입 설정이 스위치가 켜졌을 때만 스캔한다.
@SpringBootApplication(scanBasePackages = {
	"com.aifilm.app",
	"com.aifilm.common",
	"com.aifilm.domain",
	"com.aifilm.infra"
})
@Import({ApiModuleConfig.class, WorkerModuleConfig.class})
public class AiFilmApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiFilmApplication.class, args);
	}

}
