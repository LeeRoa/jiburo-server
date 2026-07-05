package com.jiburo.server.domain.finder.domain;

import com.jiburo.server.domain.finder.domain.enums.FinderResultType;
import com.jiburo.server.domain.finder.domain.enums.FinderSessionStatus;
import com.jiburo.server.domain.finder.dto.FinderRouteRecordRequestDto;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "finder_sessions", indexes = {
        @Index(name = "idx_finder_session_post", columnList = "post_id"),
        @Index(name = "idx_finder_session_finder", columnList = "finder_user_id")
})
public class FinderSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @Comment("대상 분실 게시글")
    private LostPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finder_user_id", nullable = false)
    @Comment("탐색자")
    private User finder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("세션 상태 (ACTIVE / SUBMITTED / CANCELLED)")
    private FinderSessionStatus statusCode;

    @Column(nullable = false)
    @Comment("탐색 시작 위도")
    private Double startLatitude;

    @Column(nullable = false)
    @Comment("탐색 시작 경도")
    private Double startLongitude;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Comment("탐색 결과 (FOUND / NOT_FOUND)")
    private FinderResultType resultCode;

    @Column(columnDefinition = "TEXT")
    @Comment("탐색 메모")
    private String note;

    @Column
    @Comment("결과 제출 시각")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinderSessionMedia> media = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    @Comment("탐색 경로 포인트 목록 (JSON 배열)")
    private List<FinderRouteRecordRequestDto.RoutePoint> route = new ArrayList<>();

    @Builder
    public FinderSession(LostPost post, User finder, Double startLatitude, Double startLongitude) {
        this.post = post;
        this.finder = finder;
        this.statusCode = FinderSessionStatus.ACTIVE;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
    }

    public void appendRoute(List<FinderRouteRecordRequestDto.RoutePoint> points) {
        if (this.route == null) this.route = new ArrayList<>();
        this.route.addAll(points);
    }

    public void submit(FinderResultType resultCode, String note) {
        this.statusCode = FinderSessionStatus.SUBMITTED;
        this.resultCode = resultCode;
        this.note = note;
        this.submittedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.statusCode = FinderSessionStatus.CANCELLED;
    }

    public boolean isActive() {
        return FinderSessionStatus.ACTIVE.equals(this.statusCode);
    }
}
