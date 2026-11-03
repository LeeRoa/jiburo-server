package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.finder.repository.FinderSessionRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.domain.enums.VisibilityType;
import com.jiburo.server.domain.post.repository.PostLikeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LostPostDeleterTest {

    @Test
    void 삭제는_플래그_변경_좋아요_정리_진행중_탐색세션_취소를_함께_수행한다() {
        PostLikeRepository likes = mock(PostLikeRepository.class);
        FinderSessionRepository sessions = mock(FinderSessionRepository.class);
        LostPost post = LostPost.builder().categoryCode(CategoryType.ANIMAL).title("t").statusCode(PostStatus.LOST)
                .latitude(37.5).longitude(127.0).foundLocation("서울").lostDate(LocalDate.now())
                .visibilityCode(VisibilityType.PUBLIC).build();
        ReflectionTestUtils.setField(post, "id", 7L);

        new LostPostDeleter(likes, sessions).delete(post);

        assertThat(post.isDeleted()).isTrue();
        assertThat(post.getDeletedAt()).isNotNull();
        verify(likes).deleteAllByLostPostId(7L);
        verify(sessions).cancelActiveSessionsByPostId(7L);
    }
}
