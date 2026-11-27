package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminReportDtos.*;
import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.repository.ChatMessageRepository;
import com.jiburo.server.domain.notification.domain.Notification;
import com.jiburo.server.domain.notification.repository.NotificationRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.report.domain.Report;
import com.jiburo.server.domain.report.domain.enums.ReportStatus;
import com.jiburo.server.domain.report.domain.enums.ReportTargetType;
import com.jiburo.server.domain.report.repository.ReportRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.service.UserSuspensionService;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 관리자 신고 처리.
 * - 같은 대상(target_key)의 처리 대기 신고는 한 번에 같이 처리한다 (같은 글을 10명이 신고하면 1번 처리).
 * - 조치: 콘텐츠 삭제(게시글 강제 삭제 / 채팅 메시지 가리기), 신고당한 사용자 정지(기간·영구). 조치가 없으면 기각.
 * - 처리되면 각 신고자에게 알림(REPORT_HANDLED, args = 결과 상태)을 보낸다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    static final String NOTI_REPORT_HANDLED = "REPORT_HANDLED";
    private static final int CONTEXT_SIZE = 5;

    private final ReportRepository reportRepository;
    private final LostPostRepository lostPostRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AdminPostService adminPostService;
    private final UserSuspensionService userSuspensionService;

    public Page<ReportSummary> search(SearchCondition c, Pageable pageable) {
        Page<Report> page = reportRepository.search(c.status(), c.targetType(), c.reason(), c.targetUserId(), pageable);
        Set<String> keys = page.getContent().stream().map(Report::getTargetKey).collect(Collectors.toSet());
        Map<String, Long> pending = keys.isEmpty() ? Map.of()
                : reportRepository.countByTargetKeys(ReportStatus.PENDING, keys).stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));
        return page.map(r -> ReportSummary.of(r, pending.getOrDefault(r.getTargetKey(), 0L)));
    }

    public ReportDetail find(Long reportId) {
        Report report = findOrThrow(reportId);
        long pending = reportRepository.findAllByTargetKeyAndStatus(report.getTargetKey(), ReportStatus.PENDING).size();
        User target = report.getTargetUser();

        ContentState state = ContentState.NONE;
        List<ContextMessage> context = List.of();
        if (report.getTargetType() == ReportTargetType.POST) {
            state = lostPostRepository.findById(report.getTargetPostId())
                    .filter(p -> !p.isDeleted()).map(p -> ContentState.EXISTS).orElse(ContentState.DELETED);
        } else if (report.getTargetType() == ReportTargetType.CHAT_MESSAGE) {
            Optional<ChatMessage> message = chatMessageRepository.findById(report.getTargetMessageId());
            state = message.filter(m -> !m.isDeletedByAdmin()).map(m -> ContentState.EXISTS).orElse(ContentState.DELETED);
            context = message.map(this::contextOf).orElse(List.of());
        }

        String handledByNickname = report.getHandledBy() == null ? null
                : userRepository.findById(report.getHandledBy()).map(User::getNickname).orElse(null);

        return new ReportDetail(ReportSummary.of(report, pending), state, context,
                reportRepository.countByTargetUser_Id(target.getId()), target.isSuspended() ? target.getSuspendedUntil() : null,
                report.getHandledBy(), handledByNickname, report.isContentDeleted(), report.getSuspendedUntil(),
                report.getAdminNote());
    }

    @Transactional
    public ResolveResult resolve(UUID adminId, Long reportId, ResolveRequest request) {
        Report report = findOrThrow(reportId);
        if (!report.isPending()) {
            throw new JiburoException(ErrorCode.REPORT_ALREADY_HANDLED);
        }
        if (request.deleteContent() && report.getTargetType() == ReportTargetType.USER) {
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.deleteContent()) {
            deleteContent(report);
        }
        LocalDateTime suspendedUntil = null;
        if (request.suspend()) {
            Integer days = request.suspendPermanent() ? null : request.suspendDays();
            suspendedUntil = userSuspensionService.suspend(adminId, report.getTargetUser().getId(), days,
                    request.note() == null || request.note().isBlank() ? "신고 처리" : request.note().trim());
        }

        // 같은 대상의 처리 대기 신고를 모두 같은 결과로 처리하고 신고자에게 알린다
        List<Report> sameTarget = reportRepository.findAllByTargetKeyAndStatus(report.getTargetKey(), ReportStatus.PENDING);
        for (Report r : sameTarget) {
            r.handle(adminId, request.deleteContent(), suspendedUntil, request.note());
            notificationRepository.save(Notification.builder()
                    .receiver(r.getReporter())
                    .typeCode(NOTI_REPORT_HANDLED)
                    .args(r.getStatus().getCode())
                    .targetId(r.getId())
                    .build());
        }
        return new ResolveResult(report.getStatus(), sameTarget.size(), suspendedUntil);
    }

    private void deleteContent(Report report) {
        if (report.getTargetType() == ReportTargetType.POST) {
            LostPost post = lostPostRepository.findById(report.getTargetPostId())
                    .orElseThrow(() -> new JiburoException(ErrorCode.REPORT_TARGET_NOT_FOUND));
            if (!post.isDeleted()) {
                adminPostService.forceDelete(post.getId()); // 작성자 삭제와 같은 Soft Delete + 작성자 알림
            }
        } else {
            chatMessageRepository.findById(report.getTargetMessageId())
                    .ifPresent(ChatMessage::deleteByAdmin);
        }
    }

    private List<ContextMessage> contextOf(ChatMessage message) {
        Long roomId = message.getChatRoom().getId();
        List<ChatMessage> before = new ArrayList<>(
                chatMessageRepository.findTop5ByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, message.getId()));
        Collections.reverse(before);
        List<ChatMessage> all = new ArrayList<>(before.subList(Math.max(0, before.size() - CONTEXT_SIZE), before.size()));
        all.add(message);
        all.addAll(chatMessageRepository.findTop5ByChatRoomIdAndIdGreaterThanOrderByIdAsc(roomId, message.getId()));
        return all.stream().map(m -> ContextMessage.of(m, message.getId())).toList();
    }

    private Report findOrThrow(Long reportId) {
        return reportRepository.findWithUsersById(reportId)
                .orElseThrow(() -> new JiburoException(ErrorCode.REPORT_NOT_FOUND));
    }
}
