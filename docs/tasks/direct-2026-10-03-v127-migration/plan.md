# V127 배포 실패 수정.

## 승인 범위.

- 사용자가 배포 실패 수정 후 `origin/develop` 직접 push와 BE/AI v1.6.0 릴리즈 PR 생성을 요청했다.
- 수정 기준은 develop `4dae48787e78f391dbe1d9ff70913df617cf8415`다.
- 운영 배포나 운영 DB의 직접 수정은 포함하지 않는다.

## 원인과 수정.

- [개발 환경 배포 실패](https://github.com/Aragornnnnnn/landit-be/actions/runs/37035854303)의 V127 사후 검증에서 `V127 expression, embedding or image mapping verification failed`가 발생했다. 해당 마이그레이션은 롤백됐고 API 배포 단계는 실행되지 않았다.
- V73부터 표현 난이도의 DB 계약은 1~5다. V127 데이터에도 난이도 4가 223개, 5가 28개 있지만 사후 검증만 1~3으로 제한돼 있었다.
- 검증 상한만 5로 변경한다. 표현 1,000개, 이미지 URL 3,000개, 임베딩과 다른 보호 조건은 변경하지 않는다.
- V127이 실패해 적용되지 않은 상태이므로 해당 파일을 수정한다. 후속 버전만 추가하면 V127에서 먼저 실패한다. 이미 성공 적용한 별도 환경이 있다면 checksum 상태를 먼저 확인해야 하며 자동 repair는 수행하지 않는다.

## 검증.

- PostgreSQL 15.18과 pgvector 0.8.0을 사용하는 폐기 가능한 로컬 DB에서 수정 전 동일 오류를 재현했다.
- 전용 주소는 `jdbc:postgresql://127.0.0.1:55427/v127_test?user=v127_test`다. `extensions` 스키마에 pgvector를 준비한 뒤 `V127_TEST_POSTGRES_URL`에 이 주소를 지정한다. 테스트는 다른 주소를 거부하고 매번 독립 스키마를 만들고 제거한다.
- 회귀 범위는 1~5 난이도 전체 적재, 기존 행과 앞선 시퀀스 보존, 재실행 건너뛰기, 난이도 0/6·이미지·임베딩 변조의 전체 롤백, ID·표시 순서 충돌 거부다.
- 검증 명령: `V127_TEST_POSTGRES_URL=로컬전용주소 ./gradlew spotlessApply check --offline --console=plain`.
- Java 21에서 전체 check 통과. 테스트 1,960건 중 1,942건 성공, 기존 환경 조건에 따른 18건 제외, 실패·오류 0건이다. 신규 PostgreSQL 7건은 모두 실행·통과했다. 최초 스타일 검사에서 발견한 테스트 코드 줄 길이를 수정하고 최종 check를 통과했다.
- Git 원본과 바이트 비교로 SQL의 난이도 상한 한 글자 외 데이터·구문이 모두 동일함을 확인했다. `git diff --check`도 통과했다.
- 로컬 검증과 원격 CI, 개발 환경 재배포, 운영 적용 결과는 별도로 확인한다.
