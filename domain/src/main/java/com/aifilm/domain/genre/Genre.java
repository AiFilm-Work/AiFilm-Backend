package com.aifilm.domain.genre;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 영화 장르. 기준 데이터는 Flyway migration으로 ID를 지정해 넣는다. (V2__insert_genres.sql)
@Entity
@Table(name = "genre")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    // 장르 이미지가 정해질 때까지 비어 있을 수 있다
    @Column(length = 2048)
    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Genre(String name, String imageUrl) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("장르 이름은 비어 있을 수 없습니다.");
        }
        this.name = name;
        this.imageUrl = imageUrl;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
