# ECS deployment verification design

`deploy-dev.yml`은 Docker 빌드 전에 Gradle 테스트를 수행하고, 새 PRIMARY deployment ID와 생성 시각을 검증 스크립트로 전달한다. 스크립트는 10초 간격으로 최대 5분 동안 해당 deployment와 이후 시작된 태스크만 판별하며, 실패 시 서비스·이벤트·태스크 진단을 출력한다.
