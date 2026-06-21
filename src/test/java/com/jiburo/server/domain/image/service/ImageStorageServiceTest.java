package com.jiburo.server.domain.image.service;

import com.jiburo.server.domain.image.domain.ImageMeta;
import com.jiburo.server.domain.image.domain.ImageStatus;
import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.domain.image.dto.PresignedUrlRequestDto;
import com.jiburo.server.domain.image.dto.PresignedUrlResponseDto;
import com.jiburo.server.domain.image.repository.ImageMetaRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ImageStorageServiceTest {

    @InjectMocks private ImageStorageServiceImpl imageStorageService;

    @Mock private S3Presigner s3Presigner;
    @Mock private ImageMetaRepository imageMetaRepository;
    @Mock private UserRepository userRepository;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);
        ReflectionTestUtils.setField(imageStorageService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(imageStorageService, "expirationMinutes", 15L);

        userId = UUID.randomUUID();
        user = User.builder()
                .oauthId("google_test")
                .nickname("테스터")
                .email("test@test.com")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
    }

    // ───────── createPresignedUrl ─────────

    @Test
    void presigned_URL_생성_성공() throws MalformedURLException {
        // given
        PresignedUrlRequestDto request =
                new PresignedUrlRequestDto(UploadTargetType.POST, "photo.jpg", "jpg", 1024L);

        given(imageMetaRepository.sumTotalFileSize()).willReturn(0L);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(imageMetaRepository.save(any(ImageMeta.class))).willReturn(mock(ImageMeta.class));

        PresignedPutObjectRequest presignedPutObjectRequest = mock(PresignedPutObjectRequest.class);
        given(presignedPutObjectRequest.url()).willReturn(URI.create("https://test.r2.cloudflarestorage.com/key").toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willReturn(presignedPutObjectRequest);

        // when
        PresignedUrlResponseDto result = imageStorageService.createPresignedUrl(userId, request);

        // then
        assertThat(result.presignedUrl()).contains("test.r2.cloudflarestorage.com");
        then(imageMetaRepository).should().save(any(ImageMeta.class));
    }

    @Test
    void 허용되지_않은_확장자면_INVALID_FILE_FORMAT() {
        // given
        PresignedUrlRequestDto request =
                new PresignedUrlRequestDto(UploadTargetType.POST, "document.pdf", "pdf", 1024L);

        // when & then
        assertThatThrownBy(() -> imageStorageService.createPresignedUrl(userId, request))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_FILE_FORMAT));
        then(imageMetaRepository).should(never()).save(any());
    }

    @Test
    void 용량_초과_시_IllegalStateException() {
        // given
        long nearlyFull = 10L * 1024 * 1024 * 1024 - 100; // 10GB - 100bytes
        PresignedUrlRequestDto request =
                new PresignedUrlRequestDto(UploadTargetType.POST, "big.jpg", "jpg", 200L);

        given(imageMetaRepository.sumTotalFileSize()).willReturn(nearlyFull);

        // when & then
        assertThatThrownBy(() -> imageStorageService.createPresignedUrl(userId, request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 사용자_없으면_USER_NOT_FOUND() {
        // given
        PresignedUrlRequestDto request =
                new PresignedUrlRequestDto(UploadTargetType.POST, "photo.jpg", "jpg", 1024L);
        given(imageMetaRepository.sumTotalFileSize()).willReturn(0L);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> imageStorageService.createPresignedUrl(userId, request))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    // ───────── completeUpload ─────────

    @Test
    void 업로드_완료_상태_COMPLETED로_변경() {
        // given
        ImageMeta imageMeta = ImageMeta.builder()
                .user(user)
                .fileCode(UploadTargetType.POST)
                .fileKey("post/images/" + userId + "/uuid.jpg")
                .originalFileName("photo.jpg")
                .extension("jpg")
                .fileSize(1024L)
                .statusCode(ImageStatus.PENDING)
                .build();

        given(imageMetaRepository.findByFileKeyAndUseYnTrue("post/images/" + userId + "/uuid.jpg"))
                .willReturn(Optional.of(imageMeta));

        // when
        imageStorageService.completeUpload(userId, "post/images/" + userId + "/uuid.jpg");

        // then
        assertThat(imageMeta.getStatusCode()).isEqualTo(ImageStatus.COMPLETED);
    }

    @Test
    void 이미_COMPLETED인_파일은_상태_변경_안함() {
        // given
        ImageMeta imageMeta = ImageMeta.builder()
                .user(user)
                .fileCode(UploadTargetType.POST)
                .fileKey("post/images/uuid.jpg")
                .originalFileName("photo.jpg")
                .extension("jpg")
                .fileSize(1024L)
                .statusCode(ImageStatus.COMPLETED)
                .build();

        given(imageMetaRepository.findByFileKeyAndUseYnTrue("post/images/uuid.jpg"))
                .willReturn(Optional.of(imageMeta));

        // when
        imageStorageService.completeUpload(userId, "post/images/uuid.jpg");

        // then — 상태가 그대로 COMPLETED
        assertThat(imageMeta.getStatusCode()).isEqualTo(ImageStatus.COMPLETED);
    }

    @Test
    void 파일키_없으면_INVALID_FILE_FORMAT() {
        // given
        given(imageMetaRepository.findByFileKeyAndUseYnTrue("nonexistent/key.jpg"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> imageStorageService.completeUpload(userId, "nonexistent/key.jpg"))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_FILE_FORMAT));
    }

    @Test
    void 본인_파일_아니면_POST_ACCESS_DENIED() {
        // given
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder()
                .oauthId("kakao_other")
                .nickname("다른사람")
                .email("other@test.com")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(otherUser, "id", otherUserId);

        ImageMeta imageMeta = ImageMeta.builder()
                .user(otherUser)
                .fileCode(UploadTargetType.POST)
                .fileKey("post/images/other.jpg")
                .originalFileName("photo.jpg")
                .extension("jpg")
                .fileSize(1024L)
                .statusCode(ImageStatus.PENDING)
                .build();

        given(imageMetaRepository.findByFileKeyAndUseYnTrue("post/images/other.jpg"))
                .willReturn(Optional.of(imageMeta));

        // when & then
        assertThatThrownBy(() -> imageStorageService.completeUpload(userId, "post/images/other.jpg"))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.POST_ACCESS_DENIED));
    }
}
