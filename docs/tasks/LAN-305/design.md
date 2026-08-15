<!-- LAN-305 답장 원본 피드백 응답 설계 문서 -->
# LAN-305 답장 원본 피드백 응답 설계

## 목표와 설계

- 받은 편지 상세 응답에 `feedbackType`, `quotedFeedbackContent`를 추가한다.
- `REPLY`는 수신 행의 `representative_feedback_id`로 사용자 소유 피드백을 조회해 값을 채운다.
- `NOTICE`, `UPDATE`는 두 필드를 `null`로 반환한다.
- 수신자 검증이 끝난 뒤 연결 피드백을 조회해 다른 사용자의 피드백이 노출되지 않도록 한다.

## 검증

- 답장 상세의 유형과 인용 내용, 전역 편지의 null 값, OpenAPI 스키마를 통합 테스트한다.

