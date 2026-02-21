package com.jiburo.server.domain.post.domain;

import com.jiburo.server.domain.post.dto.detail.TargetDetailDto;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.consts.entity.BaseTimeEntity;
import com.jiburo.server.global.domain.CodeConst;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "lost_posts", indexes = {
        @Index(name = "idx_lost_post_location", columnList = "latitude, longitude"),
        @Index(name = "idx_lost_post_status", columnList = "status_code"),
        @Index(name = "idx_lost_post_category", columnList = "category_code")
})
public class LostPost extends BaseTimeEntity {

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

    @OneToMany(mappedBy = "lostPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LostPostImage> images = new ArrayList<>();

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
    @Comment("발견/실종 날짜")
    private LocalDate lostDate;

    @Comment("사례금 (단위: 원)")
    private int reward;

    @Column(nullable = false, length = 20)
    @Comment("공개 권한 코드 (PUBLIC, PROTECTED, PRIVATE)")
    private String visibilityCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finder_id") // Nullable (본인이 찾았거나 비회원이 찾았을 수 있음)
    private User finder;

    @Column(length = 500)
    private String resultNote;

    @Builder
    public LostPost(User user, String categoryCode, String title, String content, String statusCode,
                    TargetDetailDto detail,
                    String imageUrl, Double latitude, Double longitude, String foundLocation,
                    LocalDate lostDate, int reward, String visibilityCode) {
        this.user = user;
        this.categoryCode = categoryCode;
        this.title = title;
        this.content = content;
        this.statusCode = statusCode;
        this.detail = detail;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foundLocation = foundLocation;
        this.lostDate = lostDate;
        this.reward = reward;
        this.visibilityCode = visibilityCode;
    }

    // --- 비즈니스 로직 ---

    public void changeStatus(String newStatusCode) {
        this.statusCode = newStatusCode;
    }

    public void update(String title, String content,
                       String categoryCode,
                       TargetDetailDto detail,
                       Double latitude, Double longitude, String foundLocation,
                       LocalDate lostDate, int reward) {

        // Null 체크 후 업데이트
        if (title != null) this.title = title;
        if (content != null) this.content = content;
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

    // LostPost.java

    public void updateImages(List<String> newImageUrls) {
        if (newImageUrls == null || newImageUrls.isEmpty()) {
            // (정책에 따라 빈 리스트를 허용하지 않는다면 예외 처리, 허용한다면 clear)
            this.images.clear();
            return;
        }

        // 1. 변경 감지: 기존 URL 목록과 새 URL 목록이 순서까지 완전히 같은지 비교
        List<String> currentUrls = this.images.stream()
                .map(LostPostImage::getImageUrl)
                .toList();

        // List.equals()는 요소의 '순서'와 '내용'이 모두 같아야 true를 반환함
        // 즉, [A, B]와 [B, A]도 다르다고 판단하므로 썸네일(0번 인덱스) 변경도 감지 가능
        if (currentUrls.equals(newImageUrls)) {
            return; // 변경 없음 -> 아무것도 안 하고 종료 (쿼리 안 나감)
        }

        // 2. 변경이 있다면 기존 로직 수행 (삭제 후 재생성)
        this.images.clear();

        // 3. 대표 이미지(썸네일) 갱신
        this.imageUrl = newImageUrls.get(0);

        // 4. 새 이미지 리스트 추가
        for (int i = 0; i < newImageUrls.size(); i++) {
            this.images.add(LostPostImage.builder()
                    .lostPost(this)
                    .imageUrl(newImageUrls.get(i))
                    .order(i)
                    .build());
        }
    }

    public void complete(User finder, String resultNote) {
        this.statusCode = CodeConst.Status.COMPLETE;
        this.finder = finder; // 회원이면 User 객체, 아니면 null
        this.resultNote = resultNote;
    }
}
