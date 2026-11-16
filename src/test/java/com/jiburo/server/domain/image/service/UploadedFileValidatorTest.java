package com.jiburo.server.domain.image.service;

import com.jiburo.server.domain.image.domain.ImageMeta;
import com.jiburo.server.domain.image.domain.ImageStatus;
import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.domain.image.repository.ImageMetaRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UploadedFileValidatorTest {

    @InjectMocks private UploadedFileValidator validator;
    @Mock private ImageMetaRepository imageMetaRepository;

    private UUID ownerId;
    private User owner;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        owner = user(ownerId);
    }

    private static User user(UUID id) {
        User u = User.builder().oauthId("kakao_" + id).nickname("u").email("").roleCode(RoleType.USER).build();
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static ImageMeta meta(User user, String key, UploadTargetType type, ImageStatus status) {
        return ImageMeta.builder().user(user).fileKey(key).fileCode(type).extension("jpg").fileSize(10L).statusCode(status).build();
    }

    private void assertRejected(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_UPLOADED_FILE));
    }

    @Test
    void 본인이_올려_완료한_지정_용도의_파일이면_통과한다() {
        given(imageMetaRepository.findAllByFileKeyInAndUseYnTrue(any()))
                .willReturn(List.of(meta(owner, "post/images/a.jpg", UploadTargetType.POST, ImageStatus.COMPLETED)));

        assertThatCode(() -> validator.requireOwnedUploads(ownerId, List.of("post/images/a.jpg"), Set.of(UploadTargetType.POST)))
                .doesNotThrowAnyException();
    }

    @Test
    void 등록되지_않은_키나_외부_URL은_거부한다() {
        given(imageMetaRepository.findAllByFileKeyInAndUseYnTrue(any())).willReturn(List.of());

        assertRejected(() -> validator.requireOwnedUpload(ownerId, "https://evil.example/a.jpg", Set.of(UploadTargetType.CHAT)));
    }

    @Test
    void 다른_사람이_올린_파일은_거부한다() {
        given(imageMetaRepository.findAllByFileKeyInAndUseYnTrue(any()))
                .willReturn(List.of(meta(user(UUID.randomUUID()), "chat/images/x.jpg", UploadTargetType.CHAT, ImageStatus.COMPLETED)));

        assertRejected(() -> validator.requireOwnedUpload(ownerId, "chat/images/x.jpg", Set.of(UploadTargetType.CHAT)));
    }

    @Test
    void 업로드가_끝나지_않은_파일은_거부한다() {
        given(imageMetaRepository.findAllByFileKeyInAndUseYnTrue(any()))
                .willReturn(List.of(meta(owner, "chat/images/x.jpg", UploadTargetType.CHAT, ImageStatus.PENDING)));

        assertRejected(() -> validator.requireOwnedUpload(ownerId, "chat/images/x.jpg", Set.of(UploadTargetType.CHAT)));
    }

    @Test
    void 다른_용도로_올린_파일은_거부한다() {
        given(imageMetaRepository.findAllByFileKeyInAndUseYnTrue(any()))
                .willReturn(List.of(meta(owner, "profile/images/x.jpg", UploadTargetType.PROFILE, ImageStatus.COMPLETED)));

        assertRejected(() -> validator.requireOwnedUpload(ownerId, "profile/images/x.jpg", Set.of(UploadTargetType.CHAT)));
    }

    @Test
    void 목록에_빈_값이_섞여_있으면_거부한다() {
        assertRejected(() -> validator.requireOwnedUploads(ownerId, java.util.Arrays.asList("a", null), Set.of(UploadTargetType.POST)));
    }
}
