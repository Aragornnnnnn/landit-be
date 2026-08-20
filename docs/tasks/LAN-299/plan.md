# LAN-299 공지 이미지 업로드 구현 계획

상세 API 계약과 범위는 [설계 문서](design.md)를 기준으로 한다.

## 구현 순서

- [x] AWS SDK와 런타임 설정을 추가하고 S3 presigner 어댑터를 구현한다.
- [x] JPEG·PNG·WebP 형식과 10 MiB 요청 크기를 검증한다.
- [x] 관리자 발급 API와 OpenAPI 문서를 추가한다.
- [x] UUID 객체 키, 서명 헤더, 이미지 블록 저장·조회를 검증한다.
- [x] 전체 포맷, Checkstyle, 테스트를 실행한다.

## 검증 결과

- 2026-08-12 `./gradlew check --rerun-tasks` 성공.
- 발급 지연이 응답 만료 시각을 늘리지 않도록 요청 시작 시각을 기준으로 계산한다.
- 반복 발급 UUID, S3 서명 헤더 값, 이미지 블록 DB 왕복을 회귀 테스트한다.
- 실제 S3 PUT과 CloudFront 응답은 배포된 API의 ECS Task Role로 확인한다.
