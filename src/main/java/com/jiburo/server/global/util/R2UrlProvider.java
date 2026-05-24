package com.jiburo.server.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class R2UrlProvider {

    private static String publicUrl;

    @Value("${cloud.cloudflare.r2.public-url}")
    public void setPublicUrl(String url) {
        publicUrl = url;
    }

    /**
     * 파일 키(경로)를 기반으로 전체 퍼블릭 URL을 생성합니다.
     * @param path 파일 키 (예: post/images/uuid.jpg)
     * @return 전체 URL (예: https://pub-xxx.r2.dev/post/images/uuid.jpg)
     */
    public static String buildUrl(String path) {
        if (path == null || path.isBlank() || path.startsWith("http")) {
            return path;
        }
        
        // publicUrl이 설정되지 않은 경우 원본 경로 반환 (방어 코드)
        if (publicUrl == null || publicUrl.isBlank()) {
            return path;
        }

        String baseUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        String relativePath = path.startsWith("/") ? path : "/" + path;

        return baseUrl + relativePath;
    }
}
