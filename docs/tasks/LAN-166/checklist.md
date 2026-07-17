# LAN-166 Checklist

## 구현

- [x] BE의 native score 기반 별점 재계산을 제거한다.
- [x] AI가 반환한 유효한 별점을 세션 결과와 시나리오 진행도에 그대로 저장한다.

## 검증

- [x] native score 구간과 달라도 AI 별점이 저장·응답되는지 검증한다.
- [x] 지원하지 않는 AI 별점을 거부하는지 검증한다.
- [x] `./gradlew check`를 실행한다.
- [x] `git diff --check`를 실행한다.
