package com.jiburo.server.domain.user.controller;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.service.AuthTokenService;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 테스트 전용 로그인 우회 API.
 * `local` 프로파일(--spring.profiles.active=local)에서만 빈으로 등록되므로 운영/개발 서버에는 존재하지 않는다.
 * 소셜 로그인 없이 닉네임만으로 테스트 계정을 만들고(없으면 가입) 정상 로그인과 같은 경로로 토큰을 발급한다.
 */
@Profile("local")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Dev Auth", description = "로컬 전용 로그인 우회 (local 프로파일에서만 활성)")
public class DevAuthController {

    private static final String DEFAULT_NICKNAME = "tester";

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;

    public record DevLoginRequest(String nickname) {}

    @Operation(summary = "로컬 전용 개발 로그인", description = "닉네임으로 테스트 계정(oauthId=dev_{닉네임})을 만들거나 찾아 토큰을 발급합니다.")
    @PostMapping("/dev-login")
    public ApiResponse<TokenResponseDto> devLogin(@RequestBody(required = false) DevLoginRequest request) {
        String nickname = (request == null || request.nickname() == null || request.nickname().isBlank())
                ? DEFAULT_NICKNAME
                : request.nickname().trim();
        String oauthId = "dev_" + nickname;

        User user = userRepository.findByOauthId(oauthId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .oauthId(oauthId)
                        .nickname(nickname)
                        .email("")
                        .roleCode(RoleType.USER)
                        .build()));

        return ApiResponse.success(authTokenService.issueLoginTokens(user));
    }
}
