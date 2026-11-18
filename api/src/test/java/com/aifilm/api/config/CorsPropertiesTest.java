package com.aifilm.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    @DisplayName("설정이 없으면 허용 출처가 비어 있다")
    void nullBecomesEmpty() {
        assertThat(new CorsProperties(null).allowedOrigins()).isEmpty();
    }

    @Test
    @DisplayName("빈 값은 빼고 앞뒤 공백은 지운다")
    void removesBlankAndTrims() {
        CorsProperties properties = new CorsProperties(List.of("", " https://aifilm.life ", "  "));

        assertThat(properties.allowedOrigins()).containsExactly("https://aifilm.life");
    }

}
