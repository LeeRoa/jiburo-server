package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dto.UserProfileResponseDto;
import com.jiburo.server.domain.user.dto.UserProfileUpdateRequestDto;
import com.jiburo.server.domain.user.dto.UserSettingsResponseDto;
import com.jiburo.server.domain.user.dto.UserSettingsUpdateRequestDto;

import java.util.UUID;

public interface UserService {

    UserProfileResponseDto getMyProfile(UUID userId);

    UserProfileResponseDto updateMyProfile(UUID userId, UserProfileUpdateRequestDto requestDto);

    UserSettingsResponseDto getMySettings(UUID userId);

    UserSettingsResponseDto updateMySettings(UUID userId, UserSettingsUpdateRequestDto requestDto);

    void withdraw(UUID userId);
}
