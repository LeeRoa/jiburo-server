package com.jiburo.server.domain.image.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UploadTargetTypeTest {

    @Test
    void 모든_허용_확장자에는_서명할_Content_Type이_정의되어_있다() {
        // 허용 확장자만 추가하고 CONTENT_TYPES를 빠뜨리면 그 확장자는 업로드 URL을 받을 수 없게 된다
        for (UploadTargetType type : UploadTargetType.values()) {
            for (String ext : type.getAllowedExtensions()) {
                assertThat(type.contentTypeOf(ext)).as("%s.%s", type, ext).isNotBlank();
            }
        }
    }

    @Test
    void 이미지_대상은_이미지_타입만_동영상_대상은_동영상_타입만_서명한다() {
        for (UploadTargetType type : UploadTargetType.values()) {
            String prefix = type.isVideo() ? "video/" : "image/";
            for (String ext : type.getAllowedExtensions()) {
                assertThat(type.contentTypeOf(ext)).as("%s.%s", type, ext).startsWith(prefix);
            }
        }
    }

    @Test
    void 허용되지_않은_확장자는_Content_Type이_없다() {
        assertThat(UploadTargetType.POST.contentTypeOf("html")).isNull();
        assertThat(UploadTargetType.POST.contentTypeOf("mp4")).isNull(); // 동영상은 게시글 이미지 대상이 아님
        assertThat(UploadTargetType.POST.contentTypeOf(null)).isNull();
        assertThat(UploadTargetType.POST.contentTypeOf("JPG")).isEqualTo("image/jpeg");
    }
}
