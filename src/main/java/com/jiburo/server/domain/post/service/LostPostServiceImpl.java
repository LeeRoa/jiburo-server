package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.*;
import com.jiburo.server.domain.post.dto.detail.AnimalDetailDto; // [추가]
import com.jiburo.server.domain.post.dto.detail.TargetDetailDto; // [추가]
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

        // 1. 상세 정보 객체 생성 (JSON 변환용)
        // 현재는 '동물'만 처리하지만, 나중에 categoryCode에 따라 분기(Switch) 가능
        TargetDetailDto detail = AnimalDetailDto.builder()
                .animalType(requestDto.animalTypeCode())
                .breed(requestDto.breed())
                .gender(requestDto.genderCode())
                .color(requestDto.color())
                .age(requestDto.age())
                .build();

        // 2. 엔티티 생성
        LostPost post = LostPost.builder()
                .user(user)
                .categoryCode(CodeConst.PostCategory.ANIMAL) // 현재는 동물 고정 (DTO에 추가되면 requestDto.categoryCode() 사용)
                .title(requestDto.title())
                .content(requestDto.content())
                .statusCode(CodeConst.Status.LOST)
                .detail(detail) // 객체를 통째로 넣으면 Converter가 JSON으로 변환
                .imageUrl(requestDto.imageUrl())
                .latitude(requestDto.latitude())
                .longitude(requestDto.longitude())
                .foundLocation(requestDto.foundLocation())
                .lostDate(requestDto.lostDate())
                .reward(requestDto.reward())
                .build();

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
                requestDto.imageUrl(),
                requestDto.categoryCode(),
                detail,
                requestDto.latitude(),
                requestDto.longitude(),
                requestDto.foundLocation(),
                requestDto.lostDate(),
                requestDto.reward()
        );
    }

    @Override
    @Transactional
    public void updateStatus(UUID userId, Long postId, String statusCode) {
        LostPost post = findPostByIdOrThrow(postId);
        validateWriter(post, userId);

        post.changeStatus(statusCode);
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
