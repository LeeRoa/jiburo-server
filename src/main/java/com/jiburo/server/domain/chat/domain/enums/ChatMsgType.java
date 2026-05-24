package com.jiburo.server.domain.chat.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatMsgType implements CommonCodeType {
    // 1. 일반 텍스트 대화
    TALK("TALK", "chat.msg.talk"),

    // 2. 이미지 파일
    IMAGE("IMAGE", "chat.msg.image"),

    // 3. 동영상 파일
    VIDEO("VIDEO", "chat.msg.video"),

    // 4. 지도/위치 정보
    MAP("MAP", "chat.msg.map"),

    // 5. 시스템 메시지 (입장, 퇴장 등)
    SYS("SYS", "chat.msg.sys");

    private final String code;           // DB의 code 컬럼값
    private final String descriptionKey; // 다국어 처리를 위한 키

    @Override
    public String getGroupCode() {
        return "CHAT_MSG_TYPE";
    }
}
