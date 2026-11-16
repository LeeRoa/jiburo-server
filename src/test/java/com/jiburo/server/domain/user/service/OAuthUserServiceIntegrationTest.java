package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.domain.user.dto.OAuthAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 실제 DB(H2)와 트랜잭션으로 동시 첫 로그인을 재현한다.
 * 클래스에 @Transactional을 걸지 않는다 — OAuthUserService가 직접 여는 새 트랜잭션들이 실제로 커밋·롤백되어야 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class OAuthUserServiceIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private OAuthUserService oAuthUserService;

    private static final String OAUTH_ID = "kakao_777000";

    private static OAuthAttributes kakao(String nickname) {
        return OAuthAttributes.of("kakao", "id", Map.of(
                "id", 777000L,
                "kakao_account", Map.of("email", "race@kakao.com",
                        "profile", Map.of("nickname", nickname, "profile_image_url", "https://kakao.com/p.png"))));
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByOauthId(OAUTH_ID).ifPresent(userRepository::delete);
    }

    @Test
    void 처음_로그인하면_가입되고_즉시_DB에_반영된다() {
        User user = oAuthUserService.findOrCreate(kakao("신규"));

        assertThat(user.getId()).isNotNull();
        assertThat(userRepository.findByOauthId(OAUTH_ID)).isPresent();
    }

    @Test
    void 다시_로그인하면_같은_계정의_프로필만_갱신된다() {
        User first = oAuthUserService.findOrCreate(kakao("처음이름"));

        User second = oAuthUserService.findOrCreate(kakao("바뀐이름"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(userRepository.findByOauthId(OAUTH_ID).orElseThrow().getNickname()).isEqualTo("바뀐이름");
    }

    @Test
    void 동시에_다른_요청이_먼저_가입시켰으면_충돌_후_그_계정으로_로그인된다() {
        // given: 다른 요청이 같은 계정을 방금 가입시켜 커밋했다
        User winner = userRepository.saveAndFlush(User.builder()
                .oauthId(OAUTH_ID).nickname("먼저가입").email("").roleCode(RoleType.USER).build());

        // 이 요청의 첫 조회는 그 행을 보기 전 시점이었다고 가정 (첫 findByOauthId만 '없음', 이후는 실제 DB)
        UserRepository racing = mock(UserRepository.class, delegatesTo(userRepository));
        given(racing.findByOauthId(anyString()))
                .willReturn(Optional.empty())
                .willAnswer(inv -> userRepository.findByOauthId(inv.getArgument(0)));
        OAuthUserService service = new OAuthUserService(racing, transactionManager);

        // when: 가입 INSERT가 유니크 제약에 걸린다 → 새 트랜잭션에서 재조회
        User result = service.findOrCreate(kakao("나중요청"));

        // then: 500 없이 먼저 가입된 계정으로 로그인, 계정은 하나뿐
        assertThat(result.getId()).isEqualTo(winner.getId());
        assertThat(userRepository.findAll().stream().filter(u -> OAUTH_ID.equals(u.getOauthId())).count()).isEqualTo(1);
    }
}
