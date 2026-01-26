package com.jiburo.server.domain.post.domain;

import com.jiburo.server.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "lost_posts", indexes = {
        // 지도 범위 검색 성능 향상 (위도, 경도)
        @Index(name = "idx_lost_post_location", columnList = "latitude, longitude"),
        // 상태별 필터링 성능 향상 (필드명이 status -> status_code로 바뀜에 주의)
        @Index(name = "idx_lost_post_status", columnList = "status_code")
})
public class LostPost extends com.jiburo.server.global.consts.entity.BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("작성자")
    private User user;

    // --- 게시글 정보 ---
    @Column(nullable = false, length = 100)
    @Comment("제목")
    private String title;

    @Column(columnDefinition = "TEXT")
    @Comment("상세 내용")
    private String content;

    // CommonCode 테이블의 'STATUS' 그룹 코드 값 (예: LOST, COMPLETE)
    @Column(nullable = false, length = 20)
    @Comment("상태 코드 (LOST, COMPLETE)")
    private String statusCode;

    // --- 동물 정보 ---

    // CommonCode 테이블의 'ANIMAL' 그룹 코드 값 (예: DOG, CAT)
    @Column(nullable = false, length = 20)
    @Comment("동물 종류 코드 (DOG, CAT)")
    private String animalTypeCode;

    @Comment("품종 (예: 말티즈)")
    private String breed;

    // CommonCode 테이블의 'GENDER' 그룹 코드 값 (예: MALE, FEMALE)
    @Column(length = 20)
    @Comment("성별 코드 (MALE, FEMALE)")
    private String genderCode;

    @Comment("털 색상")
    private String color;

    @Comment("추정 나이")
    private Integer age;

    @Column(nullable = false)
    @Comment("대표 이미지 URL")
    private String imageUrl;

    // --- 지도/위치 정보 ---
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
    public LostPost(User user, String title, String content, String statusCode,
                    String animalTypeCode, String breed, String genderCode, String color, Integer age,
                    String imageUrl, Double latitude, Double longitude, String foundLocation,
                    LocalDate lostDate, int reward) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.statusCode = statusCode;
        this.animalTypeCode = animalTypeCode;
        this.breed = breed;
        this.genderCode = genderCode;
        this.color = color;
        this.age = age;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foundLocation = foundLocation;
        this.lostDate = lostDate;
        this.reward = reward;
    }

    // --- 비즈니스 로직 ---

    // 상태 변경 (문자열 코드로 받음)
    public void changeStatus(String newStatusCode) {
        this.statusCode = newStatusCode;
    }

    // 내용 수정
    public void update(String title, String content, String imageUrl,
                       String animalTypeCode, String breed, String genderCode,
                       String color, Integer age,
                       Double latitude, Double longitude, String foundLocation,
                       LocalDate lostDate, int reward) {

        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.animalTypeCode = animalTypeCode;
        this.breed = breed;
        this.genderCode = genderCode;
        this.color = color;
        this.age = age;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foundLocation = foundLocation;
        this.lostDate = lostDate;
        this.reward = reward;
    }
}