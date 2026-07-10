# ECS deployment verification implementation plan

테스트를 먼저 작성해 성공, nullable failedTasks, deployment 실패, 컨테이너 종료, 이전 태스크 무시, 타임아웃을 검증한다. 이후 Bash 스크립트와 workflow 호출을 추가하고 Bash 구문·mock 테스트·Gradle 테스트를 실행한다.
