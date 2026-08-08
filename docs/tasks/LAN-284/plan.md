# LAN-284 개발 EC2 미러 배포 구현 계획

1. fake AWS CLI 테스트로 EC2 입력 검증, SSM 명령, 상태 polling, 실패 상태를 고정한다.
2. 동일한 40자 커밋 SHA를 API 서비스의 EC2 배포 스크립트에 전달한다.
3. ECS 안정화 검증 뒤에 EC2 미러 단계를 추가해, EC2 실패가 workflow 실패로 이어지게 한다.
4. shell 테스트와 전체 Gradle check를 실행하고 결과를 기록한다.

## 검증 기록

- RED: `bash .github/scripts/test/deploy-ec2-service_test.sh`가 helper 부재로 실패하는 것을 확인했다.
- GREEN: `bash .github/scripts/test/deploy-ec2-service_test.sh`와 `bash -n .github/scripts/deploy-ec2-service.sh .github/scripts/test/deploy-ec2-service_test.sh`가 통과했다.
- 전체: `bash .github/scripts/test/deploy-ec2-service_test.sh && ./gradlew check --rerun-tasks --no-daemon`가 통과했다.
- GitHub Environment variable 생성과 AWS 리소스 변경은 이 작업 범위에서 실행하지 않았다.
