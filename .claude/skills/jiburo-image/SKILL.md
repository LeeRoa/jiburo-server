---
name: jiburo-image
description: Use when working on jiburo-server image upload — Cloudflare R2 presigned
  PUT URLs, ImageMeta lifecycle (PENDING → COMPLETED), 10GB capacity guard, upload
  target folders (CHAT/POST/PROFILE), or anything under domain/image/. Triggers
  include keywords like ImageController, ImageStorageService, ImageStorageServiceImpl,
  ImageMeta, ImageStatus, UploadTargetType, R2Config, S3Presigner, PresignedUrlRequestDto,
  PresignedUrlResponseDto, ImageUploadCompleteRequestDto, /api/v1/images/presigned-url,
  /api/v1/images/complete, R2, 이미지 업로드, presigned, 파일 업로드.
---

# Jiburo Image (Cloudflare R2) 도메인 가이드

서버는 **파일을 직접 받지 않는다**. R2 presigned PUT URL을 발급 → 클라이언트가 R2에 직접 업로드 → 완료 콜백으로 메타 상태만 갱신. AWS S3 SDK v2를 R2 엔드포인트에 연결.

## 진입점

`ImageController` (`/api/v1/images`) — 인증 필요.

| Method | Path | 설명 |
|---|---|---|
| POST | `/presigned-url` | R2 PUT URL + objectKey 발급, `ImageMeta(PENDING)` 생성 |
| POST | `/complete` | 클라 업로드 성공 후 호출, `ImageStatus.COMPLETED`로 전환 |

⚠️ 두 엔드포인트 모두 `@AuditLog` 없음. 추가 시 첫 번째 파라미터 `toString()`이 로그에 들어가니 PII 주의.

## 핵심 파일

- `domain/image/controller/ImageController.java`
- `domain/image/service/ImageStorageService.java` + `Impl`
- `domain/image/config/R2Config.java` — `S3Presigner` 빈 (AWS S3 SDK)
- `domain/image/domain/ImageMeta.java`
- `domain/image/domain/ImageStatus.java` (`CommonCodeType`, group `IMG_STATUS`)
- `domain/image/domain/UploadTargetType.java` (`CommonCodeType`, group `UPLOAD_TARGET`, 폴더 경로 보유)
- `domain/image/repository/ImageMetaRepository.java`
- `domain/image/dto/{PresignedUrlRequestDto,PresignedUrlResponseDto,ImageUploadCompleteRequestDto}.java`

## 업로드 플로우 (3-step)

```
1) 프론트 → POST /api/v1/images/presigned-url
   Body: { fileCode: "POST", extension: "png", fileSize: 12345, originalFileName: "..." }
   ├─ ImageStorageServiceImpl.createPresignedUrl
   │  ├─ sumTotalFileSize() + fileSize > 10GB → IllegalStateException
   │  ├─ objectKey = "{basePath}/{userId}/{uuid}.{ext}"
   │  │     basePath = fileCode.getFolderPath()  // "post/images"
   │  ├─ S3Presigner.presignPutObject (서명 만료 = expiration-minutes)
   │  └─ ImageMeta(PENDING, useYn=true) DB 저장
   └─ Response: { url, objectKey }

2) 프론트 → PUT {presignedUrl} (R2로 직접)  ← 서버 거치지 않음
   Headers: Content-Type 등은 PutObjectRequest builder에서 설정한 것과 일치해야 함

3) 프론트 → POST /api/v1/images/complete
   Body: { fileKey: <objectKey> }
   ├─ findByFileKeyAndUseYnTrue → 없으면 INVALID_FILE_FORMAT
   ├─ imageMeta.user.id != userId → POST_ACCESS_DENIED (재사용된 ErrorCode)
   ├─ 이미 COMPLETED면 idempotent 종료
   └─ updateStatus(COMPLETED)
```

## 엔티티 / DB

```
ImageMeta (PK Long)
 ├── user_id → User
 ├── file_code (ENUM UploadTargetType: CHAT/POST/PROFILE)
 ├── file_key (UNIQUE, 500)   ← R2 전체 경로 (objectKey와 동일)
 ├── original_file_name
 ├── extension (20)
 ├── file_size (Long, byte) ← 10GB 한도 합산 대상
 ├── status_code (ENUM ImageStatus: PENDING/COMPLETED)
 └── use_yn (boolean) ← 논리 삭제
```

10GB 한도: `MAX_CAPACITY_BYTES = 10L * 1024 * 1024 * 1024`. 합산은 `imageMetaRepository.sumTotalFileSize()`.

## R2 설정 (application-r2.properties)

```properties
cloud.cloudflare.r2.credentials.access-key=${R2_ACCESS_KEY}
cloud.cloudflare.r2.credentials.secret-key=${R2_SECRET_KEY}
cloud.cloudflare.r2.endpoint=${R2_ENDPOINT}   # https://<account>.r2.cloudflarestorage.com
cloud.cloudflare.r2.region=auto
cloud.cloudflare.r2.bucket=${R2_BUCKET}
cloud.cloudflare.r2.presigned.expiration-minutes=${R2_PRESIGNED_EXPIRATION_MINUTES}
```

`R2Config.s3Presigner()`는 `endpointOverride` + `Region.of("auto")` + `StaticCredentialsProvider` 조합. **R2는 region 의미가 없어 "auto"** 필수.

## UploadTargetType (folder routing)

| code | folderPath |
|---|---|
| `CHAT` | `chat/images` |
| `POST` | `post/images` |
| `PROFILE` | `profile/images` |

새 타입 추가: enum에 항목 추가 + `data.sql`의 `common_codes` (group `UPLOAD_TARGET`) INSERT — [[jiburo-conventions]] §5.

최종 objectKey: `{folderPath}/{userId-UUID}/{random-uuid}.{ext}` — 폴더 단위로 user별 분리.

## 도메인 규약

1. **서버는 파일 바이트를 받지 않는다** — 항상 presigned URL 방식. multipart upload 컨트롤러를 추가하지 말 것.
2. **`fileKey` = `objectKey`** — DB 컬럼 이름은 `file_key`지만 의미는 R2 객체 키 전체 경로.
3. **`complete` 호출 전엔 PENDING** — 클라이언트가 PUT 실패하면 PENDING이 남음. 정기 청소(미구현)로 일정 기간 후 PENDING + `use_yn=false` 처리 필요.
4. **권한 검증** — `complete`에서 발급자 본인 확인. 다른 사용자가 fileKey만 알아도 상태 변경 못 함.
5. **idempotent complete** — 같은 fileKey로 두 번 호출돼도 두 번째는 그냥 종료.
6. **삭제는 논리 삭제 (`use_yn=false`)** — 물리 삭제와 R2 객체 삭제는 별도 작업 (미구현).
7. **공개 URL 형태** — 현재 코드에 R2 public domain 매핑이 없음. 이미지 GET URL은 별도 base path와 `fileKey`를 합쳐 프론트에서 조립하거나, public bucket이라면 R2 worker로 노출.

## 자주 하는 실수

- ❌ `createPresignedUrl`에 `@Transactional` 빠짐 — `ImageMeta` save가 외부 호출(presign)과 한 단위가 아니라서 일관성 깨질 수 있음. 필요 시 추가.
- ❌ 10GB 한도 검사를 빼고 presign 발급 → 무제한 업로드 가능 → 비용 폭주
- ❌ 한도 초과 시 `IllegalStateException` raw로 던짐 → ✅ `JiburoException(ErrorCode.FILE_SIZE_EXCEEDED)`로 바꾸는 게 일관성 있음 (현재는 raw)
- ❌ `complete`에서 권한 검증을 빼먹음 → 다른 사용자의 fileKey 알아내면 상태 변경 가능
- ❌ PUT 요청의 Content-Type을 prensign할 때 지정해놓고 클라이언트가 다른 값으로 PUT → R2가 SignatureDoesNotMatch 거절. 현재는 별도 지정 안 함.
- ❌ `extension`에 점 포함 (`.png`) → objectKey가 `....png` 형태. 점 없이 `png`만 받을 것 (입력 검증 추가 권장).
- ❌ `fileSize`를 클라이언트가 보낸 값 그대로 신뢰 → 한도 우회 가능. 가능하면 R2 응답으로 검증 (현재는 신뢰).

## ErrorCode

- `FILE_SIZE_EXCEEDED` (413)
- `FILE_UPLOAD_FAILED` (500)
- `INVALID_FILE_FORMAT` (400) — `complete`에서 fileKey 못 찾을 때 재사용
- `INVALID_FILE_CODE` (400) — `UploadTargetType` 매핑 실패 시
- `POST_ACCESS_DENIED` (403) — `complete`에서 본인 확인 실패 시 재사용 중 (이름 의미와 안 맞음, 별도 코드로 분리 검토)

## 관련 skill

- [[jiburo-conventions]] — `CommonCodeType` enum 추가, `ApiResponse`, `JiburoException`
- [[jiburo-post]] — `LostPost.imageUrl` / `LostPostImage`가 이 플로우의 결과를 저장
- [[jiburo-user-auth]] — `@AuthenticationPrincipal CustomOAuth2User.getUserId()`