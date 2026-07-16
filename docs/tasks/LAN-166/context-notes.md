# LAN-166 Context Notes

## 결정

- 최종 별점 정책은 메시지별 GOOD 비율과 원시 품질 점수를 함께 계산하는 AI 서버가 소유한다.
- BE는 AI 별점을 native score 구간으로 다시 계산하지 않고, 허용된 별점 값만 검증한 뒤 저장한다.
