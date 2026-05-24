package com.jiburo.server.domain.image.domain;

import com.jiburo.server.domain.user.domain.User;
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
@Table(name = "image_meta", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"file_key"}) // R2 저장 경로는 전체 시스템에서 유일해야 함
})
public class ImageMeta extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("파일을 업로드한 플랫폼 사용자 계정")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_code", nullable = false, length = 50)
    @Comment("이미지 용도 공통 코드 (예: IMG_PROFILE, IMG_CHAT)")
    private UploadTargetType fileCode;

    @Column(name = "file_key", nullable = false, length = 500)
    @Comment("R2 스토리지에 저장된 실제 경로 및 파일명 (예: IMG_PROFILE/1/uuid.png)")
    private String fileKey;

    @Column(name = "original_file_name")
    @Comment("사용자가 업로드한 원본 파일명")
    private String originalFileName;

    @Column(nullable = false, length = 20)
    @Comment("파일 확장자 (예: png, jpg)")
    private String extension;

    @Column(name = "file_size", nullable = false)
    @Comment("파일 용량 (Byte 단위, 10GB 한도 계산 및 통계용)")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 50)
    @Comment("상태 공통 코드 (예: IMG_STS_PENDING, IMG_STS_COMPLETED)")
    private ImageStatus statusCode;

    @Column(name = "use_yn", nullable = false)
    @Comment("사용 여부 (데이터 삭제 시 false 처리)")
    private boolean useYn;

    @Builder
    public ImageMeta(User user, UploadTargetType fileCode, String fileKey, String originalFileName, String extension, Long fileSize, ImageStatus statusCode) {
        this.user = user;
        this.fileCode = fileCode;
        this.fileKey = fileKey;
        this.originalFileName = originalFileName;
        this.extension = extension;
        this.fileSize = fileSize;
        this.statusCode = statusCode;
        this.useYn = true;
    }

    public void updateStatus(ImageStatus statusCode) {
        this.statusCode = statusCode;
    }
}