# LAN-269 구현 계획

1. 두 CloudFront URL 문서의 행 수, ID 범위, 중복, 경로를 검증한다.
2. PR #75의 표현학습 SQL을 V44로 포팅하고 새 URL 320개를 교체한다.
3. 같은 V44에 시나리오 썸네일 URL 40개 갱신과 identity sequence 보정을 추가한다.
4. migration 고정 매핑 테스트와 전체 Gradle check를 실행한다.
5. 독립 리뷰에서 데이터 누락과 PostgreSQL 위험을 확인한 뒤 커밋한다.

## 검증 기록

- 입력 문서 검사: 썸네일 40개, 연습 예문 320개, 중복 0개, ID/경로 오류 0개.
- 원본 매핑 SHA-256: 썸네일 `c606f53a...e1ecd`, 연습 예문 `12c6aa47...c093` 일치.
- `./gradlew test --tests com.landit.landitbe.Lan269ContentMigrationTests --no-daemon`: 통과.
- `./gradlew check --rerun-tasks --no-daemon`: 통과.
- 독립 리뷰: V44 순서, 잠금, 충돌 가드, URL 매핑, sequence 보정에 blocker 없음.
- 로컬 PostgreSQL/Docker가 없어 PostgreSQL에 V44를 직접 적용하는 검증은 실행하지 못했다.
