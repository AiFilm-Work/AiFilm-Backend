package com.aifilm.domain.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HomeBannerTest {

    private static final LocalDateTime OCT_1 = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime OCT_31 = LocalDateTime.of(2026, 10, 31, 0, 0);

    private static HomeBanner.HomeBannerBuilder validBanner() {
        return HomeBanner.builder()
                .movieId(1L)
                .displayOrder(1);
    }

    @Test
    @DisplayName("미리보기 구간과 활성 여부를 주지 않으면 0초부터 10초, 활성 상태로 만들어진다")
    void usesDefaultPreviewAndActive() {
        HomeBanner banner = validBanner().build();

        assertThat(banner.getStartSeconds()).isZero();
        assertThat(banner.getPreviewDurationSeconds()).isEqualTo(10);
        assertThat(banner.isActive()).isTrue();
        assertThat(banner.getStartsAt()).isNull();
        assertThat(banner.getEndsAt()).isNull();
    }

    @Test
    @DisplayName("값을 주면 기본값 대신 그 값으로 만들어진다")
    void usesGivenValues() {
        HomeBanner banner = validBanner()
                .startSeconds(20)
                .previewDurationSeconds(15)
                .active(false)
                .startsAt(OCT_1)
                .endsAt(OCT_31)
                .build();

        assertThat(banner.getStartSeconds()).isEqualTo(20);
        assertThat(banner.getPreviewDurationSeconds()).isEqualTo(15);
        assertThat(banner.isActive()).isFalse();
        assertThat(banner.getStartsAt()).isEqualTo(OCT_1);
        assertThat(banner.getEndsAt()).isEqualTo(OCT_31);
    }

    @Test
    @DisplayName("영화 ID가 없으면 만들 수 없다")
    void rejectsMissingMovieId() {
        assertThatThrownBy(() -> validBanner().movieId(null).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("노출 순서가 1보다 작으면 만들 수 없다")
    void rejectsDisplayOrderBelowOne(int displayOrder) {
        assertThatThrownBy(() -> validBanner().displayOrder(displayOrder).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("미리보기 시작 위치가 음수면 만들 수 없다")
    void rejectsNegativeStartSeconds() {
        assertThatThrownBy(() -> validBanner().startSeconds(-1).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("미리보기 길이가 0초 이하면 만들 수 없다")
    void rejectsNonPositivePreviewDuration(int previewDurationSeconds) {
        assertThatThrownBy(() -> validBanner().previewDurationSeconds(previewDurationSeconds).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("노출 종료 시각이 시작 시각과 같거나 앞이면 만들 수 없다")
    void rejectsEndNotAfterStart() {
        assertThatThrownBy(() -> validBanner().startsAt(OCT_31).endsAt(OCT_31).build())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validBanner().startsAt(OCT_31).endsAt(OCT_1).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("노출 기간은 시작이나 종료 한쪽만 정할 수 있다")
    void allowsOpenEndedPeriod() {
        assertThat(validBanner().startsAt(OCT_1).build().getEndsAt()).isNull();
        assertThat(validBanner().endsAt(OCT_31).build().getStartsAt()).isNull();
    }

}
