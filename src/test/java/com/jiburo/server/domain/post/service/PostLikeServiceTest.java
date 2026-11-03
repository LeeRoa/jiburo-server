package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.PostLike;
import com.jiburo.server.domain.post.domain.enums.AnimalType;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.GenderType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.domain.enums.VisibilityType;
import com.jiburo.server.domain.post.dto.LostPostResponseDto;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import com.jiburo.server.domain.post.dto.detail.AnimalDetailDto;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.post.repository.PostLikeRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @InjectMocks
    private LostPostServiceImpl lostPostService;

    @Mock private LostPostRepository lostPostRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private UserRepository userRepository;

    private UUID userId;
    private User user;
    private LostPost post;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);

        userId = UUID.randomUUID();
        user = User.builder()
                .oauthId("google_test")
                .nickname("테스터")
                .email("test@test.com")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        post = LostPost.builder()
                .user(user)
                .categoryCode(CategoryType.ANIMAL)
                .title("테스트 게시글")
                .content("내용")
                .statusCode(PostStatus.LOST)
                .detail(AnimalDetailDto.builder()
                        .animalTypeCode(AnimalType.DOG)
                        .genderCode(GenderType.MALE)
                        .build())
                .imageUrl("post/images/test.jpg")
                .latitude(37.5)
                .longitude(127.0)
                .foundLocation("서울 강남구")
                .lostDate(LocalDate.of(2024, 1, 1))
                .reward(10000)
                .visibilityCode(VisibilityType.PUBLIC)
                .build();
        ReflectionTestUtils.setField(post, "id", 1L);
    }

    // ───────── toggleLike ─────────

    @Test
    void 좋아요_추가_성공() {
        // given
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.findByUser_IdAndLostPost_Id(userId, 1L)).willReturn(Optional.empty());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(postLikeRepository.save(any(PostLike.class)))
                .willReturn(PostLike.builder().user(user).lostPost(post).build());

        // when
        boolean result = lostPostService.toggleLike(userId, 1L);

        // then
        assertThat(result).isTrue();
        then(postLikeRepository).should().save(any(PostLike.class));
        then(postLikeRepository).should(never()).delete(any());
    }

    @Test
    void 좋아요_취소_성공() {
        // given
        PostLike existingLike = PostLike.builder().user(user).lostPost(post).build();
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.findByUser_IdAndLostPost_Id(userId, 1L)).willReturn(Optional.of(existingLike));

        // when
        boolean result = lostPostService.toggleLike(userId, 1L);

        // then
        assertThat(result).isFalse();
        then(postLikeRepository).should().delete(existingLike);
        then(postLikeRepository).should(never()).save(any());
    }

    @Test
    void 좋아요_시_게시글_없으면_POST_NOT_FOUND() {
        // given
        given(lostPostRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> lostPostService.toggleLike(userId, 999L))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.POST_NOT_FOUND));
    }

    // ───────── findById ─────────

    @Test
    void 단건_조회_로그인_시_isLiked_true_likeCount_반환() {
        // given
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.countByLostPost_Id(1L)).willReturn(5L);
        given(postLikeRepository.existsByUser_IdAndLostPost_Id(userId, 1L)).willReturn(true);

        // when
        LostPostResponseDto result = lostPostService.findById(1L, userId);

        // then
        assertThat(result.isLiked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(5L);
    }

    @Test
    void 단건_조회_비로그인_시_isLiked_false_likeCount_반환() {
        // given
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.countByLostPost_Id(1L)).willReturn(3L);

        // when
        LostPostResponseDto result = lostPostService.findById(1L, null);

        // then
        assertThat(result.isLiked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(3L);
        then(postLikeRepository).should(never()).existsByUser_IdAndLostPost_Id(any(), any());
    }

    @Test
    void 단건_조회_게시글_없으면_POST_NOT_FOUND() {
        // given
        given(lostPostRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> lostPostService.findById(999L, userId))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.POST_NOT_FOUND));
    }

    // ───────── getLikedPosts ─────────

    @Test
    void 관심_게시글_목록_조회_성공() {
        // given
        PostLike like = PostLike.builder().user(user).lostPost(post).build();
        Page<PostLike> likePage = new PageImpl<>(List.of(like));
        Pageable pageable = Pageable.ofSize(10);

        List<Object[]> countResult = new ArrayList<>();
        countResult.add(new Object[]{1L, 2L});

        given(postLikeRepository.findByUser_IdWithPost(userId, pageable)).willReturn(likePage);
        given(postLikeRepository.countGroupByLostPostIdIn(any())).willReturn(countResult);

        // when
        Page<LostPostResponseDto> result = lostPostService.getLikedPosts(userId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isLiked()).isTrue();
        assertThat(result.getContent().get(0).likeCount()).isEqualTo(2L);
    }

    @Test
    void 관심_게시글_빈_목록_반환() {
        // given
        Pageable pageable = Pageable.ofSize(10);
        given(postLikeRepository.findByUser_IdWithPost(userId, pageable)).willReturn(Page.empty(pageable));

        // when
        Page<LostPostResponseDto> result = lostPostService.getLikedPosts(userId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        then(postLikeRepository).should(never()).countGroupByLostPostIdIn(any());
    }

    // ───────── search (isLiked/likeCount 포함) ─────────

    @Test
    void 목록_검색_로그인_시_isLiked_포함() {
        // given
        Page<LostPost> postPage = new PageImpl<>(List.of(post));
        Pageable pageable = Pageable.ofSize(10);
        LostPostSearchCondition condition = new LostPostSearchCondition(null, null, null, null, null, null);

        List<Object[]> countResult = new ArrayList<>();
        countResult.add(new Object[]{1L, 7L});

        given(lostPostRepository.search(any(LostPostSearchCondition.class), any())).willReturn(postPage);
        given(postLikeRepository.countGroupByLostPostIdIn(any())).willReturn(countResult);
        given(postLikeRepository.findLikedPostIdsByUser_IdAndPostIds(userId, List.of(1L))).willReturn(Set.of(1L));

        // when
        Page<LostPostResponseDto> result = lostPostService.search(condition, pageable, userId);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isLiked()).isTrue();
        assertThat(result.getContent().get(0).likeCount()).isEqualTo(7L);
    }

    @Test
    void 목록_검색_비로그인_시_isLiked_전부_false() {
        // given
        Page<LostPost> postPage = new PageImpl<>(List.of(post));
        Pageable pageable = Pageable.ofSize(10);
        LostPostSearchCondition condition = new LostPostSearchCondition(null, null, null, null, null, null);

        given(lostPostRepository.search(any(LostPostSearchCondition.class), any())).willReturn(postPage);
        given(postLikeRepository.countGroupByLostPostIdIn(any())).willReturn(new ArrayList<>());

        // when
        Page<LostPostResponseDto> result = lostPostService.search(condition, pageable, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isLiked()).isFalse();
        then(postLikeRepository).should(never()).findLikedPostIdsByUser_IdAndPostIds(any(), any());
    }
}
