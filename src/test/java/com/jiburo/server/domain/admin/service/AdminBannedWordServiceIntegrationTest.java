package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.AddResult;
import com.jiburo.server.domain.chat.domain.ChatParticipant;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.chat.repository.ChatRoomRepository;
import com.jiburo.server.domain.chat.service.ChatMessageService;
import com.jiburo.server.domain.moderation.service.BannedWordFilter;
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
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminBannedWordServiceIntegrationTest {

    @Autowired private AdminBannedWordService adminBannedWordService;
    @Autowired private BannedWordFilter filter;
    @Autowired private ChatMessageService chatMessageService;
    @Autowired private UserRepository userRepository;
    @Autowired private LostPostRepository lostPostRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatParticipantRepository chatParticipantRepository;

    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // 테스트 트랜잭션은 커밋되지 않아 afterCommit 캐시 비우기가 돌지 않으므로 직접 비운다
        filter.evictPeriodically();
    }

    private void addWords(String... words) {
        adminBannedWordService.add(List.of(words), adminId);
        filter.evictPeriodically();
    }

    @Test
    void 추가는_정규화하고_중복은_건너뛴다() {
        AddResult first = adminBannedWordService.add(List.of("Bad Word", "바보"), adminId);
        AddResult second = adminBannedWordService.add(List.of("badword", "바 보", "멍청이"), adminId);

        assertThat(first.added()).extracting("word").containsExactly("badword", "바보");
        assertThat(second.added()).extracting("word").containsExactly("멍청이");
        assertThat(second.skipped()).containsExactly("badword", "바보");
        assertThat(adminBannedWordService.search("바", PageRequest.of(0, 50)).getContent())
                .extracting("word").containsExactly("바보");
    }

    @Test
    void 대소문자와_사이에_끼운_기호를_무시하고_가린다() {
        addWords("바보", "bad");

        assertThat(filter.mask("너 바보야")).isEqualTo("너 ***야");
        assertThat(filter.mask("바.보 바-보 BAD b*a*d")).isEqualTo("*** *** *** ***");
        assertThat(filter.mask("정상 문장")).isEqualTo("정상 문장");
        // 공백을 사이에 둔 경우는 단어 경계 오탐을 막기 위해 가리지 않는다
        assertThat(filter.mask("바 보")).isEqualTo("바 보");
        assertThat(filter.contains("BaD")).isTrue();
        assertThat(adminBannedWordService.test("바보").masked()).isEqualTo("***");
    }

    @Test
    void 수정하면_수정자와_닉네임이_남고_다른_항목과_같은_단어로는_못_바꾼다() {
        User creator = userRepository.save(User.builder().oauthId("kakao_creator").nickname("등록자").roleCode(RoleType.USER).build());
        User editor = userRepository.save(User.builder().oauthId("kakao_editor").nickname("수정자").roleCode(RoleType.USER).build());
        AddResult added = adminBannedWordService.add(List.of("바부", "멍청이"), creator.getId());
        Long id = HashidsUtils.decode(added.added().get(0).id());
        assertThat(added.added().get(0).createdByNickname()).isEqualTo("등록자");
        assertThat(added.added().get(0).updatedByNickname()).isEqualTo("등록자");

        var updated = adminBannedWordService.update(id, "바 보", editor.getId());

        assertThat(updated.word()).isEqualTo("바보");
        assertThat(updated.createdByNickname()).isEqualTo("등록자");
        assertThat(updated.updatedBy()).isEqualTo(editor.getId());
        assertThat(updated.updatedByNickname()).isEqualTo("수정자");
        assertThat(updated.updatedAt()).isNotNull();

        filter.evictPeriodically();
        assertThat(filter.mask("바보 바부")).isEqualTo("*** 바부");

        assertThatThrownBy(() -> adminBannedWordService.update(id, "멍청이", editor.getId()))
                .extracting("errorCode").isEqualTo(ErrorCode.BANNED_WORD_DUPLICATED);
    }

    @Test
    void 삭제하면_더이상_가리지_않는다() {
        AddResult added = adminBannedWordService.add(List.of("바보"), adminId);
        filter.evictPeriodically();
        assertThat(filter.contains("바보")).isTrue();

        adminBannedWordService.delete(HashidsUtils.decode(added.added().get(0).id()));
        filter.evictPeriodically();

        assertThat(filter.contains("바보")).isFalse();
        assertThatThrownBy(() -> adminBannedWordService.delete(HashidsUtils.decode(added.added().get(0).id())))
                .extracting("errorCode").isEqualTo(ErrorCode.BANNED_WORD_NOT_FOUND);
    }

    @Test
    void 채팅_텍스트의_금지어는_가려서_저장된다() {
        addWords("바보");
        User a = userRepository.save(User.builder().oauthId("kakao_a").nickname("가").roleCode(RoleType.USER).build());
        User b = userRepository.save(User.builder().oauthId("kakao_b").nickname("나").roleCode(RoleType.USER).build());
        LostPost post = lostPostRepository.save(LostPost.builder().user(b).categoryCode(CategoryType.ANIMAL).title("글")
                .statusCode(PostStatus.LOST).detail(AnimalDetailDto.builder().animalTypeCode(AnimalType.DOG).build())
                .latitude(37.5).longitude(127.0).foundLocation("서울").lostDate(LocalDate.now()).reward(0)
                .visibilityCode(VisibilityType.PUBLIC).build());
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().post(post).host(a).build());
        room.addParticipant(chatParticipantRepository.save(ChatParticipant.builder().chatRoom(room).user(a).build()));
        room.addParticipant(chatParticipantRepository.save(ChatParticipant.builder().chatRoom(room).user(b).build()));

        var sent = chatMessageService.saveMessage(room.getId(), a.getId(), new ChatMessageRequestDto("이 바보야", ChatMsgType.TALK));

        assertThat(sent.content()).isEqualTo("이 ***야");
        assertThat(room.getLastMessage()).isEqualTo("이 ***야");
    }
}
