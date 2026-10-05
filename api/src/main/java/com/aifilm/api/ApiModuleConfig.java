package com.aifilm.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// api 모듈의 진입 설정. aifilm.api.enabled=true일 때만 등록되고, 이때만 com.aifilm.api 패키지를 스캔한다.
// 스위치가 꺼지면 이 클래스가 통째로 건너뛰어져 api 모듈의 빈이 하나도 등록되지 않는다.
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("aifilm.api.enabled")
@ComponentScan
public class ApiModuleConfig {
}
