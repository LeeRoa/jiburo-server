package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminPostDtos.PostSummary;
import com.jiburo.server.domain.admin.dto.AdminPostDtos.SearchCondition;
import com.jiburo.server.domain.admin.repository.AdminPostQueryRepository.DeletedFilter;
import com.jiburo.server.domain.notification.domain.Notification;
import com.jiburo.server.domain.notification.repository.NotificationRepository;
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
import com.jiburo.server.global.error.JiburoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminPostServiceIntegrationTest {

    @Autowired private AdminPostService adminPostService;
    @Autowired private LostPostRepository lostPostRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;

    private User writer;
    private LostPost publicPost;
    private LostPost privatePost;
    private LostPost deletedPost;

    @BeforeEach
    void setUp() {
        writer = userRepository.save(User.builder().oauthId("kakao_writer").nickname("작성자").roleCode(RoleType.USER).build());
        publicPost = lostPostRepository.save(post("공개 글, 강아지", VisibilityType.PUBLIC));
        privatePost = lostPostRepository.save(post("비공개 글", VisibilityType.PRIVATE));
        deletedPost = lostPostRepository.save(post("삭제된 글", VisibilityType.PUBLIC));
        deletedPost.softDelete();
        lostPostRepository.flush();
    }

    private LostPost post(String title, VisibilityType visibility) {
        return LostPost.builder().user(writer).categoryCode(CategoryType.ANIMAL).title(title).statusCode(PostStatus.LOST)
                .detail(AnimalDetailDto.builder().animalTypeCode(AnimalType.DOG).build())
                .latitude(37.5).longitude(127.0).foundLocation("서울").lostDate(LocalDate.now()).reward(0)
                .visibilityCode(visibility).build();
    }

    private List<String> titles(SearchCondition c) {
        return adminPostService.search(c, PageRequest.of(0, 20)).getContent().stream().map(PostSummary::title).toList();
    }

    @Test
    void 관리자_검색은_비공개_글을_포함하고_삭제_필터를_지원한다() {
        assertThat(titles(new SearchCondition(null, null, null, null, null)))
                .containsExactlyInAnyOrder("공개 글, 강아지", "비공개 글");
        assertThat(titles(new SearchCondition(null, null, null, null, DeletedFilter.ONLY)))
                .containsExactly("삭제된 글");
        assertThat(titles(new SearchCondition(null, null, null, null, DeletedFilter.ALL))).hasSize(3);
        assertThat(titles(new SearchCondition("작성자", null, null, VisibilityType.PRIVATE, null)))
                .containsExactly("비공개 글");
    }

    @Test
    void 강제_삭제하면_soft_delete되고_작성자에게_알림이_간다() {
        adminPostService.forceDelete(publicPost.getId());

        assertThat(lostPostRepository.findById(publicPost.getId()).orElseThrow().isDeleted()).isTrue();
        List<Notification> notis = notificationRepository.findAll();
        assertThat(notis).singleElement().satisfies(n -> {
            assertThat(n.getTypeCode()).isEqualTo(AdminPostService.NOTI_POST_DELETED_BY_ADMIN);
            assertThat(n.getReceiver().getId()).isEqualTo(writer.getId());
            assertThat(n.getArgs()).isEqualTo("공개 글  강아지"); // 콤마 치환
        });
    }

    @Test
    void 이미_삭제된_글은_다시_강제_삭제할_수_없고_복구할_수_있다() {
        assertThatThrownBy(() -> adminPostService.forceDelete(deletedPost.getId())).isInstanceOf(JiburoException.class);

        adminPostService.restore(deletedPost.getId());

        LostPost restored = lostPostRepository.findById(deletedPost.getId()).orElseThrow();
        assertThat(restored.isDeleted()).isFalse();
        assertThat(restored.getDeletedAt()).isNull();
        assertThatThrownBy(() -> adminPostService.restore(publicPost.getId())).isInstanceOf(JiburoException.class);
    }

    @Test
    void 상세는_삭제된_글도_조회된다() {
        assertThat(adminPostService.find(deletedPost.getId()).summary().deleted()).isTrue();
    }
}
