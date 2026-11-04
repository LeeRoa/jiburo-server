package com.jiburo.server.domain.emoticon.domain;

import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * 이모티콘 묶음 (관리자 웹에서 등록/노출 관리).
 * 모든 사용자가 무료로 사용하며, 노출(visible)된 묶음만 채팅 이모티콘 목록에 내려간다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "emoticon_packs")
public class EmoticonPack extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    @Comment("묶음 이름")
    private String name;

    @Column(name = "sort_order", nullable = false)
    @Comment("목록 정렬 순서 (오름차순)")
    private int sortOrder;

    @Column(nullable = false)
    @Comment("사용자에게 노출 여부 (false면 채팅에서 목록·전송 모두 불가, 과거 메시지는 유지)")
    private boolean visible;

    @OneToMany(mappedBy = "pack", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<Emoticon> emoticons = new ArrayList<>();

    @Builder
    public EmoticonPack(String name, int sortOrder, boolean visible) {
        this.name = name;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeVisible(boolean visible) {
        this.visible = visible;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Emoticon addEmoticon(String imageKey) {
        int next = emoticons.stream().mapToInt(Emoticon::getSortOrder).max().orElse(-1) + 1;
        Emoticon emoticon = new Emoticon(this, imageKey, next);
        emoticons.add(emoticon);
        return emoticon;
    }

    public void removeEmoticon(Emoticon emoticon) {
        emoticons.remove(emoticon);
    }
}
