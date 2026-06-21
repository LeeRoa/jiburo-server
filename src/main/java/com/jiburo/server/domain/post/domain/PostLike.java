package com.jiburo.server.domain.post.domain;

import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_post_likes_user_post",
                columnNames = {"user_id", "lost_post_id"}
        ))
public class PostLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_post_id", nullable = false)
    private LostPost lostPost;

    @Builder
    public PostLike(User user, LostPost lostPost) {
        this.user = user;
        this.lostPost = lostPost;
    }
}
