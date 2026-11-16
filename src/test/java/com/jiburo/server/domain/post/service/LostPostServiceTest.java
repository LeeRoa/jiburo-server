package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.*;
import com.jiburo.server.domain.post.dto.*;
import com.jiburo.server.domain.post.dto.detail.AnimalDetailDto;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.post.repository.PostLikeRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LostPostServiceTest {

    @InjectMocks private LostPostServiceImpl lostPostService;

    @Mock private LostPostRepository lostPostRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private UserRepository userRepository;
    @org.mockito.Spy private com.jiburo.server.global.setting.AppSettingService appSettingService =
            com.jiburo.server.global.setting.TestSettings.defaults();
    @Mock private LostPostDeleter lostPostDeleter;
    @Mock private com.jiburo.server.domain.image.service.UploadedFileValidator uploadedFileValidator;
    @org.mockito.Spy private com.jiburo.server.domain.moderation.service.BannedWordFilter bannedWordFilter =
            new com.jiburo.server.domain.moderation.service.BannedWordFilter(org.mockito.Mockito.mock(com.jiburo.server.domain.moderation.repository.BannedWordRepository.class)); // 금지어 없음 = 그대로 통과

    private UUID userId;
    private UUID otherUserId;
    private User user;
    private LostPost post;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);

        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

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

    // ───────── create ─────────

    @Test
    void 게시글_생성_성공() {
        // given
        LostPostCreateRequestDto request = new LostPostCreateRequestDto(
                "제목", "내용", CategoryType.ANIMAL, "레이", AnimalType.ETC, "이구아나",
                null, GenderType.MALE, null, null, null, true,
                List.of("post/images/img1.jpg"), 37.5, 127.0, "서울", LocalDate.now(), 0, VisibilityType.PUBLIC
        );
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(lostPostRepository.save(any(LostPost.class))).willReturn(post);

        // when
        Long result = lostPostService.create(userId, request);

        // then
        assertThat(result).isEqualTo(1L);

        ArgumentCaptor<LostPost> captor = ArgumentCaptor.forClass(LostPost.class);
        then(lostPostRepository).should().save(captor.capture());
        AnimalDetailDto savedDetail = (AnimalDetailDto) captor.getValue().getDetail();
        assertThat(savedDetail.name()).isEqualTo("레이");
        assertThat(savedDetail.animalTypeCode()).isEqualTo(AnimalType.ETC);
        assertThat(savedDetail.animalTypeCustom()).isEqualTo("이구아나");
        assertThat(savedDetail.isBiting()).isTrue();
    }

    @Test
    void 게시글_생성_시_사용자_없으면_USER_NOT_FOUND() {
        // given
        LostPostCreateRequestDto request = new LostPostCreateRequestDto(
                "제목", "내용", CategoryType.ANIMAL, "레이", AnimalType.DOG, null,
                null, GenderType.MALE, null, null, null, null,
                List.of("post/images/img1.jpg"), 37.5, 127.0, "서울", LocalDate.now(), 0, VisibilityType.PUBLIC
        );
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> lostPostService.create(userId, request))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void 게시글_생성_시_첨부_이미지는_본인이_올린_게시글용_파일이어야_한다() {
        LostPostCreateRequestDto request = new LostPostCreateRequestDto(
                "제목", "내용", CategoryType.ANIMAL, "레이", AnimalType.DOG, null,
                null, GenderType.MALE, null, null, null, null,
                List.of("https://evil.example/a.jpg"), 37.5, 127.0, "서울", LocalDate.now(), 0, VisibilityType.PUBLIC
        );
        org.mockito.Mockito.doThrow(new JiburoException(ErrorCode.INVALID_UPLOADED_FILE))
                .when(uploadedFileValidator).requireOwnedUploads(userId, List.of("https://evil.example/a.jpg"),
                        java.util.Set.of(com.jiburo.server.domain.image.domain.UploadTargetType.POST));

        assertThatThrownBy(() -> lostPostService.create(userId, request))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_UPLOADED_FILE));
        then(lostPostRepository).should(never()).save(any());
    }

    @Test
    void 게시글_수정_시_이미_붙어_있던_이미지는_다시_검증하지_않고_새_이미지만_검증한다() {
        post.updateImages(List.of("post/images/old.jpg"));
        LostPostUpdateRequestDto request = new LostPostUpdateRequestDto(
                null, null, List.of("post/images/old.jpg", "post/images/new.jpg"), null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null
        );
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        lostPostService.update(userId, 1L, request);

        then(uploadedFileValidator).should().requireOwnedUploads(userId, List.of("post/images/new.jpg"),
                java.util.Set.of(com.jiburo.server.domain.image.domain.UploadTargetType.POST));
        assertThat(post.getImages()).hasSize(2);
    }

    // ───────── update ─────────

    @Test
    void 게시글_수정_성공() {
        // given
        LostPostUpdateRequestDto request = new LostPostUpdateRequestDto(
                "수정된 제목", "수정된 내용", null, null,
                CategoryType.ANIMAL, null, AnimalType.DOG, null, null, GenderType.FEMALE, null, null, null, null,
                null, null, null, null, 5000
        );
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        // when
        lostPostService.update(userId, 1L, request);

        // then
        assertThat(post.getTitle()).isEqualTo("수정된 제목");
        assertThat(post.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    void 게시글_수정_시_reward를_보내지_않으면_기존_사례금_유지() {
        // given: 제목만 수정 (reward = null)
        LostPostUpdateRequestDto request = new LostPostUpdateRequestDto(
                "제목만 수정", null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null
        );
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        // when
        lostPostService.update(userId, 1L, request);

        // then: 기존에는 primitive int라 0으로 덮어써졌음
        assertThat(post.getTitle()).isEqualTo("제목만 수정");
        assertThat(post.getReward()).isEqualTo(10000);
    }

    @Test
    void 게시글_수정_시_reward를_0으로_보내면_0으로_변경() {
        LostPostUpdateRequestDto request = new LostPostUpdateRequestDto(
                null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, 0
        );
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        lostPostService.update(userId, 1L, request);

        assertThat(post.getReward()).isZero();
    }

    @Test
    void 게시글_수정_시_작성자_아니면_POST_ACCESS_DENIED() {
        // given
        LostPostUpdateRequestDto request = new LostPostUpdateRequestDto(
                "수정된 제목", null, null, null,
                CategoryType.ANIMAL, null, AnimalType.DOG, null, null, null, null, null, null, null,
                null, null, null, null, 0
        );
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> lostPostService.update(otherUserId, 1L, request))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.POST_ACCESS_DENIED));
    }

    // ───────── updateStatus ─────────

    @Test
    void 상태_변경_PROTECTING으로_성공() {
        // given
        LostPostStatusUpdateRequestDto request =
                new LostPostStatusUpdateRequestDto(PostStatus.PROTECTING, null, null);
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        // when
        lostPostService.updateStatus(userId, 1L, request);

        // then
        assertThat(post.getStatusCode()).isEqualTo(PostStatus.PROTECTING);
    }

    @Test
    void 상태_변경_COMPLETE_finder없이_성공() {
        // given
        LostPostStatusUpdateRequestDto request =
                new LostPostStatusUpdateRequestDto(PostStatus.COMPLETE, null, "자력으로 찾았습니다");
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        // when
        lostPostService.updateStatus(userId, 1L, request);

        // then
        assertThat(post.getStatusCode()).isEqualTo(PostStatus.COMPLETE);
        assertThat(post.getResultNote()).isEqualTo("자력으로 찾았습니다");
    }

    @Test
    void 상태_변경_COMPLETE_finder있을_때_성공() {
        // given
        User finder = User.builder()
                .oauthId("kakao_finder")
                .nickname("찾은사람")
                .email("finder@test.com")
                .roleCode(RoleType.USER)
                .build();
        UUID finderId = UUID.randomUUID();
        ReflectionTestUtils.setField(finder, "id", finderId);

        LostPostStatusUpdateRequestDto request =
                new LostPostStatusUpdateRequestDto(PostStatus.COMPLETE, finderId, "감사합니다");
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(finderId)).willReturn(Optional.of(finder));

        // when
        lostPostService.updateStatus(userId, 1L, request);

        // then
        assertThat(post.getStatusCode()).isEqualTo(PostStatus.COMPLETE);
        assertThat(post.getFinder()).isEqualTo(finder);
    }

    @Test
    void 상태_변경_시_작성자_아니면_POST_ACCESS_DENIED() {
        // given
        LostPostStatusUpdateRequestDto request =
                new LostPostStatusUpdateRequestDto(PostStatus.PROTECTING, null, null);
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> lostPostService.updateStatus(otherUserId, 1L, request))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.POST_ACCESS_DENIED));
    }

    // ───────── delete ─────────

    @Test
    void 게시글_삭제_성공() {
        // given
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        // when
        lostPostService.delete(userId, 1L);

        // then: 물리 삭제하지 않고 공통 삭제 규칙(LostPostDeleter)에 위임
        then(lostPostRepository).should(never()).delete(any());
        then(lostPostDeleter).should().delete(post);
    }

    @Test
    void 게시글_삭제_시_작성자_아니면_POST_ACCESS_DENIED() {
        // given
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> lostPostService.delete(otherUserId, 1L))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.POST_ACCESS_DENIED));
    }

    @Test
    void 게시글_삭제_시_게시글_없으면_POST_NOT_FOUND() {
        // given
        given(lostPostRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> lostPostService.delete(userId, 999L))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.POST_NOT_FOUND));
    }

    // ───────── getPostsForList (nearby) ─────────

    @Test
    void 반경_조회_거리순_정렬로_searchByRadius_호출() {
        // given
        LostPostNearbyRequestDto request = new LostPostNearbyRequestDto(
                37.5, 127.0, 3.0, null, null, NearbySortType.DISTANCE, null, 0, 20
        );
        given(lostPostRepository.searchByRadius(request, null))
                .willReturn(new SliceImpl<>(List.of(post), PageRequest.of(0, 20), false));

        // when
        lostPostService.getPostsForList(request, null);

        // then
        then(lostPostRepository).should().searchByRadius(request, null);
    }

    @Test
    void 반경_조회_최신순_정렬로_searchByRadius_호출() {
        // given
        LostPostNearbyRequestDto request = new LostPostNearbyRequestDto(
                37.5, 127.0, 3.0, null, null, NearbySortType.LATEST, null, 0, 20
        );
        given(lostPostRepository.searchByRadius(request, null))
                .willReturn(new SliceImpl<>(List.of(post), PageRequest.of(0, 20), false));

        // when
        lostPostService.getPostsForList(request, null);

        // then
        then(lostPostRepository).should().searchByRadius(request, null);
    }

    @Test
    void 반경을_생략하면_운영_설정_기본_반경을_쓰고_페이지_크기는_최대값으로_제한된다() {
        LostPostNearbyRequestDto request = new LostPostNearbyRequestDto(
                37.5, 127.0, null, null, null, NearbySortType.DISTANCE, null, 0, 500
        );
        LostPostNearbyRequestDto expected = request.withRadiusAndSize(3.0, 50); // 기본 반경 3.0, 최대 크기 50
        given(lostPostRepository.searchByRadius(expected, null))
                .willReturn(new SliceImpl<>(List.of(post), PageRequest.of(0, 50), false));

        lostPostService.getPostsForList(request, null);

        then(lostPostRepository).should().searchByRadius(expected, null);
    }

    @Test
    void 최대_반경을_넘으면_INVALID_INPUT_VALUE() {
        com.jiburo.server.global.setting.TestSettings.with(appSettingService,
                com.jiburo.server.global.setting.SettingKey.NEARBY_MAX_RADIUS_KM, "10");
        LostPostNearbyRequestDto request = new LostPostNearbyRequestDto(
                37.5, 127.0, 20.0, null, null, NearbySortType.DISTANCE, null, 0, 20
        );

        assertThatThrownBy(() -> lostPostService.getPostsForList(request, null))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 지도_마커_요청_개수는_운영_설정_최대값으로_제한된다() {
        com.jiburo.server.domain.post.dto.LostPostMapRequestDto request =
                new com.jiburo.server.domain.post.dto.LostPostMapRequestDto(37.0, 38.0, 126.0, 128.0, 100000);
        given(lostPostRepository.searchByViewport(request.withLimit(300), null)).willReturn(List.of(post));

        lostPostService.getPostsForMap(request, null);

        then(lostPostRepository).should().searchByViewport(request.withLimit(300), null);
    }

    @Test
    void 반경_조회_sortType_null이면_DISTANCE_기본값() {
        // given: sortType을 null로 전달
        LostPostNearbyRequestDto request = new LostPostNearbyRequestDto(
                37.5, 127.0, 3.0, null, null, null, null, 0, 20
        );

        // then: compact constructor에서 DISTANCE로 초기화
        assertThat(request.sortType()).isEqualTo(NearbySortType.DISTANCE);
    }
}
