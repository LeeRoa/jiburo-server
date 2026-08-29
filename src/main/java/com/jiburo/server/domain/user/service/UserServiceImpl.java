package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.dto.UserProfileResponseDto;
import com.jiburo.server.domain.user.dto.UserProfileUpdateRequestDto;
import com.jiburo.server.domain.user.dto.UserSettingsResponseDto;
import com.jiburo.server.domain.user.dto.UserSettingsUpdateRequestDto;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserProfileResponseDto getMyProfile(UUID userId) {
        return UserProfileResponseDto.from(findActiveUserOrThrow(userId));
    }

    @Override
    @Transactional
    public UserProfileResponseDto updateMyProfile(UUID userId, UserProfileUpdateRequestDto requestDto) {
        User user = findActiveUserOrThrow(userId);

        String newNickname = requestDto.nickname();
        if (newNickname != null && !newNickname.equals(user.getNickname())
                && userRepository.existsByNicknameAndIsDeletedFalse(newNickname)) {
            throw new JiburoException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.editProfile(newNickname, requestDto.profileImageUrl());
        return UserProfileResponseDto.from(user);
    }

    @Override
    public UserSettingsResponseDto getMySettings(UUID userId) {
        return UserSettingsResponseDto.from(findActiveUserOrThrow(userId));
    }

    @Override
    @Transactional
    public UserSettingsResponseDto updateMySettings(UUID userId, UserSettingsUpdateRequestDto requestDto) {
        User user = findActiveUserOrThrow(userId);
        user.updateSettings(requestDto.notificationEnabled(), requestDto.locationEnabled());
        return UserSettingsResponseDto.from(user);
    }

    @Override
    @Transactional
    public void withdraw(UUID userId) {
        findActiveUserOrThrow(userId).withdraw();
    }

    private User findActiveUserOrThrow(UUID userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));
    }
}
