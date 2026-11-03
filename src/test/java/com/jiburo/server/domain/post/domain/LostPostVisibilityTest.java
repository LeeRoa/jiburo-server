package com.jiburo.server.domain.post.domain;

import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.domain.enums.VisibilityType;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LostPostVisibilityTest {

    private UUID authorId;
    private User author;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        author = User.builder().oauthId("kakao_1").nickname("author").roleCode(RoleType.USER).build();
        ReflectionTestUtils.setField(author, "id", authorId);
    }

    private LostPost post(VisibilityType visibility) {
        return LostPost.builder()
                .user(author)
                .categoryCode(CategoryType.ANIMAL)
                .title("title")
                .statusCode(PostStatus.LOST)
                .latitude(37.5).longitude(127.0)
                .foundLocation("서울")
                .lostDate(LocalDate.now())
                .visibilityCode(visibility)
                .build();
    }

    @Test
    void PUBLIC은_비로그인_포함_누구나_조회_가능() {
        LostPost post = post(VisibilityType.PUBLIC);
        assertThat(post.isVisibleTo(null)).isTrue();
        assertThat(post.isVisibleTo(UUID.randomUUID())).isTrue();
    }

    @Test
    void PROTECTED는_로그인_사용자만_조회_가능() {
        LostPost post = post(VisibilityType.PROTECTED);
        assertThat(post.isVisibleTo(null)).isFalse();
        assertThat(post.isVisibleTo(UUID.randomUUID())).isTrue();
    }

    @Test
    void PRIVATE은_작성자만_조회_가능() {
        LostPost post = post(VisibilityType.PRIVATE);
        assertThat(post.isVisibleTo(null)).isFalse();
        assertThat(post.isVisibleTo(UUID.randomUUID())).isFalse();
        assertThat(post.isVisibleTo(authorId)).isTrue();
    }

    @Test
    void softDelete시_삭제_플래그와_시각이_기록된다() {
        LostPost post = post(VisibilityType.PUBLIC);
        assertThat(post.isDeleted()).isFalse();

        post.softDelete();

        assertThat(post.isDeleted()).isTrue();
        assertThat(post.getDeletedAt()).isNotNull();
    }
}
