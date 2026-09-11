# 결제 ON 통합 테스트.

수정하지 않은 FE API·훅을 실제 BE·AI HTTP 서버와 연결한다. H2 테스트 DB만 사용한다. LLM·스토어 브릿지·라우터 이동은 대역이며 실제 결제나 운영 배포를 하지 않는다.

정상 시나리오 5개, 웹훅 지연 시 재차단 1개, **현재 FE 오류를 재현하는 시나리오 3개**다. `9 passed`는 결제 ON이 모든 상황에서 안전하다는 뜻이 아니다. 판정과 수정 내용은 [LAN-474 계획](../../docs/tasks/LAN-474/plan.md)에 기록했다.

확인한 코드: FE develop `c62be093`, AI `66dc226`와 BE `feat/LAN-474`. FE 체크아웃은 읽기만 한다. FE 의존성은 해당 저장소의 lockfile로 미리 설치한다. Node와 Java 21, Gradle, AI의 Python 가상 환경이 필요하다.

BE 저장소 루트에서 별도 터미널로 테스트 AI 서버를 실행한다. 아래 경로는 각자의 로컬 체크아웃 절대 경로로 바꾼다.

```sh
export LAN474_AI_ROOT=/absolute/path/to/landit-ai
/path/to/ai/.venv/bin/python -m uvicorn ai_payment_fixture:app \
  --app-dir scripts/integration --host 127.0.0.1 --port 18974
```

다른 터미널의 BE 저장소 루트에서 실행한다.

```sh
export LAN474_FE_ROOT=/absolute/path/to/landit-fe
export LAN474_AI_URL=http://127.0.0.1:18974
./gradlew test --rerun --tests '*PaymentOnCrossStackIntegrationTests' \
  --tests '*RemoteAiConversationClientTest'
./gradlew check
```

외부 FE 파일은 Gradle 입력에 포함되지 않으므로 통합 테스트를 다시 실행할 때는 `--rerun`을 유지한다.

FE 세부 결과는 `build/payment-on-fe.log`, BE 결과는 `build/reports/tests/test/index.html`에 남는다. `LAN474_FE_ROOT`가 없으면 외부 체크아웃이 필요한 테스트만 생략한다. 테스트가 끝나면 첫 터미널의 AI 서버를 Ctrl-C로 종료한다.

AI 전체 회귀 테스트는 AI 저장소에서 `.venv/bin/python -m unittest discover -s tests`로 실행한다. 외부 LLM은 호출하지 않는다.
