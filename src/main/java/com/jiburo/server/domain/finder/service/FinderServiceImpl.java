package com.jiburo.server.domain.finder.service;

import com.jiburo.server.domain.finder.domain.FinderSession;
import com.jiburo.server.domain.finder.domain.FinderSessionMedia;
import com.jiburo.server.domain.finder.domain.enums.FinderSessionStatus;
import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.domain.image.service.UploadedFileValidator;
import com.jiburo.server.domain.finder.dto.*;
import com.jiburo.server.domain.finder.repository.FinderSessionMediaRepository;
import com.jiburo.server.domain.finder.repository.FinderSessionRepository;
import com.jiburo.server.domain.notification.domain.Notification;
import com.jiburo.server.domain.notification.repository.NotificationRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinderServiceImpl implements FinderService {

    // 제보 1회 요청에 붙일 수 있는 파일 수 / 세션 1개에 쌓을 수 있는 제보(미디어·텍스트) 수 / 메모 최대 길이
    static final int MAX_FILES_PER_REQUEST = 10;
    static final int MAX_MEDIA_PER_SESSION = 100;
    static final int MAX_MEMO_LENGTH = 1000;
    // 이동 경로: 1회 기록 지점 수 / 세션 전체 지점 수 (JSON 컬럼 하나에 누적되므로 상한 필요)
    static final int MAX_ROUTE_POINTS_PER_REQUEST = 500;
    static final int MAX_ROUTE_POINTS_PER_SESSION = 10_000;

    private final FinderSessionRepository finderSessionRepository;
    private final FinderSessionMediaRepository finderSessionMediaRepository;
    private final LostPostRepository lostPostRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final UploadedFileValidator uploadedFileValidator;

    @Override
    @Transactional
    public String createSession(UUID finderId, FinderSessionCreateRequestDto dto) {
        Long postId = HashidsUtils.decode(dto.postId());
        // 삭제되었거나 볼 권한이 없는 게시글이면 존재하지 않는 것으로 처리
        LostPost post = lostPostRepository.findByIdAndDeletedFalse(postId)
                .filter(p -> p.isVisibleTo(finderId))
                .orElseThrow(() -> new JiburoException(ErrorCode.POST_NOT_FOUND));
        User finder = userRepository.findById(finderId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));

        if (post.getUser().getId().equals(finderId)) {
            throw new JiburoException(ErrorCode.FINDER_CANNOT_FIND_OWN_POST);
        }
        if (PostStatus.COMPLETE.equals(post.getStatusCode())) {
            throw new JiburoException(ErrorCode.FINDER_POST_ALREADY_COMPLETE);
        }
        if (finderSessionRepository.existsByPost_IdAndFinder_IdAndStatusCode(postId, finderId, FinderSessionStatus.ACTIVE)) {
            throw new JiburoException(ErrorCode.FINDER_SESSION_ALREADY_EXISTS);
        }

        FinderSession session = FinderSession.builder()
                .post(post)
                .finder(finder)
                .startLatitude(dto.latitude())
                .startLongitude(dto.longitude())
                .build();
        finderSessionRepository.save(session);

        notificationRepository.save(Notification.builder()
                .receiver(post.getUser())
                .sender(finder)
                .typeCode("FINDER_SESSION_STARTED")
                .args(finder.getNickname())
                .targetId(post.getId())
                .build());

        return HashidsUtils.encode(session.getId());
    }

    @Override
    public FinderSessionResponseDto getSession(UUID finderId, Long sessionId) {
        FinderSession session = finderSessionRepository.findByIdWithMedia(sessionId)
                .orElseThrow(() -> new JiburoException(ErrorCode.FINDER_SESSION_NOT_FOUND));
        validateFinder(session, finderId);
        return FinderSessionResponseDto.from(session);
    }

    @Override
    public Slice<FinderSessionListResponseDto> getMySessions(UUID finderId, Pageable pageable) {
        return finderSessionRepository.findByFinderIdWithPost(finderId, pageable)
                .map(FinderSessionListResponseDto::from);
    }

    @Override
    @Transactional
    public List<FinderMediaResponseDto> addMedia(UUID finderId, Long sessionId, FinderMediaAddRequestDto dto) {
        FinderSession session = findSessionOrThrow(sessionId);
        validateFinder(session, finderId);
        validateActive(session);

        boolean hasFiles = dto.fileKeys() != null && !dto.fileKeys().isEmpty();
        boolean hasMemo = dto.memo() != null && !dto.memo().isBlank();

        if (!hasFiles && !hasMemo) {
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (hasFiles && dto.mediaType() != UploadTargetType.FINDER_IMAGE && dto.mediaType() != UploadTargetType.FINDER_VIDEO) {
            throw new JiburoException(ErrorCode.INVALID_FILE_CODE);
        }
        if (hasMemo && dto.memo().length() > MAX_MEMO_LENGTH) {
            throw new JiburoException(ErrorCode.FINDER_MEDIA_LIMIT_EXCEEDED);
        }
        int adding = hasFiles ? dto.fileKeys().size() : 1;
        if (adding > MAX_FILES_PER_REQUEST
                || finderSessionMediaRepository.countBySession_Id(sessionId) + adding > MAX_MEDIA_PER_SESSION) {
            throw new JiburoException(ErrorCode.FINDER_MEDIA_LIMIT_EXCEEDED);
        }
        // 본인이 이 미디어 타입으로 올려 업로드를 마친 파일만 첨부 가능 (외부 URL·남의 파일 차단)
        if (hasFiles) {
            uploadedFileValidator.requireOwnedUploads(finderId, dto.fileKeys(), Set.of(dto.mediaType()));
        }

        List<FinderSessionMedia> saved;
        if (hasFiles) {
            saved = dto.fileKeys().stream()
                    .map(fileKey -> FinderSessionMedia.builder()
                            .session(session)
                            .fileKey(fileKey)
                            .mediaType(dto.mediaType())
                            .latitude(dto.latitude())
                            .longitude(dto.longitude())
                            .memo(dto.memo())
                            .build())
                    .map(finderSessionMediaRepository::save)
                    .toList();
        } else {
            FinderSessionMedia text = FinderSessionMedia.builder()
                    .session(session)
                    .memo(dto.memo())
                    .build();
            finderSessionMediaRepository.save(text);
            saved = List.of(text);
        }

        return saved.stream().map(FinderMediaResponseDto::from).toList();
    }

    @Override
    @Transactional
    public void deleteMedia(UUID finderId, Long sessionId, Long mediaId) {
        FinderSession session = findSessionOrThrow(sessionId);
        validateFinder(session, finderId);
        validateActive(session);

        FinderSessionMedia media = finderSessionMediaRepository.findByIdAndSession_Id(mediaId, sessionId)
                .orElseThrow(() -> new JiburoException(ErrorCode.FINDER_SESSION_NOT_FOUND));
        finderSessionMediaRepository.delete(media);
    }

    @Override
    @Transactional
    public void submitSession(UUID finderId, Long sessionId, FinderSessionSubmitRequestDto dto) {
        FinderSession session = findSessionOrThrow(sessionId);
        validateFinder(session, finderId);
        validateActive(session);

        session.submit(dto.resultCode(), dto.note());

        notificationRepository.save(Notification.builder()
                .receiver(session.getPost().getUser())
                .sender(session.getFinder())
                .typeCode("FINDER_SESSION_SUBMITTED")
                .args(session.getFinder().getNickname() + "," + dto.resultCode().name())
                .targetId(session.getPost().getId())
                .build());
    }

    @Override
    @Transactional
    public void cancelSession(UUID finderId, Long sessionId) {
        FinderSession session = findSessionOrThrow(sessionId);
        validateFinder(session, finderId);
        validateActive(session);
        session.cancel();
    }

    @Override
    public Slice<FinderSessionResponseDto> getSessionsByPost(UUID postOwnerId, Long postId, Pageable pageable) {
        LostPost post = lostPostRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new JiburoException(ErrorCode.POST_NOT_FOUND));
        if (!post.getUser().getId().equals(postOwnerId)) {
            throw new JiburoException(ErrorCode.POST_ACCESS_DENIED);
        }
        return finderSessionRepository.findByPostIdWithFinder(postId, pageable)
                .map(FinderSessionResponseDto::from);
    }

    @Override
    @Transactional
    public void recordRoute(UUID finderId, Long sessionId, FinderRouteRecordRequestDto dto) {
        FinderSession session = findSessionOrThrow(sessionId);
        validateFinder(session, finderId);
        validateActive(session);
        int existing = session.getRoute() == null ? 0 : session.getRoute().size();
        if (dto.points().size() > MAX_ROUTE_POINTS_PER_REQUEST
                || existing + dto.points().size() > MAX_ROUTE_POINTS_PER_SESSION) {
            throw new JiburoException(ErrorCode.FINDER_ROUTE_LIMIT_EXCEEDED);
        }
        session.appendRoute(dto.points());
    }

    @Override
    public List<FinderRoutePointResponseDto> getRoute(UUID finderId, Long sessionId) {
        FinderSession session = findSessionOrThrow(sessionId);
        validateFinder(session, finderId);
        return FinderRoutePointResponseDto.fromList(session.getRoute());
    }

    private FinderSession findSessionOrThrow(Long sessionId) {
        return finderSessionRepository.findById(sessionId)
                .orElseThrow(() -> new JiburoException(ErrorCode.FINDER_SESSION_NOT_FOUND));
    }

    private void validateFinder(FinderSession session, UUID finderId) {
        if (!session.getFinder().getId().equals(finderId)) {
            throw new JiburoException(ErrorCode.FINDER_SESSION_ACCESS_DENIED);
        }
    }

    private void validateActive(FinderSession session) {
        if (!session.isActive()) {
            throw new JiburoException(ErrorCode.FINDER_SESSION_ALREADY_SUBMITTED);
        }
    }
}
