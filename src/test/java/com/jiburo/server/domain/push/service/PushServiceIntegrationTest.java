package com.jiburo.server.domain.push.service;

import com.jiburo.server.domain.notification.domain.Notification;
import com.jiburo.server.domain.notification.repository.NotificationRepository;
import com.jiburo.server.domain.push.domain.DevicePlatform;
import com.jiburo.server.domain.push.repository.PushDeviceRepository;
import com.jiburo.server.domain.push.service.PushSender.PushMessage;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/** 커밋 후(AFTER_COMMIT) 비동기 발송을 확인해야 해서 테스트 트랜잭션 없이 실행하고 직접 정리한다 */
@SpringBootTest
@ActiveProfiles("test")
class PushServiceIntegrationTest {

    @Autowired private PushService pushService;
    @Autowired private PushDeviceRepository pushDeviceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private TransactionTemplate tx;
    @MockitoBean private PushSender pushSender;

    private User a;
    private User b;

    @BeforeEach
    void setUp() {
        a = userRepository.save(User.builder().oauthId("kakao_push_a").nickname("가").roleCode(RoleType.USER).build());
        b = userRepository.save(User.builder().oauthId("kakao_push_b").nickname("나").roleCode(RoleType.USER).build());
        given(pushSender.send(anyList(), any())).willReturn(List.of());
    }

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll();
        pushDeviceRepository.deleteAll();
        userRepository.deleteAll(List.of(a, b));
    }

    @Test
    void 같은_토큰을_다른_계정으로_등록하면_옮겨지고_해제는_본인_것만() {
        pushService.register(a.getId(), "token-1", DevicePlatform.ANDROID, Locale.KOREAN);
        pushService.register(b.getId(), "token-1", DevicePlatform.IOS, Locale.ENGLISH);

        assertThat(pushService.deviceCount(a.getId())).isZero();
        assertThat(pushService.deviceCount(b.getId())).isEqualTo(1);
        assertThat(pushDeviceRepository.findByToken("token-1").orElseThrow().getLocale()).isEqualTo("en");

        pushService.unregister(a.getId(), "token-1");
        assertThat(pushService.deviceCount(b.getId())).isEqualTo(1);
        pushService.unregister(b.getId(), "token-1");
        assertThat(pushService.deviceCount(b.getId())).isZero();
    }

    @Test
    void 만료된_토큰은_발송_후_삭제된다() {
        pushService.register(a.getId(), "alive", DevicePlatform.ANDROID, Locale.KOREAN);
        pushService.register(a.getId(), "dead", DevicePlatform.ANDROID, Locale.KOREAN);
        given(pushSender.send(anyList(), any())).willReturn(List.of("dead"));

        pushService.sendToUsers(List.of(a.getId()), locale -> new PushMessage("t", "b", java.util.Map.of()));

        assertThat(pushDeviceRepository.findByToken("dead")).isEmpty();
        assertThat(pushDeviceRepository.findByToken("alive")).isPresent();
    }

    @Test
    void 알림이_저장되면_커밋_후_기기_언어로_푸시가_나간다() {
        pushService.register(a.getId(), "ko-device", DevicePlatform.ANDROID, Locale.KOREAN);

        tx.executeWithoutResult(s -> notificationRepository.save(Notification.builder()
                .receiver(userRepository.findById(a.getId()).orElseThrow())
                .typeCode("POST_DELETED_BY_ADMIN").args("우리 강아지").targetId(1L).build()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<PushMessage> message = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushSender, timeout(5000)).send(tokens.capture(), message.capture());
        assertThat(tokens.getValue()).containsExactly("ko-device");
        assertThat(message.getValue().title()).isEqualTo("게시글이 삭제되었습니다");
        assertThat(message.getValue().body()).contains("우리 강아지");
        assertThat(message.getValue().data()).containsEntry("type", "NOTIFICATION").containsKey("targetId");
    }
}
