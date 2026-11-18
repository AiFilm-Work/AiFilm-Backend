package com.aifilm.domain.genre;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GenreTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("이름이 비어 있으면 장르를 만들 수 없다")
    void rejectsBlankName(String blank) {
        assertThatThrownBy(() -> new Genre(blank, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
