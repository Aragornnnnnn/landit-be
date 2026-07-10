# ECS 배포 검증 타임아웃 조정 기록

- GitHub Actions job `86416268649`에서 ECS deployment는 검증 시작 약 5분 8초 뒤 `COMPLETED`가 됐다.
- workflow의 `timeout-minutes: 5`가 다음 polling 전에 Job을 종료해 실제 배포 성공을 실패로 기록했다.
- 검증 스크립트는 실패 조건을 즉시 감지하므로 외부 Job 제한만 10분으로 늘린다.
- `MAX_ATTEMPTS=30`, `POLL_INTERVAL_SECONDS=10` 설정은 유지한다.
- `bash -n .github/scripts/verify-ecs-deployment.sh`와 Bash mock 테스트가 통과했다.
- `./gradlew test --no-daemon`은 `BUILD SUCCESSFUL`로 통과했다.
