# LAN-284 개발 EC2 미러 배포 구현 계획

1. fake AWS CLI 테스트로 EC2 입력 검증, SSM 명령, 상태 polling, 실패 상태를 고정한다.
2. 동일한 40자 커밋 SHA를 API 서비스의 EC2 배포 스크립트에 전달한다.
3. ECS 안정화 검증 뒤에 EC2 미러 단계를 추가해, EC2 실패가 workflow 실패로 이어지게 한다.
4. shell 테스트와 전체 Gradle check를 실행하고 결과를 기록한다.

## 검증 기록

- RED: `bash .github/scripts/test/deploy-ec2-service_test.sh`가 helper 부재로 실패하는 것을 확인했다.
- GREEN: `bash .github/scripts/test/deploy-ec2-service_test.sh`와 `bash -n .github/scripts/deploy-ec2-service.sh .github/scripts/test/deploy-ec2-service_test.sh`가 통과했다.
- 전체: `bash .github/scripts/test/deploy-ec2-service_test.sh && ./gradlew check --rerun-tasks --no-daemon`가 통과했다.
- IaC 적용으로 개발 EC2와 전용 SSM 문서를 생성했고, BE `develop` Environment에 `EC2_INSTANCE_ID`를 등록했다. 임시 도메인의 HTTPS와 API→AI 내부 health도 확인했다.
- 최신 `develop` 기준 통합 검증에서 shell test와 `./gradlew check --rerun-tasks --no-daemon`가 통과했다. 배포 workflow는 Flyway migration 성공 뒤 ECS를 검증하고 같은 SHA를 EC2에 전달한다.
- 이 PR 병합 후 `workflow_dispatch`로 실제 재배포를 검증한다. 기존 ECS·ALB는 해당 검증과 개발 DNS 전환이 끝날 때까지 유지하며, 제거 전 EC2 전용 workflow에서도 Flyway migration 선행 순서를 보존한다.
