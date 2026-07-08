# 작업별 노트

이 디렉터리는 이슈별 작업 기록을 보관합니다.

- Notion 이슈가 있는 작업은 `docs/tasks/{ISSUE_NUMBER}/checklist.md`와 `docs/tasks/{ISSUE_NUMBER}/context-notes.md`를 사용합니다.
- 이슈 번호 없이 `origin/develop` 직접 작업을 명시받은 예외 작업은 `docs/tasks/direct-{YYYY-MM-DD}-{short-slug}/`를 사용합니다.
- 새 작업은 루트의 `checklist.md`, `context-notes.md`에 기록하지 않습니다. 두 파일은 과거 누적 로그로만 둡니다.
- 병렬 브랜치 충돌을 줄이기 위해 하나의 작업은 자기 이슈 디렉터리 안의 파일만 갱신합니다.
