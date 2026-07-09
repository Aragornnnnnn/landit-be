# LAN-62 PR 충돌 해소 계획

## 2026-07-09

1. `feat/LAN-62`를 최신 `origin/develop` 위로 rebase한다.
   - 검증은 rebase 충돌 파일 확인과 `git diff --check`로 한다.
2. develop에 새로 들어온 Flyway migration, 문서, 보안 설정 변경과 LAN-62 변경이 충돌하면 실제 파일 기준으로 병합한다.
   - 검증은 관련 통합 테스트와 전체 테스트로 한다.
3. 충돌 해소 후 `feat/LAN-62`를 `--force-with-lease`로 push한다.
   - 검증은 PR #3의 mergeable 상태 재조회로 한다.
4. LAN-91 후속 PR 스택은 LAN-62 push 후 아래 브랜치부터 차례대로 rebase한다.
   - 검증은 각 브랜치 rebase 완료와 PR #5, #6, #7 mergeable 상태 재조회로 한다.
