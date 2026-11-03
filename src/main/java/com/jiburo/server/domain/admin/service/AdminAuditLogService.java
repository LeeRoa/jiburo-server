package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminAuditLogDtos.LogResponse;
import com.jiburo.server.domain.admin.dto.AdminAuditLogDtos.SearchCondition;
import com.jiburo.server.domain.admin.repository.AdminAuditLogQueryRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.log.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuditLogService {

    private final AdminAuditLogQueryRepository adminAuditLogQueryRepository;
    private final UserRepository userRepository;

    public Page<LogResponse> search(SearchCondition c, Pageable pageable) {
        if (c.from() != null && c.to() != null && c.from().isAfter(c.to())) {
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Page<AuditLog> logs = adminAuditLogQueryRepository.search(
                new AdminAuditLogQueryRepository.Condition(c.userId(), c.action(), c.actionPrefix(), c.from(), c.to()), pageable);

        // audit_logs는 users와 연관관계가 없으므로(이력 보존) 닉네임은 페이지 단위로 한 번에 조회
        Set<UUID> userIds = logs.getContent().stream().map(AuditLog::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> nicknames = userIds.isEmpty() ? Map.of()
                : userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, User::getNickname));

        return logs.map(log -> LogResponse.of(log, nicknames.get(log.getUserId())));
    }
}
