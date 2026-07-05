package com.jiburo.server.global.config;

import com.jiburo.server.global.cache.CommonCodeCache;
import com.jiburo.server.global.domain.enums.CommonCodeType;
import com.jiburo.server.global.repository.CommonCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 앱 시작 시 CommonCodeType 구현 enum을 DB에 upsert하고 캐시를 리로드한다.
 * com.jiburo.server 하위 패키지를 자동 스캔하므로 새 enum 추가 시 별도 등록 불필요.
 * data.sql은 enum과 무관한 설정값(CONFIG 등)에만 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonCodeSyncer implements ApplicationRunner {

    private static final String BASE_PACKAGE = "com.jiburo.server";

    private final CommonCodeRepository repository;
    private final CommonCodeCache cache;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Class<? extends Enum<?>>> enumClasses = scanEnumClasses();
        int count = 0;

        for (Class<? extends Enum<?>> cls : enumClasses) {
            for (Enum<?> constant : cls.getEnumConstants()) {
                CommonCodeType cct = (CommonCodeType) constant;
                repository.upsert(
                        cct.getGroupCode(),
                        cct.getCode(),
                        cct.getDescriptionKey(),
                        cct.getValue(),
                        cct.getRef2()
                );
                count++;
            }
        }

        cache.reload();
        log.info("CommonCodeSyncer: {} enum values synced ({} enums)", count, enumClasses.size());
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends Enum<?>>> scanEnumClasses() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(CommonCodeType.class));

        List<Class<? extends Enum<?>>> result = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(BASE_PACKAGE)) {
            try {
                Class<?> cls = Class.forName(bd.getBeanClassName());
                if (cls.isEnum()) {
                    result.add((Class<? extends Enum<?>>) cls);
                }
            } catch (ClassNotFoundException e) {
                log.warn("CommonCodeSyncer: cannot load class {}", bd.getBeanClassName());
            }
        }
        return result;
    }
}
