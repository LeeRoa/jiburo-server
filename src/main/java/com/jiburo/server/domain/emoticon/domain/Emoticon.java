package com.jiburo.server.domain.emoticon.domain;

import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 개별 이모티콘 (png / 움직이는 gif·webp).
 * 채팅 메시지에는 이 엔티티의 ID가 아니라 imageKey가 저장되므로, 삭제해도 과거 메시지는 그대로 보인다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "emoticons", indexes = {
        @Index(name = "idx_emoticon_pack", columnList = "pack_id, sort_order")
})
public class Emoticon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id", nullable = false)
    @Comment("소속 묶음")
    private EmoticonPack pack;

    @Column(name = "image_key", nullable = false, length = 500)
    @Comment("R2 이미지 키 (image_meta.file_key, UploadTargetType.EMOTICON)")
    private String imageKey;

    @Column(name = "sort_order", nullable = false)
    @Comment("묶음 안 정렬 순서 (오름차순)")
    private int sortOrder;

    Emoticon(EmoticonPack pack, String imageKey, int sortOrder) {
        this.pack = pack;
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
