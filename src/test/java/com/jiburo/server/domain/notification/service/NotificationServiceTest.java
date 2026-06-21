package com.jiburo.server.domain.notification.service;

import com.jiburo.server.domain.notification.domain.Notification;
import com.jiburo.server.domain.notification.dto.NotificationResponseDto;
import com.jiburo.server.domain.notification.repository.NotificationRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks private NotificationServiceImpl notificationService;

    @Mock private NotificationRepository notificationRepository;

    private UUID userId;
    private User receiver;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);

        userId = UUID.randomUUID();
        receiver = User.builder()
                .oauthId("google_test")
                .nickname("테스터")
                .email("test@test.com")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(receiver, "id", userId);
    }

    // ───────── getMyNotifications ─────────

    @Test
    void 내_알림_목록_조회_성공() {
        // given
        Notification noti = Notification.builder()
                .receiver(receiver)
                .typeCode("NOTI_POST_LIKE")
                .args("닉네임,게시글제목")
                .targetId(1L)
                .build();
        ReflectionTestUtils.setField(noti, "id", 10L);

        Pageable pageable = Pageable.ofSize(20);
        Slice<Notification> slice = new SliceImpl<>(List.of(noti), pageable, false);
        given(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable))
                .willReturn(slice);

        // when
        Slice<NotificationResponseDto> result = notificationService.getMyNotifications(userId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).typeCode()).isEqualTo("NOTI_POST_LIKE");
        assertThat(result.getContent().get(0).args()).containsExactly("닉네임", "게시글제목");
        assertThat(result.getContent().get(0).isRead()).isFalse();
    }

    @Test
    void 알림_없으면_빈_슬라이스_반환() {
        // given
        Pageable pageable = Pageable.ofSize(20);
        given(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable))
                .willReturn(new SliceImpl<>(List.of(), pageable, false));

        // when
        Slice<NotificationResponseDto> result = notificationService.getMyNotifications(userId, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void args_null인_알림은_빈_리스트_반환() {
        // given
        Notification noti = Notification.builder()
                .receiver(receiver)
                .typeCode("NOTI_SYSTEM")
                .args(null)
                .targetId(null)
                .build();
        ReflectionTestUtils.setField(noti, "id", 11L);

        Pageable pageable = Pageable.ofSize(20);
        Slice<Notification> slice = new SliceImpl<>(List.of(noti), pageable, false);
        given(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable))
                .willReturn(slice);

        // when
        Slice<NotificationResponseDto> result = notificationService.getMyNotifications(userId, pageable);

        // then
        assertThat(result.getContent().get(0).args()).isEmpty();
    }

    // ───────── getUnreadCount ─────────

    @Test
    void 안읽은_알림_개수_반환() {
        // given
        given(notificationRepository.countByReceiverIdAndIsReadFalse(userId)).willReturn(5L);

        // when
        long count = notificationService.getUnreadCount(userId);

        // then
        assertThat(count).isEqualTo(5L);
    }

    @Test
    void 안읽은_알림_없으면_0_반환() {
        // given
        given(notificationRepository.countByReceiverIdAndIsReadFalse(userId)).willReturn(0L);

        // when
        long count = notificationService.getUnreadCount(userId);

        // then
        assertThat(count).isZero();
    }
}
