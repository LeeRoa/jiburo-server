package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminReportDtos.ContentState;
import com.jiburo.server.domain.admin.dto.AdminReportDtos.ResolveRequest;
import com.jiburo.server.domain.admin.dto.AdminReportDtos.SearchCondition;
import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.ChatParticipant;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.domain.chat.repository.ChatMessageRepository;
import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.chat.repository.ChatRoomRepository;
import com.jiburo.server.domain.notification.repository.NotificationRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.AnimalType;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.domain.enums.VisibilityType;
import com.jiburo.server.domain.post.dto.detail.AnimalDetailDto;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.report.domain.enums.ReportReason;
import com.jiburo.server.domain.report.domain.enums.ReportStatus;
import com.jiburo.server.domain.report.domain.enums.ReportTargetType;
import com.jiburo.server.domain.report.dto.ReportCreateRequestDto;
import com.jiburo.server.domain.report.service.ReportService;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.domain.user.service.AuthTokenService;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminReportServiceIntegrationTest {

    @Autowired private ReportService reportService;
    @Autowired private AdminReportService adminReportService;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private LostPostRepository lostPostRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatParticipantRepository chatParticipantRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private NotificationRepository notificationRepository;
    @MockitoBean private com.jiburo.server.domain.user.dao.AuthTokenRedisRepository authTokenRedisRepository;

    private User admin;
    private User writer;
    private User reporterA;
    private User reporterB;
    private LostPost post;

    @BeforeEach
    void setUp() {
        admin = user("admin", RoleType.ADMIN);
        writer = user("writer", RoleType.USER);
        reporterA = user("a", RoleType.USER);
        reporterB = user("b", RoleType.USER);
        post = lostPostRepository.save(LostPost.builder().user(writer).categoryCode(CategoryType.ANIMAL).title("광고 글")
                .content("싸게 팝니다").statusCode(PostStatus.LOST)
                .detail(AnimalDetailDto.builder().animalTypeCode(AnimalType.DOG).build())
                .latitude(37.5).longitude(127.0).foundLocation("서울").lostDate(LocalDate.now()).reward(0)
                .visibilityCode(VisibilityType.PUBLIC).build());
    }

    private User user(String name, RoleType role) {
        return userRepository.save(User.builder().oauthId("kakao_" + name).nickname(name).roleCode(role).build());
    }

    private String reportPost(User reporter) {
        return reportService.create(reporter.getId(), new ReportCreateRequestDto(
                ReportTargetType.POST, HashidsUtils.encode(post.getId()), ReportReason.SPAM, null));
    }

    @Test
    void 같은_대상은_한_번_처리로_모두_처리되고_신고자에게_알림이_간다() {
        String r1 = reportPost(reporterA);
        reportPost(reporterB);

        var summary = adminReportService.search(new SearchCondition(ReportStatus.PENDING, null, null, null), PageRequest.of(0, 20));
        assertThat(summary.getContent()).hasSize(2);
        assertThat(summary.getContent().get(0).pendingCountForTarget()).isEqualTo(2);
        assertThat(summary.getContent().get(0).contentSnapshot()).contains("광고 글");

        var result = adminReportService.resolve(admin.getId(), HashidsUtils.decode(r1),
                new ResolveRequest(true, 7, false, "광고"));

        assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(result.handledCount()).isEqualTo(2);
        assertThat(post.isDeleted()).isTrue();
        assertThat(writer.isSuspended()).isTrue();
        assertThat(writer.getSuspendedUntil()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(notificationRepository.findAll()).extracting("typeCode")
                .contains("REPORT_HANDLED", "POST_DELETED_BY_ADMIN");

        var detail = adminReportService.find(HashidsUtils.decode(r1));
        assertThat(detail.contentState()).isEqualTo(ContentState.DELETED);
        assertThat(detail.handledByNickname()).isEqualTo("admin");
        assertThatThrownBy(() -> adminReportService.resolve(admin.getId(), HashidsUtils.decode(r1),
                new ResolveRequest(false, null, false, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_ALREADY_HANDLED);
    }

    @Test
    void 조치가_없으면_기각되고_콘텐츠는_그대로다() {
        String r1 = reportPost(reporterA);

        var result = adminReportService.resolve(admin.getId(), HashidsUtils.decode(r1), new ResolveRequest(false, null, false, null));

        assertThat(result.status()).isEqualTo(ReportStatus.DISMISSED);
        assertThat(post.isDeleted()).isFalse();
        assertThat(writer.isSuspended()).isFalse();
    }

    @Test
    void 중복_신고와_자기_신고는_막는다() {
        reportPost(reporterA);
        assertThatThrownBy(() -> reportPost(reporterA)).extracting("errorCode").isEqualTo(ErrorCode.ALREADY_REPORTED);
        assertThatThrownBy(() -> reportPost(writer)).extracting("errorCode").isEqualTo(ErrorCode.CANNOT_REPORT_SELF);
        assertThatThrownBy(() -> reportService.create(reporterA.getId(), new ReportCreateRequestDto(
                ReportTargetType.USER, writer.getId().toString(), ReportReason.ETC, " ")))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 채팅_메시지_신고는_참여자만_가능하고_삭제하면_가려진다() {
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().post(post).host(reporterA).build());
        room.addParticipant(chatParticipantRepository.save(ChatParticipant.builder().chatRoom(room).user(reporterA).build()));
        room.addParticipant(chatParticipantRepository.save(ChatParticipant.builder().chatRoom(room).user(writer).build()));
        chatMessageRepository.save(ChatMessage.builder().chatRoom(room).sender(reporterA).content("안녕하세요").messageTypeCode(ChatMsgType.TALK).build());
        ChatMessage bad = chatMessageRepository.save(ChatMessage.builder().chatRoom(room).sender(writer).content("나쁜 말").messageTypeCode(ChatMsgType.TALK).build());
        String messageId = HashidsUtils.encode(bad.getId());

        assertThatThrownBy(() -> reportService.create(reporterB.getId(), new ReportCreateRequestDto(
                ReportTargetType.CHAT_MESSAGE, messageId, ReportReason.ABUSE, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);

        String reportId = reportService.create(reporterA.getId(), new ReportCreateRequestDto(
                ReportTargetType.CHAT_MESSAGE, messageId, ReportReason.ABUSE, "욕설"));

        var detail = adminReportService.find(HashidsUtils.decode(reportId));
        assertThat(detail.chatContext()).extracting("content").containsExactly("안녕하세요", "나쁜 말");
        assertThat(detail.chatContext()).extracting("reported").containsExactly(false, true);

        adminReportService.resolve(admin.getId(), HashidsUtils.decode(reportId), new ResolveRequest(true, null, false, null));

        assertThat(bad.isDeletedByAdmin()).isTrue();
        assertThat(bad.getContent()).doesNotContain("나쁜 말");
        assertThat(adminReportService.find(HashidsUtils.decode(reportId)).summary().contentSnapshot()).isEqualTo("나쁜 말");
    }

    @Test
    void 영구_정지된_계정은_토큰을_받을_수_없고_관리자는_정지할_수_없다() {
        String reportId = reportService.create(reporterA.getId(), new ReportCreateRequestDto(
                ReportTargetType.USER, writer.getId().toString(), ReportReason.ABUSE, null));

        var result = adminReportService.resolve(admin.getId(), HashidsUtils.decode(reportId), new ResolveRequest(false, null, true, "반복 욕설"));

        assertThat(result.suspendedUntil()).isEqualTo(User.PERMANENT_SUSPENSION);
        assertThat(writer.isPermanentlySuspended()).isTrue();
        assertThat(writer.getSuspendReason()).isEqualTo("반복 욕설");
        assertThatThrownBy(() -> authTokenService.issueTokens(writer))
                .extracting("errorCode").isEqualTo(ErrorCode.USER_SUSPENDED);

        String adminReport = reportService.create(reporterA.getId(), new ReportCreateRequestDto(
                ReportTargetType.USER, admin.getId().toString(), ReportReason.ABUSE, null));
        assertThatThrownBy(() -> adminReportService.resolve(reporterB.getId(),
                HashidsUtils.decode(adminReport), new ResolveRequest(false, 3, false, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
    }
}
