package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.domain.user.dto.UserProfileResponseDto;
import com.jiburo.server.domain.user.dto.UserProfileUpdateRequestDto;
import com.jiburo.server.domain.user.dto.UserSettingsResponseDto;
import com.jiburo.server.domain.user.dto.UserSettingsUpdateRequestDto;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .oauthId("google_12345")
                .nickname("테스터")
                .email("test@test.com")
                .profileImageUrl("https://cdn.test/old.png")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
    }

    // ───────── getMyProfile ─────────

    @Test
    void 프로필_조회_성공() {
        // given
        given(userRepository.findByIdAndIsDeletedFalse(userId)).willReturn(Optional.of(user));

        // when
        UserProfileResponseDto result = userService.getMyProfile(userId);

        // then
        assertThat(result.nickname()).isEqualTo("테스터");
        assertThat(result.provider()).isEqualTo("google");
        assertThat(result.notificationEnabled()).isTrue();
        assertThat(result.locationEnabled()).isTrue();
    }

    @Test
    void 탈퇴했거나_없는_사용자_조회하면_USER_NOT_FOUND() {
        // given
        given(userRepository.findByIdAndIsDeletedFalse(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMyProfile(userId))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    // ───────── updateMyProfile ─────────

    @Test
    void 프로필_수정_성공() {
        // given
        given(userRepository.findByIdAndIsDeletedFalse(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndIsDeletedFalse("새닉네임")).willReturn(false);

        // when
        UserProfileResponseDto result = userService.updateMyProfile(
                userId, new UserProfileUpdateRequestDto("새닉네임", "https://cdn.test/new.png"));

        // then
        assertThat(result.nickname()).isEqualTo("새닉네임");
        assertThat(result.profileImageUrl()).isEqualTo("https://cdn.test/new.png");
    }

    @Test
    void 프로필_수정_시_null_필드는_기존값_유지() {
        // given
        given(userRepository.findByIdAndIsDeletedFalse(userId)).willReturn(Optional.of(user));

        // when
        UserProfileResponseDto result = userService.updateMyProfile(
                userId, new UserProfileUpdateRequestDto(null, "https://cdn.test/new.png"));

        // then
        assertThat(result.nickname()).isEqualTo("테스터");
        assertThat(result.profileImageUrl()).isEqualTo("https://cdn.test/new.png");
        then(userRepository).should(never()).existsByNicknameAndIsDeletedFalse(null);
    }

    @Test
    void 중복된_닉네임으로_수정하면_DUPLICATE_NICKNAME() {
        // given
        given(userRepository.findByIdAndIsDeletedFalse(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndIsDeletedFalse("중복닉")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateMyProfile(
                userId, new UserProfileUpdateRequestDto("중복닉", null)))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_NICKNAME));
    }

    @Test
    void 자기_닉네임_그대로_보내면_중복검사_하지_않음() {
        // given
        given(userRepository.findByIdAndIsDeletedFalse(userId)).willReturn(Optional.of(user));

        // when
        userService.updateMyProfile(userId, new UserProfileUpdateRequestDto("테스터", null));

        // then
        then(userRepository).should(never()).existsByNicknameAndIsDeletedFalse("테스터");
    }

    // ───────── settings ─────────

    @Test
    void 설정_변경_성공() {
        // given
        given(userRepository.findByIdAndIsDeletedFalse(userId)).willReturn(Optional.of(user));

        // when
        UserSettingsResponseDto result = userService.updateMySettings(
                userId, new UserSettingsUpdateRequestDto(false, null));

        // then
        assertThat(result.notificationEnabled()).isFalse();
        assertThat(result.locationEnabled()).isTrue();
    }

    // ───────── withdraw ─────────

    @Test
    void 회원탈퇴_시_개인정보_제거되고_oauthId가_재가입_가능하게_변경된다() {
        // given
        given(userRepository.findByIdAndIsDeletedFalse(userId)).willReturn(Optional.of(user));

        // when
        userService.withdraw(userId);

        // then
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getOauthId()).startsWith("withdrawn_").isNotEqualTo("google_12345");
        assertThat(user.getEmail()).isNull();
        assertThat(user.getProfileImageUrl()).isNull();
        assertThat(user.getNickname()).isEqualTo("탈퇴한 사용자");
    }
}
