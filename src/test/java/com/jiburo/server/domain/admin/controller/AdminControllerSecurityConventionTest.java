package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.domain.AdminMenu;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 API 권한 규약 안전장치.
 * - domain.admin.controller의 모든 엔드포인트는 @PreAuthorize("@adminAccess.xxx(...)")를 가져야 한다 (누락 = 권한 검사 없음)
 * - 메뉴 이름은 AdminMenu에 실제로 있어야 한다 (오타 방지)
 * - 조회(GET)가 아닌 엔드포인트는 write(...) 권한이어야 한다
 */
class AdminControllerSecurityConventionTest {

    private static final Pattern MENU_EXPR = Pattern.compile("^@adminAccess\\.(read|write)\\('([A-Z0-9_]+)'\\)$");
    private static final String ANY_EXPR = "@adminAccess.any()";

    @Test
    void 모든_관리자_API는_메뉴_권한_검사를_가진다() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> violations = new ArrayList<>();
        int endpoints = 0;
        for (BeanDefinition bd : scanner.findCandidateComponents("com.jiburo.server.domain.admin.controller")) {
            Class<?> type = Class.forName(bd.getBeanClassName());
            for (Method method : type.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) continue;
                endpoints++;
                String name = type.getSimpleName() + "#" + method.getName();

                PreAuthorize pre = method.getAnnotation(PreAuthorize.class);
                if (pre == null) {
                    violations.add(name + ": @PreAuthorize 없음");
                    continue;
                }
                String expr = pre.value().trim();
                if (expr.equals(ANY_EXPR)) continue;

                Matcher m = MENU_EXPR.matcher(expr);
                if (!m.matches()) {
                    violations.add(name + ": 지원하지 않는 권한 식 " + expr);
                    continue;
                }
                try {
                    AdminMenu.valueOf(m.group(2));
                } catch (IllegalArgumentException e) {
                    violations.add(name + ": 없는 메뉴 " + m.group(2));
                }
                boolean isGet = AnnotatedElementUtils.hasAnnotation(method, GetMapping.class);
                if (!isGet && m.group(1).equals("read")) {
                    violations.add(name + ": 변경 API인데 read 권한");
                }
            }
        }

        assertThat(endpoints).isPositive();
        assertThat(violations).isEmpty();
    }
}
