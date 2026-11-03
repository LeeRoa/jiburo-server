package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.UserAdminRole;
import com.jiburo.server.domain.admin.dto.AdminDashboardDtos.DashboardResponse;
import com.jiburo.server.domain.admin.dto.AdminDashboardDtos.Period;
import com.jiburo.server.domain.admin.repository.AdminRoleRepository;
import com.jiburo.server.domain.admin.repository.UserAdminRoleRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.AnimalType;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.domain.enums.VisibilityType;
import com.jiburo.server.domain.post.dto.detail.AnimalDetailDto;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminDashboardServiceIntegrationTest {

    @Autowired private AdminDashboardService dashboardService;
    @Autowired private AdminAuthorizationService authorizationService;
    @Autowired private UserRepository userRepository;
    @Autowired private LostPostRepository lostPostRepository;
    @Autowired private AdminRoleRepository adminRoleRepository;
    @Autowired private UserAdminRoleRepository userAdminRoleRepository;

    private User writer;

    @BeforeEach
    void setUp() {
        authorizationService.invalidateAll();
        writer = userRepository.save(User.builder().oauthId("kakao_w").nickname("작성자").roleCode(RoleType.USER).build());
        userRepository.save(User.builder().oauthId("kakao_w2").nickname("작성자2").roleCode(RoleType.USER).build());
        LostPost lost = post(PostStatus.LOST);
        LostPost done = post(PostStatus.LOST);
        lostPostRepository.save(lost);
        lostPostRepository.save(done);
        done.changeStatus(PostStatus.COMPLETE);
        lostPostRepository.flush();
    }

    private LostPost post(PostStatus status) {
        return LostPost.builder().user(writer).categoryCode(CategoryType.ANIMAL).title("t").statusCode(status)
                .detail(AnimalDetailDto.builder().animalTypeCode(AnimalType.DOG).build())
                .latitude(37.5).longitude(127.0).foundLocation("서울").lostDate(LocalDate.now()).reward(0)
                .visibilityCode(VisibilityType.PUBLIC).build();
    }

    @Test
    void 기간_요약과_일별_추이를_집계한다() {
        DashboardResponse r = dashboardService.get(writer.getId(), Period.DAYS_7);

        assertThat(r.summary().newUsers()).isEqualTo(2);
        assertThat(r.summary().newPosts()).isEqualTo(2);
        assertThat(r.summary().completedPosts()).isEqualTo(1);
        assertThat(r.daily()).hasSize(7);
        assertThat(r.daily().get(6).date()).isEqualTo(LocalDate.now());
        assertThat(r.daily().get(6).newPosts()).isEqualTo(2);
        assertThat(r.daily().get(0).newPosts()).isZero();
        assertThat(r.storage().capacityBytes()).isEqualTo(10L * 1024 * 1024 * 1024);
        assertThat(r.storage().warning()).isFalse();
    }

    @Test
    void 감사_로그_권한이_없으면_최근_관리자_작업은_null_있으면_목록() {
        assertThat(dashboardService.get(writer.getId(), Period.TODAY).recentAdminActions()).isNull();

        AdminRole superAdmin = adminRoleRepository.findByCode(AdminRole.SUPER_ADMIN_CODE).orElseThrow();
        userAdminRoleRepository.save(new UserAdminRole(writer, superAdmin));
        authorizationService.invalidate(writer.getId());

        assertThat(dashboardService.get(writer.getId(), Period.TODAY).recentAdminActions()).isNotNull();
    }
}
