# 구·신 AI 배포 호환성 검증.

AI 저장소의 설치된 가상환경과 해당 버전의 테스트 도우미가 필요하다. 실제 AI 라우터와 BE RemoteAiConversationClient를 연결하고 외부 LLM만 대역으로 바꾼다. fixture는 로컬 테스트 전용이며 운영 서버에서 실행하지 않는다.

```sh
LAN474_AI_ROOT=/path/to/landit-ai /path/to/ai/.venv/bin/python -m uvicorn compatibility_ai_fixture:app --app-dir scripts/integration --host 127.0.0.1 --port 7814
LAN474_AI_URL=http://127.0.0.1:7814 LAN474_EXPECT_SNAPSHOT=true ./gradlew test --tests '*AiDeploymentCompatibilityIntegrationTest' --rerun-tasks
```

구 AI origin/main을 별도 디렉터리에 추출한 뒤 같은 fixture를 실행하고 `LAN474_EXPECT_SNAPSHOT=false`로 검사한다. 구 AI는 캐시 유실 후 메시지 평가를 다시 요청하면 최종 조회가 성공해야 한다. 새 AI는 완성 결과를 요청에 제공하면 캐시 없이도 성공해야 한다.

전체 `check`는 외부 서버 없이 실행하며 이 테스트 하나만 조건부 제외한다. DB 복구·동시 실행·제한 시간은 일반 통합 테스트에서 따로 검증한다. 이 검사는 실제 ECS 교체, PostgreSQL 운영 DB, FE 실기기 검증을 대신하지 않는다.
