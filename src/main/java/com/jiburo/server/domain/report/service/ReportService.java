package com.jiburo.server.domain.report.service;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.domain.chat.repository.ChatMessageRepository;
import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.report.domain.Report;
import com.jiburo.server.domain.report.domain.enums.ReportReason;
import com.jiburo.server.domain.report.domain.enums.ReportStatus;
import com.jiburo.server.domain.report.dto.ReportCreateRequestDto;
import com.jiburo.server.domain.report.repository.ReportRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 사용자 신고 접수.
 * - 자기 자신 / 자기 글·메시지는 신고 불가
 * - 같은 사용자가 같은 대상을 처리 대기 중에 다시 신고하면 ALREADY_REPORTED
 * - 채팅 메시지는 그 채팅방 참여자만 신고 가능 (남의 방 메시지 ID를 넣어 신고하는 것 차단)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final LostPostRepository lostPostRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository chatParticipantRepository;

    public String create(UUID reporterId, ReportCreateRequestDto request) {
        if (request.reason() == ReportReason.ETC && (request.detail() == null || request.detail().isBlank())) {
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));

        Report report = switch (request.targetType()) {
            case POST -> postReport(reporter, request);
            case CHAT_MESSAGE -> messageReport(reporter, request);
            case USER -> userReport(reporter, request);
        };

        if (report.getTargetUser().getId().equals(reporterId)) {
            throw new JiburoException(ErrorCode.CANNOT_REPORT_SELF);
        }
        if (reportRepository.existsByReporter_IdAndTargetKeyAndStatus(reporterId, report.getTargetKey(), ReportStatus.PENDING)) {
            throw new JiburoException(ErrorCode.ALREADY_REPORTED);
        }
        return HashidsUtils.encode(reportRepository.save(report).getId());
    }

    private Report postReport(User reporter, ReportCreateRequestDto request) {
        LostPost post = lostPostRepository.findById(HashidsUtils.decode(request.targetId()))
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new JiburoException(ErrorCode.REPORT_TARGET_NOT_FOUND));
        return base(reporter, request)
                .targetPostId(post.getId())
                .targetUser(post.getUser())
                .contentSnapshot(post.getTitle() + "\n\n" + (post.getContent() == null ? "" : post.getContent()))
                .build();
    }

    private Report messageReport(User reporter, ReportCreateRequestDto request) {
        ChatMessage message = chatMessageRepository.findById(HashidsUtils.decode(request.targetId()))
                .orElseThrow(() -> new JiburoException(ErrorCode.REPORT_TARGET_NOT_FOUND));
        if (!chatParticipantRepository.existsByChatRoomIdAndUserId(message.getChatRoom().getId(), reporter.getId())) {
            throw new JiburoException(ErrorCode.REPORT_TARGET_NOT_FOUND); // 존재 여부를 노출하지 않도록 같은 에러
        }
        String snapshot = message.getMessageTypeCode() == ChatMsgType.TALK
                ? message.getContent()
                : "[" + message.getMessageTypeCode().getCode() + "] " + message.getContent();
        return base(reporter, request)
                .targetMessageId(message.getId())
                .targetUser(message.getSender())
                .contentSnapshot(snapshot)
                .build();
    }

    private Report userReport(User reporter, ReportCreateRequestDto request) {
        UUID targetId;
        try {
            targetId = UUID.fromString(request.targetId());
        } catch (IllegalArgumentException e) {
            throw new JiburoException(ErrorCode.INVALID_IDENTIFIER);
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new JiburoException(ErrorCode.REPORT_TARGET_NOT_FOUND));
        return base(reporter, request)
                .targetUser(target)
                .contentSnapshot(target.getNickname())
                .build();
    }

    private static Report.ReportBuilder base(User reporter, ReportCreateRequestDto request) {
        return Report.builder()
                .reporter(reporter)
                .targetType(request.targetType())
                .reason(request.reason())
                .detail(request.detail() == null || request.detail().isBlank() ? null : request.detail().trim());
    }
}
