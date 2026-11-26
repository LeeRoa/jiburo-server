package com.jiburo.server.domain.chat.service;

import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.chat.dto.ChatRoomCreateDto;
import com.jiburo.server.domain.chat.dto.ChatRoomListDto;
import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.chat.repository.ChatRoomRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.domain.enums.VisibilityType;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 사용자가 같은 게시글로 채팅방 생성을 동시에 여러 번 요청해도 방은 하나만 생겨야 한다.
 * 실제 트랜잭션·행 잠금이 필요하므로 클래스에 @Transactional을 걸지 않고 직접 정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ChatRoomCreateConcurrencyIntegrationTest {

    private static final int REQUESTS = 4;

    @Autowired private ChatRoomService chatRoomService;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatParticipantRepository chatParticipantRepository;
    @Autowired private LostPostRepository lostPostRepository;
    @Autowired private UserRepository userRepository;

    private User owner;
    private User requester;
    private LostPost post;

    @BeforeEach
    void setUp() {
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        owner = userRepository.save(User.builder().oauthId("kakao_room_owner_" + suffix).nickname("작성자").email("").roleCode(RoleType.USER).build());
        requester = userRepository.save(User.builder().oauthId("kakao_room_req_" + suffix).nickname("요청자").email("").roleCode(RoleType.USER).build());
        post = lostPostRepository.save(LostPost.builder()
                .user(owner).categoryCode(CategoryType.ANIMAL).title("강아지 찾아요").content("내용")
                .statusCode(PostStatus.LOST).latitude(37.5).longitude(127.0).foundLocation("서울")
                .detail(com.jiburo.server.domain.post.dto.detail.AnimalDetailDto.builder().animalTypeCode(com.jiburo.server.domain.post.domain.enums.AnimalType.DOG).build())
                .lostDate(java.time.LocalDate.now()).reward(0)
                .visibilityCode(VisibilityType.PUBLIC).build());
    }

    @AfterEach
    void cleanUp() {
        if (post == null || post.getId() == null) {
            userRepository.deleteAll(java.util.stream.Stream.of(owner, requester).filter(java.util.Objects::nonNull).toList());
            return;
        }
        List<ChatRoom> rooms = chatRoomRepository.findAll().stream()
                .filter(r -> r.getPost().getId().equals(post.getId())).toList();
        rooms.forEach(r -> chatParticipantRepository.deleteAll(chatParticipantRepository.findAllByChatRoomId(r.getId())));
        chatRoomRepository.deleteAll(rooms);
        lostPostRepository.deleteById(post.getId());
        userRepository.deleteAll(List.of(owner, requester));
    }

    @RepeatedTest(3)
    void 같은_게시글로_동시에_채팅을_요청해도_방은_하나만_생긴다() throws Exception {
        ChatRoomCreateDto dto = new ChatRoomCreateDto(HashidsUtils.encode(post.getId()));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
        try {
            List<Future<ChatRoomListDto>> results = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                Callable<ChatRoomListDto> call = () -> {
                    start.await();
                    return chatRoomService.createChatRoom(dto, requester.getId());
                };
                results.add(pool.submit(call));
            }
            start.countDown(); // 동시에 출발

            List<String> roomIds = new ArrayList<>();
            for (Future<ChatRoomListDto> f : results) {
                roomIds.add(f.get(30, TimeUnit.SECONDS).roomId()); // 모두 성공해야 한다 (500 없음)
            }

            // 모든 요청이 같은 방을 받았고, DB에도 방이 하나뿐
            assertThat(roomIds).containsOnly(roomIds.get(0));
            long roomCount = chatRoomRepository.findAll().stream()
                    .filter(r -> r.getPost().getId().equals(post.getId())).count();
            assertThat(roomCount).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
