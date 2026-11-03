package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.finder.repository.FinderSessionRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.PostLike;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.dto.*;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.post.repository.PostLikeRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostPostServiceImpl implements LostPostService {

    private final LostPostRepository lostPostRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final FinderSessionRepository finderSessionRepository;

    @Override
    @Transactional
    public Long create(UUID userId, LostPostCreateRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));

        // 2. 엔티티 생성
        LostPost post = requestDto.toEntity(user);
        post.updateImages(requestDto.imageUrls());

        return lostPostRepository.save(post).getId();
    }

    @Override
    public LostPostResponseDto findById(Long id, UUID userId) {
        LostPost post = findVisiblePostOrThrow(id, userId);
        long likeCount = postLikeRepository.countByLostPost_Id(post.getId());
        boolean isLiked = userId != null && postLikeRepository.existsByUser_IdAndLostPost_Id(userId, post.getId());
        return LostPostResponseDto.from(post, isLiked, likeCount);
    }

    @Override
    @Transactional
    public void update(UUID userId, Long postId, LostPostUpdateRequestDto requestDto) {
        LostPost post = findPostByIdOrThrow(postId);
        validateWriter(post, userId);

        // 엔티티 업데이트 호출 (categoryCode가 없으면 상세정보도 변경하지 않음)
        post.update(
                requestDto.title(),
                requestDto.content(),
                requestDto.categoryCode(),
                requestDto.categoryCode() != null ? requestDto.toDetail() : null,
                requestDto.latitude(),
                requestDto.longitude(),
                requestDto.foundLocation(),
                requestDto.lostDate(),
                requestDto.reward()
        );

        // imageUrls가 null이면 이미지 유지, 명시적으로 보내면(빈 배열 포함) 갱신
        if (requestDto.imageUrls() != null) {
            post.updateImages(requestDto.imageUrls());
        }
    }

    // TODO 분실 대상을 찾았을 경우 찾은 대상에게 뱃지를 줘야함.
    @Override
    @Transactional
    public void updateStatus(UUID userId, Long postId, LostPostStatusUpdateRequestDto requestDto) {
        LostPost post = findPostByIdOrThrow(postId);
        validateWriter(post, userId); // 작성자 확인

        PostStatus newStatus = requestDto.statusCode();

        // 1. [완료 처리 로직] 상태가 COMPLETE로 변경되는 경우
        if (PostStatus.COMPLETE.equals(newStatus)) {
            User finder = null;

            // 1-1. 찾아준 사람이 우리 앱 회원인가? (ID가 넘어왔는가?)
            if (requestDto.finderId() != null) {
                // 본인이 본인을 지정할 수도 있고(자력 해결), 다른 사람을 지정할 수도 있음
                finder = userRepository.findById(requestDto.finderId())
                        .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));
            }

            // 1-2. 엔티티 업데이트 (상태 + 해결사 정보)
            post.complete(finder, requestDto.resultNote());

            // TODO [보상 로직] 다른 회원이 찾아줬다면 뱃지/경험치 지급!
            if (finder != null && !finder.getId().equals(userId)) {
                // 작성자(userId)가 아닌 다른 사람(finder)이 찾았을 때만 보상
                // badgeService.giveReward(finder, "GOOD_NEIGHBOR");
            }

        } else {
            // 2. [일반 상태 변경] (LOST <-> PROTECTING)
            // 해결사 정보는 초기화하거나 유지 (정책에 따라 다름, 여기선 단순 상태 변경)
            post.changeStatus(newStatus);
        }
    }

    @Override
    @Transactional
    public void delete(UUID userId, Long postId) {
        LostPost post = findPostByIdOrThrow(postId);
        validateWriter(post, userId);

        // Soft Delete: 채팅방/탐색 세션 기록은 보존하고, 목록·상세·지도 조회에서만 제외한다.
        post.softDelete();
        // 좋아요는 의미가 없어지므로 정리, 진행 중인 탐색 세션은 취소 처리
        postLikeRepository.deleteAllByLostPostId(postId);
        finderSessionRepository.cancelActiveSessionsByPostId(postId);
    }

    @Override
    public Page<LostPostResponseDto> search(LostPostSearchCondition condition, Pageable pageable, UUID userId) {
        Page<LostPost> posts = lostPostRepository.search(condition, pageable, userId);
        if (posts.isEmpty()) {
            return posts.map(p -> LostPostResponseDto.from(p, false, 0L));
        }

        List<Long> postIds = posts.stream().map(LostPost::getId).toList();
        Map<Long, Long> likeCountMap = buildLikeCountMap(postIds);
        Set<Long> likedPostIds = userId != null
                ? postLikeRepository.findLikedPostIdsByUser_IdAndPostIds(userId, postIds)
                : Set.of();

        return posts.map(post -> LostPostResponseDto.from(
                post,
                likedPostIds.contains(post.getId()),
                likeCountMap.getOrDefault(post.getId(), 0L)
        ));
    }

    // 지도 렌더링용 (마커 뿌리기)
    @Override
    public List<LostPostResponseDto> getPostsForMap(LostPostMapRequestDto request, UUID viewerId) {
        return lostPostRepository.searchByViewport(request, viewerId).stream()
                .map(LostPostResponseDto::from)
                .toList();
    }

    // 리스트 렌더링용 (클릭 시 상세)
    @Override
    public Slice<LostPostResponseDto> getPostsForList(LostPostNearbyRequestDto request, UUID viewerId) {
        // 중심 좌표가 없으면 박스 계산에서 NPE(500)가 나므로 400으로 거부
        if (request.centerLat() == null || request.centerLng() == null || request.radius() <= 0) {
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Slice<LostPost> postSlice = lostPostRepository.searchByRadius(request, viewerId);
        return postSlice.map(LostPostResponseDto::from);
    }

    @Override
    @Transactional
    public boolean toggleLike(UUID userId, Long postId) {
        LostPost post = findVisiblePostOrThrow(postId, userId);
        return postLikeRepository.findByUser_IdAndLostPost_Id(userId, postId)
                .map(like -> {
                    postLikeRepository.delete(like);
                    return false;
                })
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));
                    postLikeRepository.save(PostLike.builder().user(user).lostPost(post).build());
                    return true;
                });
    }

    @Override
    public Page<LostPostResponseDto> getLikedPosts(UUID userId, Pageable pageable) {
        Page<PostLike> likes = postLikeRepository.findByUser_IdWithPost(userId, pageable);
        if (likes.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> postIds = likes.stream().map(pl -> pl.getLostPost().getId()).toList();
        Map<Long, Long> likeCountMap = buildLikeCountMap(postIds);

        return likes.map(pl -> LostPostResponseDto.from(
                pl.getLostPost(),
                true,
                likeCountMap.getOrDefault(pl.getLostPost().getId(), 0L)
        ));
    }

    private Map<Long, Long> buildLikeCountMap(List<Long> postIds) {
        return postLikeRepository.countGroupByLostPostIdIn(postIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    // 삭제되지 않은 게시글 조회 (작성자 전용 작업: 수정/상태변경/삭제)
    private LostPost findPostByIdOrThrow(Long id) {
        return lostPostRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new JiburoException(ErrorCode.POST_NOT_FOUND));
    }

    // 삭제되지 않았고 viewer가 볼 수 있는 게시글 조회.
    // 권한이 없으면 존재 자체를 숨기기 위해 403 대신 POST_NOT_FOUND(404)로 응답한다.
    private LostPost findVisiblePostOrThrow(Long id, UUID viewerId) {
        return lostPostRepository.findByIdAndDeletedFalse(id)
                .filter(post -> post.isVisibleTo(viewerId))
                .orElseThrow(() -> new JiburoException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateWriter(LostPost post, UUID userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new JiburoException(ErrorCode.POST_ACCESS_DENIED);
        }
    }
}
