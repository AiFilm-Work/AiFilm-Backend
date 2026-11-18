package com.aifilm.domain.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class YoutubeMovieSourceTest {

    private static YoutubeMovieSource.YoutubeMovieSourceBuilder validSourceBuilder() {
        return YoutubeMovieSource.builder()
                .movieId(1L)
                .youtubeVideoId("abc123")
                .youtubeChannelId("channel-1")
                .youtubeChannelTitle("AI 스튜디오")
                .youtubeDescription("기억과 사랑을 주제로 제작한 AI 단편영화입니다.")
                .youtubePublishedAt(LocalDateTime.of(2025, 1, 10, 13, 20));
    }

    @Test
    void 등록한_YouTube_출처는_공개_임베드가능_재생가능_상태로_시작하고_동기화시점은_NULL이다() {
        // when
        YoutubeMovieSource source = validSourceBuilder().build();

        // then
        assertThat(source.getMovieId()).isEqualTo(1L);
        assertThat(source.getPrivacyStatus()).isEqualTo(PrivacyStatus.PUBLIC);
        assertThat(source.isEmbeddable()).isTrue();
        assertThat(source.isYoutubeDeleted()).isFalse();
        assertThat(source.getYoutubeAvailabilityStatus()).isEqualTo(YoutubeAvailabilityStatus.AVAILABLE);
        assertThat(source.getYoutubeSyncedAt()).isNull();
    }

    @Test
    void 영화ID_없이는_YoutubeMovieSource를_만들_수_없다() {
        // when & then
        assertThatThrownBy(() -> validSourceBuilder().movieId(null).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void YouTube_영상ID_채널ID_채널이름이_비어_있으면_YoutubeMovieSource를_만들_수_없다(String blank) {
        // when & then
        assertThatThrownBy(() -> validSourceBuilder().youtubeVideoId(blank).build())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validSourceBuilder().youtubeChannelId(blank).build())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validSourceBuilder().youtubeChannelTitle(blank).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

}
