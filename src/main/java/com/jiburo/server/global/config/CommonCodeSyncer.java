package com.jiburo.server.global.config;

import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.domain.image.domain.ImageStatus;
import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.domain.post.domain.enums.AnimalType;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.GenderType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.domain.enums.VisibilityType;
import com.jiburo.server.domain.user.domain.enums.BadgeType;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.cache.CommonCodeCache;
import com.jiburo.server.global.domain.enums.CommonCodeType;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.repository.CommonCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 앱 시작 시 CommonCodeType 구현 enum을 DB에 upsert하고 캐시를 리로드한다.
 * 새 enum 클래스 추가 시 ENUM_CLASSES 리스트에 한 줄만 추가하면 된다.
 * data.sql은 enum과 무관한 설정값(CONFIG 등)에만 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonCodeSyncer implements ApplicationRunner {

    // 새 CommonCodeType enum 추가 시 여기 한 줄 추가
    @SuppressWarnings("rawtypes")
    private static final List<Class<? extends Enum>> ENUM_CLASSES = List.of(
            RoleType.class,
            BadgeType.class,
            CategoryType.class,
            AnimalType.class,
            PostStatus.class,
            GenderType.class,
            VisibilityType.class,
            LogActionType.class,
            ChatMsgType.class,
            ImageStatus.class,
            UploadTargetType.class
    );

    private final CommonCodeRepository repository;
    private final CommonCodeCache cache;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int count = 0;
        for (Class<? extends Enum> cls : ENUM_CLASSES) {
            for (Enum<?> constant : cls.getEnumConstants()) {
                CommonCodeType cct = (CommonCodeType) constant;
                repository.upsert(
                        cct.getGroupCode(),
                        cct.getCode(),
                        cct.getDescriptionKey(),
                        cct.getValue()
                );
                count++;
            }
        }
        cache.reload();
        log.info("CommonCodeSyncer: {} enum values synced to common_codes", count);
    }
}
