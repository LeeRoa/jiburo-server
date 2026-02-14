package com.jiburo.server.domain.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "lost_post_images")
public class LostPostImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_post_id", nullable = false)
    private LostPost lostPost;

    @Column(nullable = false)
    private String imageUrl;

    @Column(name = "image_order")
    private int order; // 이미지 순서 (1, 2, 3...)

    @Builder
    public LostPostImage(LostPost lostPost, String imageUrl, int order) {
        this.lostPost = lostPost;
        this.imageUrl = imageUrl;
        this.order = order;
    }
}
