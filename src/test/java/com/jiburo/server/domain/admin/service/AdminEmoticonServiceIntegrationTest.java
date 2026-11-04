package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminEmoticonDtos.CreatePackRequest;
import com.jiburo.server.domain.admin.dto.AdminEmoticonDtos.PackResponse;
import com.jiburo.server.domain.admin.dto.AdminEmoticonDtos.UpdatePackRequest;
import com.jiburo.server.domain.chat.domain.ChatParticipant;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.repository.ChatMessageRepository;
import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.chat.repository.ChatRoomRepository;
import com.jiburo.server.domain.chat.service.ChatMessageService;
import com.jiburo.server.domain.emoticon.dto.EmoticonPackResponseDto;
import com.jiburo.server.domain.emoticon.service.EmoticonServiceImpl;
import com.jiburo.server.domain.image.domain.ImageMeta;
import com.jiburo.server.domain.image.domain.ImageStatus;
import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.domain.image.repository.ImageMetaRepository;
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
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminEmoticonServiceIntegrationTest {

    @Autowired private AdminEmoticonService adminEmoticonService;
    @Autowired private EmoticonServiceImpl emoticonService;
    @Autowired private ChatMessageService chatMessageService;
    @Autowired private ImageMetaRepository imageMetaRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LostPostRepository lostPostRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatParticipantRepository chatParticipantRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder().oauthId("kakao_admin").nickname("관리자").roleCode(RoleType.USER).build());
        // 테스트 트랜잭션은 커밋되지 않아 afterCommit 캐시 비우기가 돌지 않으므로 직접 비운다
        emoticonService.evictPeriodically();
    }

    private String uploaded(String key, UploadTargetType target, ImageStatus status) {
        ImageMeta meta = ImageMeta.builder().user(admin).fileCode(target).fileKey(key)
                .extension(key.substring(key.lastIndexOf('.') + 1)).fileSize(100L).statusCode(status).build();
        imageMetaRepository.save(meta);
        return key;
    }

    private PackResponse packWith(String name, boolean visible, String... keys) {
        PackResponse pack = adminEmoticonService.createPack(new CreatePackRequest(name, visible));
        return keys.length == 0 ? pack
                : adminEmoticonService.addEmoticons(HashidsUtils.decode(pack.id()), List.of(keys));
    }

    @Test
    void 묶음은_목록_뒤에_추가되고_이모티콘은_순서대로_등록된다() {
        PackResponse first = packWith("강아지", true);
        PackResponse second = packWith("고양이", false,
                uploaded("emoticon/images/a/1.png", UploadTargetType.EMOTICON, ImageStatus.COMPLETED),
                uploaded("emoticon/images/a/2.gif", UploadTargetType.EMOTICON, ImageStatus.COMPLETED));

        assertThat(second.sortOrder()).isGreaterThan(first.sortOrder());
        assertThat(second.visible()).isFalse();
        assertThat(second.emoticons()).extracting("imageKey")
                .containsExactly("emoticon/images/a/1.png", "emoticon/images/a/2.gif");
        assertThat(second.emoticons()).extracting("sortOrder").containsExactly(0, 1);
    }

    @Test
    void 업로드가_끝나지_않았거나_이모티콘용이_아닌_이미지는_등록할_수_없다() {
        Long packId = HashidsUtils.decode(packWith("강아지", true).id());
        String pending = uploaded("emoticon/images/a/p.png", UploadTargetType.EMOTICON, ImageStatus.PENDING);
        String chatImage = uploaded("chat/images/a/c.png", UploadTargetType.CHAT, ImageStatus.COMPLETED);

        for (String key : List.of(pending, chatImage, "emoticon/images/none.png")) {
            assertThatThrownBy(() -> adminEmoticonService.addEmoticons(packId, List.of(key)))
                    .isInstanceOf(JiburoException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_EMOTICON_IMAGE);
        }
    }

    @Test
    void 채팅_목록에는_노출_중이고_이모티콘이_있는_묶음만_나온다() {
        String key = uploaded("emoticon/images/a/1.png", UploadTargetType.EMOTICON, ImageStatus.COMPLETED);
        packWith("노출", true, key);
        packWith("비노출", false, uploaded("emoticon/images/a/2.png", UploadTargetType.EMOTICON, ImageStatus.COMPLETED));
        packWith("빈 묶음", true);

        List<EmoticonPackResponseDto> catalog = emoticonService.findCatalog();

        assertThat(catalog).extracting(EmoticonPackResponseDto::name).containsExactly("노출");
        assertThat(catalog.get(0).thumbnailUrl()).isEqualTo(catalog.get(0).emoticons().get(0).imageUrl());
    }

    @Test
    void 순서_변경은_전체_항목과_정확히_일치해야_한다() {
        PackResponse pack = packWith("강아지", true,
                uploaded("emoticon/images/a/1.png", UploadTargetType.EMOTICON, ImageStatus.COMPLETED),
                uploaded("emoticon/images/a/2.png", UploadTargetType.EMOTICON, ImageStatus.COMPLETED));
        Long packId = HashidsUtils.decode(pack.id());
        String e1 = pack.emoticons().get(0).id();
        String e2 = pack.emoticons().get(1).id();

        assertThatThrownBy(() -> adminEmoticonService.reorderEmoticons(packId, List.of(e2)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_EMOTICON_ORDER);
        assertThatThrownBy(() -> adminEmoticonService.reorderEmoticons(packId, List.of(e2, e2)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_EMOTICON_ORDER);

        adminEmoticonService.reorderEmoticons(packId, List.of(e2, e1));

        PackResponse reordered = adminEmoticonService.findAll().stream().filter(p -> p.id().equals(pack.id())).findFirst().orElseThrow();
        assertThat(reordered.emoticons()).extracting("id").containsExactly(e2, e1);
    }

    @Test
    void 이모티콘_메시지는_이미지_키로_저장되고_비노출_묶음은_보낼_수_없다() {
        PackResponse pack = packWith("강아지", true,
                uploaded("emoticon/images/a/wave.gif", UploadTargetType.EMOTICON, ImageStatus.COMPLETED));
        String emoticonId = pack.emoticons().get(0).id();

        User other = userRepository.save(User.builder().oauthId("kakao_other").nickname("상대").roleCode(RoleType.USER).build());
        LostPost post = lostPostRepository.save(LostPost.builder().user(other).categoryCode(CategoryType.ANIMAL).title("글")
                .statusCode(PostStatus.LOST).detail(AnimalDetailDto.builder().animalTypeCode(AnimalType.DOG).build())
                .latitude(37.5).longitude(127.0).foundLocation("서울").lostDate(LocalDate.now()).reward(0)
                .visibilityCode(VisibilityType.PUBLIC).build());
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().post(post).host(admin).build());
        room.addParticipant(chatParticipantRepository.save(ChatParticipant.builder().chatRoom(room).user(admin).build()));
        room.addParticipant(chatParticipantRepository.save(ChatParticipant.builder().chatRoom(room).user(other).build()));

        ChatMessageResponseDto sent = chatMessageService.saveMessage(room.getId(), admin.getId(),
                new ChatMessageRequestDto(emoticonId, ChatMsgType.EMOTICON));

        assertThat(sent.messageTypeCode()).isEqualTo(ChatMsgType.EMOTICON);
        assertThat(sent.content()).endsWith("emoticon/images/a/wave.gif");
        assertThat(chatMessageRepository.findById(HashidsUtils.decode(sent.messageId())).orElseThrow().getContent())
                .isEqualTo("emoticon/images/a/wave.gif");
        assertThat(room.getLastMessage()).isEqualTo("[EMOTICON]");

        adminEmoticonService.updatePack(HashidsUtils.decode(pack.id()), new UpdatePackRequest(null, false));

        assertThatThrownBy(() -> chatMessageService.saveMessage(room.getId(), admin.getId(),
                new ChatMessageRequestDto(emoticonId, ChatMsgType.EMOTICON)))
                .extracting("errorCode").isEqualTo(ErrorCode.EMOTICON_NOT_FOUND);
    }
}
