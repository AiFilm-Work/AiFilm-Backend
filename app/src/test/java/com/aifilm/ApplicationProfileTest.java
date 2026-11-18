package com.aifilm;

import static org.assertj.core.api.Assertions.assertThat;

import com.aifilm.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

// 실행 프로파일 설정을 검증한다. 프로파일을 지정하지 않으면 local로 뜨고, 배포 서버용 prod 프로파일로도 앱이 뜨는지 확인한다.
// prod의 DB 접속 정보는 환경변수가 없어도 @ServiceConnection이 테스트 컨테이너 주소로 바꿔 넣는다.
@IntegrationTest
class ApplicationProfileTest {

    @Nested
    @DisplayName("프로파일을 지정하지 않았을 때")
    class NoActiveProfile {

        @Autowired
        private Environment environment;

        @Test
        @DisplayName("기본 프로파일인 local로 뜨고 application-local.yml이 적용된다")
        void defaultsToLocal() {
            assertThat(environment.getActiveProfiles()).isEmpty();
            assertThat(environment.matchesProfiles("local")).isTrue();
            assertThat(environment.getProperty("logging.level.org.hibernate.SQL")).isEqualTo("debug");
        }
    }

    @Nested
    @DisplayName("prod 프로파일을 지정했을 때")
    @ActiveProfiles("prod")
    class ProdProfile {

        @Autowired
        private Environment environment;

        @Test
        @DisplayName("local 설정 없이 prod 설정으로 앱이 뜬다")
        void startsWithProdProfile() {
            assertThat(environment.getActiveProfiles()).containsExactly("prod");
            assertThat(environment.matchesProfiles("local")).isFalse();
            assertThat(environment.getProperty("logging.level.org.hibernate.SQL")).isNull();
        }
    }

}
