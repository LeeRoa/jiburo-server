package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminPostDtos.PostDetail;
import com.jiburo.server.domain.admin.dto.AdminPostDtos.PostSummary;
import com.jiburo.server.domain.admin.dto.AdminPostDtos.SearchCondition;
import com.jiburo.server.domain.admin.repository.AdminPostQueryRepository;
import com.jiburo.server.domain.notification.domain.Notification;
import com.jiburo.server.domain.notification.repository.NotificationRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.post.repository.PostLikeRepository;
import com.jiburo.server.domain.post.service.LostPostDeleter;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPostService {

    // 알림 typeCode — 프론트가 다국어 문구로 조립 (args: 게시글 제목)
    static final String NOTI_POST_DELETED_BY_ADMIN = "POST_DELETED_BY_ADMIN";

    private final AdminPostQueryRepository adminPostQueryRepository;
    private final LostPostRepository lostPostRepository;
    private final PostLikeRepository postLikeRepository;
    private final LostPostDeleter lostPostDeleter;
    private final NotificationRepository notificationRepository;

    public Page<PostSummary> search(SearchCondition c, Pageable pageable) {
        Page<LostPost> posts = adminPostQueryRepository.search(new AdminPostQueryRepository.Condition(
                c.keyword(), c.category(), c.status(), c.visibility(), c.deleted()), pageable);

        List<Long> ids = posts.getContent().stream().map(LostPost::getId).toList();
        Map<Long, Long> likes = ids.isEmpty() ? Map.of() : postLikeRepository.countGroupByLostPostIdIn(ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        Map<Long, Long> chats = adminPostQueryRepository.countChatRooms(ids);

        return posts.map(p -> PostSummary.of(p, likes.getOrDefault(p.getId(), 0L), chats.getOrDefault(p.getId(), 0L)));
    }

    public PostDetail find(Long postId) {
        LostPost post = findOrThrow(postId);
        return PostDetail.of(post, postLikeRepository.countByLostPost_Id(postId),
                adminPostQueryRepository.countChatRooms(List.of(postId)).getOrDefault(postId, 0L));
    }

    /**
     * 관리자 강제 삭제 — 작성자 삭제와 같은 Soft Delete 규칙 + 작성자에게 알림.
     * 사유는 @AuditLog로 감사 로그에 남는다.
     */
    @Transactional
    public void forceDelete(Long postId) {
        LostPost post = findOrThrow(postId);
        if (post.isDeleted()) {
            throw new JiburoException(ErrorCode.POST_NOT_FOUND);
        }
        lostPostDeleter.delete(post);

        notificationRepository.save(Notification.builder()
                .receiver(post.getUser())
                .typeCode(NOTI_POST_DELETED_BY_ADMIN)
                // args는 콤마 구분 저장이라 제목의 콤마는 공백으로 치환
                .args(post.getTitle().replace(",", " "))
                .targetId(post.getId())
                .build());
    }

    @Transactional
    public void restore(Long postId) {
        LostPost post = findOrThrow(postId);
        if (!post.isDeleted()) {
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }
        post.restore();
    }

    // 관리자는 삭제된 글도 조회해야 하므로 findById (findByIdAndDeletedFalse 아님)
    private LostPost findOrThrow(Long postId) {
        return lostPostRepository.findById(postId)
                .orElseThrow(() -> new JiburoException(ErrorCode.POST_NOT_FOUND));
    }
}
