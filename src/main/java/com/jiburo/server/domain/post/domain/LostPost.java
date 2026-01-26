package com.jiburo.server.domain.post.domain;

import com.jiburo.server.domain.post.dto.detail.TargetDetailDto;
import com.jiburo.server.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "lost_posts", indexes = {
        @Index(name = "idx_lost_post_location", columnList = "latitude, longitude"),
        @Index(name = "idx_lost_post_status", columnList = "status_code"),
        @Index(name = "idx_lost_post_category", columnList = "category_code")
})
public class LostPost extends com.jiburo.server.global.consts.entity.BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("작성자")
    private User user;

    // --- 게시글 기본 정보 ---
    @Column(nullable = false, length = 20)
    @Comment("대분류 코드 (ANIMAL, PERSON, ITEM)")
    private String categoryCode;

    @Column(nullable = false, length = 100)
    @Comment("제목")
    private String title;

    @Column(columnDefinition = "TEXT")
    @Comment("상세 내용")
    private String content;

    @Column(nullable = false, length = 20)
    @Comment("상태 코드 (LOST, COMPLETE)")
    private String statusCode;

    // ---  상세 정보 (JSON) ---
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    @Comment("상세 정보 (JSON 데이터)")
    private TargetDetailDto detail;

    // --- 이미지 및 위치 정보 ---
    @Column(nullable = false)
    @Comment("대표 이미지 URL")
    private String imageUrl;

    @Column(nullable = false)
    @Comment("위도 (Latitude)")
    private Double latitude;

    @Column(nullable = false)
    @Comment("경도 (Longitude)")
    private Double longitude;

    @Column(nullable = false)
    @Comment("발견/실종 장소명 (주소)")
    private String foundLocation;

    @Column(nullable = false)
    @Comment("실종 날짜")
    private LocalDate lostDate;

    @Comment("사례금 (단위: 원)")
    private int reward;

    @Builder
    public LostPost(User user, String categoryCode, String title, String content, String statusCode,
                    TargetDetailDto detail, // [변경] 개별 필드 대신 객체 통째로 받음
                    String imageUrl, Double latitude, Double longitude, String foundLocation,
                    LocalDate lostDate, int reward) {
        this.user = user;
        this.categoryCode = categoryCode;
        this.title = title;
        this.content = content;
        this.statusCode = statusCode;
        this.detail = detail; // [변경]
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foundLocation = foundLocation;
        this.lostDate = lostDate;
        this.reward = reward;
    }

    // --- 비즈니스 로직 ---

    public void changeStatus(String newStatusCode) {
        this.statusCode = newStatusCode;
    }

    public void update(String title, String content, String imageUrl,
                       String categoryCode,
                       TargetDetailDto detail,
                       Double latitude, Double longitude, String foundLocation,
                       LocalDate lostDate, int reward) {

        // Null 체크 후 업데이트 (Dirty Checking)
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (categoryCode != null) this.categoryCode = categoryCode;

        // 상세 정보 업데이트 (카테고리가 바뀌면 상세 정보 구조도 바뀜 -> JSON이 알아서 처리)
        if (detail != null) this.detail = detail;

        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (foundLocation != null) this.foundLocation = foundLocation;
        if (lostDate != null) this.lostDate = lostDate;

        // primitive type인 int는 0이 올 수 있으므로 주의 (여기선 그냥 덮어쓰기)
        this.reward = reward;
    }
}