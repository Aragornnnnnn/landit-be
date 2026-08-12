# LAN-299 공지 이미지 업로드 설계

## 목표

- 어드민이 JPEG, PNG, WebP 이미지를 S3에 직접 업로드할 수 있도록 presigned PUT URL을 발급한다.
- 업로드된 이미지의 CloudFront URL을 기존 편지함 `contentBlocks`에 저장한다.
- 미사용 이미지 정리와 실제 업로드 파일 검증은 이번 범위에서 제외한다.

## API

```text
POST /api/v1/admin/content-images/presigned-url
```

요청은 `fileName`, `contentType`, `fileSize`를 받는다. 확장자와 MIME type이 일치하고 크기가 1 byte 이상 10 MiB 이하일 때만 발급한다. 실제 업로드 크기는 현재 PUT 계약의 제약상 요청값만 검증한다.

응답에는 다음 값을 포함한다.

- 5분 동안 유효한 presigned PUT URL.
- `Content-Type`, `Cache-Control: public, max-age=31536000, immutable`, `If-None-Match: *` 요청 헤더.
- 서버가 만든 `content/inbox/{uuid}.{extension}` 객체 키.
- `${CONTENT_CLOUDFRONT_URL}/{objectKey}` 형식의 조회 URL.
- HTTP method와 만료 시각.

원본 파일명은 확장자 검증에만 사용한다. 객체 키는 UUID로 생성하며 presigned URL을 로그나 DB에 저장하지 않는다.

## 구성과 책임

- `ContentImageUploadService`가 파일 메타데이터를 검증하고 객체 키와 응답을 만든다.
- S3 presigner 연동은 외부 의존성으로 분리해 Service가 AWS SDK 타입에 의존하지 않게 한다.
- `CONTENT_BUCKET_NAME`, `CONTENT_CLOUDFRONT_URL`, `AWS_REGION`을 런타임 설정으로 사용한다.
- AWS SDK 기본 credential provider를 사용하며 Access Key 설정은 추가하지 않는다.

## 이미지 블록 연동

기존 `contentBlocks`의 범용 JSON 배열 계약을 유지한다. 이번 작업에서는 아래 이미지 블록이 생성·수정 API를 거쳐 동일하게 저장되고 조회되는지만 검증한다.

```json
{
  "type": "image",
  "url": "https://d19azau1un4t7r.cloudfront.net/content/inbox/{uuid}.webp",
  "altText": "업데이트 화면 예시"
}
```

블록별 스키마 전환, 임의 이미지 URL 차단, 대체 텍스트 정책은 후속 범위로 둔다.

## 오류와 검증

- 지원하지 않는 MIME type이나 확장자, MIME type과 확장자 불일치, 허용 범위를 벗어난 크기는 `400`으로 거부한다.
- `/api/v1/admin/**`의 기존 관리자 권한 검사를 그대로 적용한다.
- 단위 테스트에서 형식·크기·UUID 키·서명 요청 헤더를 검증한다.
- 통합 테스트에서 관리자 발급, 비관리자 거부, OpenAPI 계약, 이미지 블록 저장·조회를 검증한다.
- 배포 후 실제 PUT, CloudFront `200`, `Content-Type`, immutable `Cache-Control`은 런타임 환경에서 확인한다.
