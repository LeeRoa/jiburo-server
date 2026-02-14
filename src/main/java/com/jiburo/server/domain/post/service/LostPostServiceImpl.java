package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.*;
import com.jiburo.server.domain.post.dto.detail.AnimalDetailDto;
import com.jiburo.server.domain.post.dto.detail.TargetDetailDto;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.domain.CodeConst;
import com.jiburo.server.global.error.BusinessException;
import com.jiburo.server.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostPostServiceImpl implements LostPostService {

    private final LostPostRepository lostPostRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Long create(UUID userId, LostPostCreateRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 엔티티 생성
        LostPost post = requestDto.toEntity(user);
        post.updateImages(requestDto.imageUrls());

        return lostPostRepository.save(post).getId();
    }

    @Override
    public LostPostResponseDto findById(Long id) {
        LostPost post = findPostByIdOrThrow(id);
        return LostPostResponseDto.from(post);
    }

    @Override
    @Transactional
    public void update(UUID userId, Long postId, LostPostUpdateRequestDto requestDto) {
        LostPost post = findPostByIdOrThrow(postId);
        validateWriter(post, userId);

        // 1. 수정할 상세 정보 객체 생성
        // (수정 시 카테고리가 바뀔 수도 있으므로, DTO의 categoryCode를 확인해야 함)
        // 여기서는 일단 ANIMAL로 가정하고 업데이트
        TargetDetailDto detail = AnimalDetailDto.builder()
                .animalType(requestDto.animalTypeCode())
                .breed(requestDto.breed())
                .gender(requestDto.genderCode())
                .color(requestDto.color())
                .age(requestDto.age())
                .build();

        // 2. 엔티티 업데이트 호출
        post.update(
                requestDto.title(),
                requestDto.content(),
                requestDto.categoryCode(),
                requestDto.toDetail(),
                requestDto.latitude(),
                requestDto.longitude(),
                requestDto.foundLocation(),
                requestDto.lostDate(),
                requestDto.reward()
        );
    }

    // TODO 분실 대상을 찾았을 경우 찾은 대상에게 뱃지를 줘야함.
    @Override
    @Transactional
    public void updateStatus(UUID userId, Long postId, LostPostStatusUpdateRequestDto requestDto) {
        LostPost post = findPostByIdOrThrow(postId);
        validateWriter(post, userId); // 작성자 확인

        String newStatus = requestDto.statusCode();

        // 1. [완료 처리 로직] 상태가 COMPLETE로 변경되는 경우
        if (CodeConst.Status.COMPLETE.equals(newStatus)) {
            User finder = null;

            // 1-1. 찾아준 사람이 우리 앱 회원인가? (ID가 넘어왔는가?)
            if (requestDto.finderId() != null) {
                // 본인이 본인을 지정할 수도 있고(자력 해결), 다른 사람을 지정할 수도 있음
                finder = userRepository.findById(requestDto.finderId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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

        lostPostRepository.delete(post);
    }

    @Override
    public Page<LostPostResponseDto> search(LostPostSearchCondition condition, Pageable pageable) {
        return lostPostRepository.search(condition, pageable)
                .map(LostPostResponseDto::from);
    }

    // 지도 렌더링용 (마커 뿌리기)
    @Override
    public List<LostPostResponseDto> getPostsForMap(LostPostMapRequestDto request) {
        return lostPostRepository.searchByViewport(request).stream()
                .map(LostPostResponseDto::from)
                .toList();
    }

    // 리스트 렌더링용 (클릭 시 상세)
    @Override
    public Slice<LostPostResponseDto> getPostsForList(LostPostNearbyRequestDto request) {
        Slice<LostPost> postSlice = lostPostRepository.searchByRadius(request);
        return postSlice.map(LostPostResponseDto::from);
    }

    private LostPost findPostByIdOrThrow(Long id) {
        return lostPostRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateWriter(LostPost post, UUID userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }
    }
}
