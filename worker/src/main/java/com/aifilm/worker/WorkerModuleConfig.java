package com.aifilm.worker;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// worker 모듈의 진입 설정. aifilm.worker.enabled=true일 때만 등록되고, 이때만 com.aifilm.worker 패키지를 스캔한다.
// @EnableScheduling도 여기에 두어, Worker를 끄면 @Scheduled 작업까지 함께 꺼지게 한다.
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("aifilm.worker.enabled")
@ComponentScan
@EnableScheduling
public class WorkerModuleConfig {
}
