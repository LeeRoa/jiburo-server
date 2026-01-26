package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.*;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.domain.CodeConst;
import com.jiburo.server.global.error.BusinessException;
import com.jiburo.server.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostPostServiceImpl implements LostPostService {

    private final LostPostRepository lostPostRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Long create(Long userId, LostPostCreateRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LostPost post = LostPost.builder()
                .user(user)
                .title(requestDto.title())
                .content(requestDto.content())
                .statusCode(CodeConst.Status.LOST)
                .animalTypeCode(requestDto.animalTypeCode())
                .breed(requestDto.breed())
                .genderCode(requestDto.genderCode())
                .color(requestDto.color())
                .age(requestDto.age())
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
    public void update(Long userId, Long postId, LostPostUpdateRequestDto requestDto) {
        LostPost post = findPostByIdOrThrow(postId);
        validateWriter(post, userId);

        post.update(
                requestDto.title(), requestDto.content(), requestDto.imageUrl(),
                requestDto.animalTypeCode(), requestDto.breed(), requestDto.genderCode(),
                requestDto.color(), requestDto.age(),
                requestDto.latitude(), requestDto.longitude(), requestDto.foundLocation(),
                requestDto.lostDate(), requestDto.reward()
        );
    }

    @Override
    @Transactional
    public void updateStatus(Long userId, Long postId, String statusCode) {
        LostPost post = findPostByIdOrThrow(postId);
        validateWriter(post, userId);

        post.changeStatus(statusCode);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long postId) {
        LostPost post = findPostByIdOrThrow(postId);
        validateWriter(post, userId);

        lostPostRepository.delete(post);
    }

    @Override
    public Page<LostPostResponseDto> search(LostPostSearchCondition condition, Pageable pageable) {
        // Repository가 반환한 Page<Entity>를 Page<Dto>로 변환 (.map 사용)
        return lostPostRepository.search(condition, pageable)
                .map(LostPostResponseDto::from);
    }

    private LostPost findPostByIdOrThrow(Long id) {
        return lostPostRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateWriter(LostPost post, Long userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }
    }
}