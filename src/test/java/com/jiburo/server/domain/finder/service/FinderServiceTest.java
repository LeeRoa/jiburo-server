package com.jiburo.server.domain.finder.service;

import com.jiburo.server.domain.finder.domain.FinderSession;
import com.jiburo.server.domain.finder.domain.FinderSessionMedia;
import com.jiburo.server.domain.finder.domain.enums.FinderResultType;
import com.jiburo.server.domain.finder.domain.enums.FinderSessionStatus;
import com.jiburo.server.domain.finder.dto.*;
import com.jiburo.server.domain.finder.repository.FinderSessionMediaRepository;
import com.jiburo.server.domain.finder.repository.FinderSessionRepository;
import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.domain.notification.repository.NotificationRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.repository.LostPostRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FinderServiceTest {

    @InjectMocks private FinderServiceImpl finderService;

    @Mock private FinderSessionRepository finderSessionRepository;
    @Mock private FinderSessionMediaRepository finderSessionMediaRepository;
    @Mock private LostPostRepository lostPostRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;

    private UUID finderId;
    private UUID postOwnerId;
    private User finder;
    private User postOwner;
    private LostPost post;
    private FinderSession activeSession;
    private String encodedPostId;
    private String encodedSessionId;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);

        finderId = UUID.randomUUID();
        postOwnerId = UUID.randomUUID();

        finder = User.builder()
                .oauthId("google_finder")
                .nickname("탐색자")
                .email("finder@test.com")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(finder, "id", finderId);

        postOwner = User.builder()
                .oauthId("google_owner")
                .nickname("게시글작성자")
                .email("owner@test.com")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(postOwner, "id", postOwnerId);

        post = LostPost.builder()
                .user(postOwner)
                .categoryCode(CategoryType.ANIMAL)
                .title("분실 강아지 찾아요")
                .content("골든 리트리버 분실")
                .statusCode(PostStatus.LOST)
                .latitude(37.5)
                .longitude(127.0)
                .foundLocation("서울 강남구")
                .visibilityCode(com.jiburo.server.domain.post.domain.enums.VisibilityType.PUBLIC)
                .build();
        ReflectionTestUtils.setField(post, "id", 1L);

        activeSession = FinderSession.builder()
                .post(post)
                .finder(finder)
                .startLatitude(37.5)
                .startLongitude(127.0)
                .build();
        ReflectionTestUtils.setField(activeSession, "id", 10L);

        encodedPostId = HashidsUtils.encode(1L);
        encodedSessionId = HashidsUtils.encode(10L);
    }

    // ───────── createSession ─────────

    @Test
    void 세션_생성_성공() {
        FinderSessionCreateRequestDto dto = new FinderSessionCreateRequestDto(encodedPostId, 37.5, 127.0);
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(finderId)).willReturn(Optional.of(finder));
        given(finderSessionRepository.existsByPost_IdAndFinder_IdAndStatusCode(1L, finderId, FinderSessionStatus.ACTIVE))
                .willReturn(false);
        doAnswer(inv -> {
            ReflectionTestUtils.setField(inv.getArgument(0, FinderSession.class), "id", 10L);
            return inv.getArgument(0);
        }).when(finderSessionRepository).save(any(FinderSession.class));

        String result = finderService.createSession(finderId, dto);

        assertThat(result).isEqualTo(encodedSessionId);
        then(finderSessionRepository).should().save(any(FinderSession.class));
        then(notificationRepository).should().save(any());
    }

    @Test
    void 세션_생성_시_게시글_없으면_POST_NOT_FOUND() {
        FinderSessionCreateRequestDto dto = new FinderSessionCreateRequestDto(encodedPostId, 37.5, 127.0);
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> finderService.createSession(finderId, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
    }

    @Test
    void 세션_생성_시_사용자_없으면_USER_NOT_FOUND() {
        FinderSessionCreateRequestDto dto = new FinderSessionCreateRequestDto(encodedPostId, 37.5, 127.0);
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(finderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> finderService.createSession(finderId, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void 자신의_게시글에_세션_생성하면_FINDER_CANNOT_FIND_OWN_POST() {
        FinderSessionCreateRequestDto dto = new FinderSessionCreateRequestDto(encodedPostId, 37.5, 127.0);
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(postOwnerId)).willReturn(Optional.of(postOwner));

        assertThatThrownBy(() -> finderService.createSession(postOwnerId, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_CANNOT_FIND_OWN_POST));
    }

    @Test
    void 완료된_게시글에_세션_생성하면_FINDER_POST_ALREADY_COMPLETE() {
        ReflectionTestUtils.setField(post, "statusCode", PostStatus.COMPLETE);
        FinderSessionCreateRequestDto dto = new FinderSessionCreateRequestDto(encodedPostId, 37.5, 127.0);
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(finderId)).willReturn(Optional.of(finder));

        assertThatThrownBy(() -> finderService.createSession(finderId, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_POST_ALREADY_COMPLETE));
    }

    @Test
    void 이미_ACTIVE_세션이_있으면_FINDER_SESSION_ALREADY_EXISTS() {
        FinderSessionCreateRequestDto dto = new FinderSessionCreateRequestDto(encodedPostId, 37.5, 127.0);
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(userRepository.findById(finderId)).willReturn(Optional.of(finder));
        given(finderSessionRepository.existsByPost_IdAndFinder_IdAndStatusCode(1L, finderId, FinderSessionStatus.ACTIVE))
                .willReturn(true);

        assertThatThrownBy(() -> finderService.createSession(finderId, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_SESSION_ALREADY_EXISTS));
    }

    // ───────── getSession ─────────

    @Test
    void 세션_조회_성공() {
        given(finderSessionRepository.findByIdWithMedia(10L)).willReturn(Optional.of(activeSession));

        FinderSessionResponseDto result = finderService.getSession(finderId, 10L);

        assertThat(result.id()).isEqualTo(encodedSessionId);
        assertThat(result.statusCode()).isEqualTo(FinderSessionStatus.ACTIVE.name());
    }

    @Test
    void 세션_조회_시_세션_없으면_FINDER_SESSION_NOT_FOUND() {
        given(finderSessionRepository.findByIdWithMedia(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> finderService.getSession(finderId, 999L))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_SESSION_NOT_FOUND));
    }

    @Test
    void 세션_조회_시_본인이_아니면_FINDER_SESSION_ACCESS_DENIED() {
        UUID otherUserId = UUID.randomUUID();
        given(finderSessionRepository.findByIdWithMedia(10L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> finderService.getSession(otherUserId, 10L))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_SESSION_ACCESS_DENIED));
    }

    // ───────── getMySessions ─────────

    @Test
    void 내_세션_목록_조회_성공() {
        var pageable = PageRequest.of(0, 10);
        given(finderSessionRepository.findByFinderIdWithPost(finderId, pageable))
                .willReturn(new SliceImpl<>(List.of(activeSession), pageable, false));

        var result = finderService.getMySessions(finderId, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // ───────── addMedia ─────────

    @Test
    void 미디어_추가_단일_파일_성공() {
        FinderMediaAddRequestDto dto = new FinderMediaAddRequestDto(
                List.of("finder/images/test.jpg"), UploadTargetType.FINDER_IMAGE, 37.5, 127.0, "골목 입구에서 발견"
        );
        FinderSessionMedia media = FinderSessionMedia.builder()
                .session(activeSession).fileKey("finder/images/test.jpg")
                .mediaType(UploadTargetType.FINDER_IMAGE).latitude(37.5).longitude(127.0)
                .memo("골목 입구에서 발견").build();
        ReflectionTestUtils.setField(media, "id", 100L);

        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));
        given(finderSessionMediaRepository.save(any(FinderSessionMedia.class))).willReturn(media);

        var result = finderService.addMedia(finderId, 10L, dto);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fileKey()).isEqualTo("finder/images/test.jpg");
        assertThat(result.get(0).mediaType()).isEqualTo(UploadTargetType.FINDER_IMAGE.name());
    }

    @Test
    void 미디어_추가_여러_파일_성공() {
        FinderMediaAddRequestDto dto = new FinderMediaAddRequestDto(
                List.of("finder/images/a.jpg", "finder/images/b.jpg", "finder/images/c.jpg"),
                UploadTargetType.FINDER_IMAGE, null, null, null
        );
        FinderSessionMedia media = FinderSessionMedia.builder()
                .session(activeSession).fileKey("finder/images/a.jpg")
                .mediaType(UploadTargetType.FINDER_IMAGE).build();
        ReflectionTestUtils.setField(media, "id", 100L);

        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));
        given(finderSessionMediaRepository.save(any(FinderSessionMedia.class))).willReturn(media);

        var result = finderService.addMedia(finderId, 10L, dto);

        assertThat(result).hasSize(3);
    }

    @Test
    void 미디어_추가_영상_성공() {
        FinderMediaAddRequestDto dto = new FinderMediaAddRequestDto(
                List.of("finder/videos/test.mp4"), UploadTargetType.FINDER_VIDEO, null, null, null
        );
        FinderSessionMedia media = FinderSessionMedia.builder()
                .session(activeSession).fileKey("finder/videos/test.mp4")
                .mediaType(UploadTargetType.FINDER_VIDEO).build();
        ReflectionTestUtils.setField(media, "id", 101L);

        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));
        given(finderSessionMediaRepository.save(any(FinderSessionMedia.class))).willReturn(media);

        var result = finderService.addMedia(finderId, 10L, dto);

        assertThat(result.get(0).mediaType()).isEqualTo(UploadTargetType.FINDER_VIDEO.name());
    }

    @Test
    void 미디어_추가_메모만_텍스트_제보_성공() {
        FinderMediaAddRequestDto dto = new FinderMediaAddRequestDto(
                null, null, null, null, "오후 3시쯤 한강공원에서 목격했습니다"
        );
        FinderSessionMedia media = FinderSessionMedia.builder()
                .session(activeSession).memo("오후 3시쯤 한강공원에서 목격했습니다").build();
        ReflectionTestUtils.setField(media, "id", 102L);

        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));
        given(finderSessionMediaRepository.save(any(FinderSessionMedia.class))).willReturn(media);

        var result = finderService.addMedia(finderId, 10L, dto);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).memo()).isEqualTo("오후 3시쯤 한강공원에서 목격했습니다");
        assertThat(result.get(0).fileKey()).isNull();
    }

    @Test
    void 미디어_추가_시_파일도_메모도_없으면_INVALID_INPUT_VALUE() {
        FinderMediaAddRequestDto dto = new FinderMediaAddRequestDto(null, null, null, null, null);
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> finderService.addMedia(finderId, 10L, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 미디어_추가_시_유효하지_않은_타입이면_INVALID_FILE_CODE() {
        FinderMediaAddRequestDto dto = new FinderMediaAddRequestDto(
                List.of("post/images/test.jpg"), UploadTargetType.POST, null, null, null
        );
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> finderService.addMedia(finderId, 10L, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_CODE));
    }

    @Test
    void 미디어_추가_시_본인_세션_아니면_FINDER_SESSION_ACCESS_DENIED() {
        UUID otherUserId = UUID.randomUUID();
        FinderMediaAddRequestDto dto = new FinderMediaAddRequestDto(
                List.of("finder/images/test.jpg"), UploadTargetType.FINDER_IMAGE, null, null, null
        );
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> finderService.addMedia(otherUserId, 10L, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_SESSION_ACCESS_DENIED));
    }

    @Test
    void 미디어_추가_시_ACTIVE가_아닌_세션이면_FINDER_SESSION_ALREADY_SUBMITTED() {
        activeSession.submit(FinderResultType.FOUND, "찾았습니다");
        FinderMediaAddRequestDto dto = new FinderMediaAddRequestDto(
                List.of("finder/images/test.jpg"), UploadTargetType.FINDER_IMAGE, null, null, null
        );
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> finderService.addMedia(finderId, 10L, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_SESSION_ALREADY_SUBMITTED));
    }

    // ───────── deleteMedia ─────────

    @Test
    void 미디어_삭제_성공() {
        FinderSessionMedia media = FinderSessionMedia.builder()
                .session(activeSession).fileKey("finder/images/test.jpg")
                .mediaType(UploadTargetType.FINDER_IMAGE).build();
        ReflectionTestUtils.setField(media, "id", 100L);

        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));
        given(finderSessionMediaRepository.findByIdAndSession_Id(100L, 10L)).willReturn(Optional.of(media));

        finderService.deleteMedia(finderId, 10L, 100L);

        then(finderSessionMediaRepository).should().delete(media);
    }

    @Test
    void 미디어_삭제_시_미디어_없으면_FINDER_SESSION_NOT_FOUND() {
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));
        given(finderSessionMediaRepository.findByIdAndSession_Id(999L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> finderService.deleteMedia(finderId, 10L, 999L))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_SESSION_NOT_FOUND));
    }

    // ───────── submitSession ─────────

    @Test
    void 세션_제출_FOUND_성공() {
        FinderSessionSubmitRequestDto dto = new FinderSessionSubmitRequestDto(FinderResultType.FOUND, "강아지 발견!");
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        finderService.submitSession(finderId, 10L, dto);

        assertThat(activeSession.getStatusCode()).isEqualTo(FinderSessionStatus.SUBMITTED);
        assertThat(activeSession.getResultCode()).isEqualTo(FinderResultType.FOUND);
        assertThat(activeSession.getNote()).isEqualTo("강아지 발견!");
        assertThat(activeSession.getSubmittedAt()).isNotNull();
        then(notificationRepository).should().save(any());
    }

    @Test
    void 세션_제출_NOT_FOUND_성공() {
        FinderSessionSubmitRequestDto dto = new FinderSessionSubmitRequestDto(FinderResultType.NOT_FOUND, "찾지 못했습니다");
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        finderService.submitSession(finderId, 10L, dto);

        assertThat(activeSession.getResultCode()).isEqualTo(FinderResultType.NOT_FOUND);
    }

    @Test
    void 이미_제출된_세션에_재제출하면_FINDER_SESSION_ALREADY_SUBMITTED() {
        activeSession.submit(FinderResultType.FOUND, "첫 번째 제출");
        FinderSessionSubmitRequestDto dto = new FinderSessionSubmitRequestDto(FinderResultType.NOT_FOUND, "재제출 시도");
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> finderService.submitSession(finderId, 10L, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_SESSION_ALREADY_SUBMITTED));
    }

    // ───────── cancelSession ─────────

    @Test
    void 세션_취소_성공() {
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        finderService.cancelSession(finderId, 10L);

        assertThat(activeSession.getStatusCode()).isEqualTo(FinderSessionStatus.CANCELLED);
    }

    @Test
    void 이미_제출된_세션을_취소하면_FINDER_SESSION_ALREADY_SUBMITTED() {
        activeSession.submit(FinderResultType.FOUND, "제출 완료");
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> finderService.cancelSession(finderId, 10L))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_SESSION_ALREADY_SUBMITTED));
    }

    // ───────── recordRoute ─────────

    @Test
    void 경로_기록_성공() {
        var points = List.of(
                new FinderRouteRecordRequestDto.RoutePoint(37.5665, 126.9780, LocalDateTime.of(2026, 7, 5, 10, 0, 0)),
                new FinderRouteRecordRequestDto.RoutePoint(37.5670, 126.9785, LocalDateTime.of(2026, 7, 5, 10, 0, 10)),
                new FinderRouteRecordRequestDto.RoutePoint(37.5675, 126.9790, LocalDateTime.of(2026, 7, 5, 10, 0, 20))
        );
        FinderRouteRecordRequestDto dto = new FinderRouteRecordRequestDto(points);
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        finderService.recordRoute(finderId, 10L, dto);

        assertThat(activeSession.getRoute()).hasSize(3);
        assertThat(activeSession.getRoute().get(0).latitude()).isEqualTo(37.5665);
    }

    @Test
    void 경로_기록_배치_누적_성공() {
        // 첫 번째 배치 저장
        var first = List.of(
                new FinderRouteRecordRequestDto.RoutePoint(37.5665, 126.9780, LocalDateTime.of(2026, 7, 5, 10, 0, 0)),
                new FinderRouteRecordRequestDto.RoutePoint(37.5670, 126.9785, LocalDateTime.of(2026, 7, 5, 10, 0, 5))
        );
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));
        finderService.recordRoute(finderId, 10L, new FinderRouteRecordRequestDto(first));

        // 두 번째 배치 저장
        var second = List.of(
                new FinderRouteRecordRequestDto.RoutePoint(37.5675, 126.9790, LocalDateTime.of(2026, 7, 5, 10, 0, 10))
        );
        finderService.recordRoute(finderId, 10L, new FinderRouteRecordRequestDto(second));

        assertThat(activeSession.getRoute()).hasSize(3);
    }

    @Test
    void 경로_기록_시_ACTIVE가_아닌_세션이면_FINDER_SESSION_ALREADY_SUBMITTED() {
        activeSession.submit(FinderResultType.FOUND, "제출 완료");
        var dto = new FinderRouteRecordRequestDto(List.of(
                new FinderRouteRecordRequestDto.RoutePoint(37.5665, 126.9780, LocalDateTime.now())
        ));
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> finderService.recordRoute(finderId, 10L, dto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.FINDER_SESSION_ALREADY_SUBMITTED));
    }

    // ───────── getRoute ─────────

    @Test
    void 경로_조회_성공() {
        var points = List.of(
                new FinderRouteRecordRequestDto.RoutePoint(37.5665, 126.9780, LocalDateTime.of(2026, 7, 5, 10, 0, 0)),
                new FinderRouteRecordRequestDto.RoutePoint(37.5670, 126.9785, LocalDateTime.of(2026, 7, 5, 10, 0, 10))
        );
        activeSession.appendRoute(points);
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        var result = finderService.getRoute(finderId, 10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).latitude()).isEqualTo(37.5665);
        assertThat(result.get(1).latitude()).isEqualTo(37.5670);
    }

    @Test
    void 경로_조회_경로_없으면_빈_리스트() {
        given(finderSessionRepository.findById(10L)).willReturn(Optional.of(activeSession));

        var result = finderService.getRoute(finderId, 10L);

        assertThat(result).isEmpty();
    }

    // ───────── getSessionsByPost ─────────

    @Test
    void 게시글별_세션_목록_조회_성공() {
        var pageable = PageRequest.of(0, 10);
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));
        given(finderSessionRepository.findByPostIdWithFinder(1L, pageable))
                .willReturn(new SliceImpl<>(List.of(activeSession), pageable, false));

        var result = finderService.getSessionsByPost(postOwnerId, 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void 게시글별_세션_조회_시_게시글_없으면_POST_NOT_FOUND() {
        var pageable = PageRequest.of(0, 10);
        given(lostPostRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> finderService.getSessionsByPost(postOwnerId, 999L, pageable))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
    }

    @Test
    void 게시글별_세션_조회_시_작성자_아니면_POST_ACCESS_DENIED() {
        var pageable = PageRequest.of(0, 10);
        given(lostPostRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> finderService.getSessionsByPost(finderId, 1L, pageable))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.POST_ACCESS_DENIED));
        then(finderSessionRepository).should(never()).findByPostIdWithFinder(any(), any());
    }
}
