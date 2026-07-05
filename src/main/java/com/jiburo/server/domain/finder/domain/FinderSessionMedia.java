package com.jiburo.server.domain.finder.domain;

import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "finder_session_media")
public class FinderSessionMedia extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @Comment("연결된 탐색 세션")
    private FinderSession session;

    @Column(nullable = false, length = 500)
    @Comment("R2 objectKey (파일 전체 경로)")
    private String fileKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Comment("미디어 유형 (FINDER_IMAGE / FINDER_VIDEO) — 텍스트 제보 시 null")
    private UploadTargetType mediaType;

    @Column
    @Comment("촬영 위치 위도")
    private Double latitude;

    @Column
    @Comment("촬영 위치 경도")
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    @Comment("메모 / 텍스트 제보 내용")
    private String memo;

    @Builder
    public FinderSessionMedia(FinderSession session, String fileKey, UploadTargetType mediaType,
                               Double latitude, Double longitude, String memo) {
        this.session = session;
        this.fileKey = fileKey;
        this.mediaType = mediaType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.memo = memo;
    }
}
