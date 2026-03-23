package com.jiburo.server.global.service;

import com.jiburo.server.global.dto.PresignedUrlRequestDto;
import com.jiburo.server.global.dto.PresignedUrlResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class R2ServiceImpl implements R2Service {

    private final S3Presigner s3Presigner;

    @Value("${cloud.cloudflare.r2.bucket}")
    private String bucketName;

    @Override
    public PresignedUrlResponseDto createPresignedUrl(PresignedUrlRequestDto request) {
        // 고유한 파일명 생성
        String fileKey = UUID.randomUUID() + "." + request.extension();
        // R2 버킷 내부의 저장 경로
        String objectKey = "chat/images/" + fileKey;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5)) // 5분간 유효
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String url = presignedRequest.url().toString();

        return new PresignedUrlResponseDto(url, fileKey);
    }
}
