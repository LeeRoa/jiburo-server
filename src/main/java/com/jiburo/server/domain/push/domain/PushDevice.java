package com.jiburo.server.domain.push.domain;

import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 푸시를 받을 기기 (FCM 등록 토큰). 한 사용자가 여러 기기를 가질 수 있고,
 * 한 기기(토큰)는 마지막으로 로그인한 사용자 한 명에게만 묶인다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "push_devices",
        uniqueConstraints = @UniqueConstraint(name = "uq_push_device_token", columnNames = "token"),
        indexes = @Index(name = "idx_push_device_user", columnList = "user_id"))
public class PushDevice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("기기 소유 사용자")
    private User user;

    @Column(nullable = false, length = 500)
    @Comment("FCM 등록 토큰")
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(nullable = false, length = 20)
    @Comment("푸시 문구 언어 (등록 시 Accept-Language, BCP 47)")
    private String locale;

    public PushDevice(User user, String token, DevicePlatform platform, String locale) {
        this.user = user;
        this.token = token;
        this.platform = platform;
        this.locale = locale;
    }

    /** 같은 기기에 다른 계정으로 로그인했거나 정보가 바뀐 경우 */
    public void reassign(User user, DevicePlatform platform, String locale) {
        this.user = user;
        this.platform = platform;
        this.locale = locale;
    }
}
