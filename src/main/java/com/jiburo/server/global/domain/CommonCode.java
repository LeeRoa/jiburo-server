package com.jiburo.server.global.domain;

import com.jiburo.server.global.consts.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "common_codes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code_group", "code"}) // 그룹 내 코드는 유일해야 함
})
public class CommonCode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 그룹 코드 (예: ANIMAL_TYPE, LOST_STATUS, BADGE_LEVEL)
    // 이 값을 통해 어떤 종류의 코드인지 구분합니다.
    @Column(name = "code_group", nullable = false)
    @Comment("코드 그룹 (예: ANIMAL, BADGE)")
    private String codeGroup;

    // 실제 코드 값 (예: DOG, CAT, BEGINNER)
    @Column(nullable = false)
    @Comment("코드 값 (식별자)")
    private String code;

    // 화면에 표시할 다국어 메시지 키 (예: animal.dog)
    @Column(nullable = false)
    @Comment("다국어 메시지 키")
    private String messageKey;

    // [선택] 부가 정보 1 (예: 뱃지 승급 점수, 정렬 순서 등)
    // 숫자가 들어올 수도 있고 문자가 들어올 수도 있어서 String으로 잡고 필요시 변환
    @Column(name = "ref_1")
    @Comment("참조값 1 (예: 정렬순서, 뱃지 점수)")
    private String ref1;

    // [선택] 부가 정보 2
    @Column(name = "ref_2")
    private String ref2;

    // 사용 여부 (나중에 컬럼을 더 이상 안 쓰게 되면 false로 변경)
    @Column(nullable = false)
    private boolean useYn;
}