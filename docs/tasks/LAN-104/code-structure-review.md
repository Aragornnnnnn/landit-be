# LAN-104 코드 구조 점검

## Ponytail 점검

- `shrink:` `CorsProperties`가 method/header/credential까지 바인딩하던 구조를 제거했다. 환경별로 바뀌는 값은 origin 하나다.
- `delete:` `OPTIONS /** permitAll`은 제거된 상태다. 유효한 preflight는 `http.cors(...)`의 CORS 필터에서 먼저 처리된다.
- `yagni:` CORS 전용 Adapter, Provider, 별도 Service는 추가하지 않았다. Security 설정 bean 하나로 충분하다.

## 구조 판단

- `CorsProperties`는 `LANDIT_CORS_ALLOWED_ORIGINS`만 바인딩한다.
- 기본 method/header/credential 정책은 `AuthSecurityConfig`의 상수로 둔다.
- CORS 설정은 웹 계층 공통 관심사라 `common.web`의 properties와 `auth.security`의 SecurityFilterChain 연결로 충분하다.
- 현재 LAN-104 변경은 새 dependency, 새 abstraction, DB migration, public API 응답 형식 변경 없이 닫혀 있다.

## 남긴 복잡도

- `CorsConfigurationSource` bean은 Spring Security CORS 연결에 필요한 표준 경계라 유지한다.
- 통합 테스트는 인증 필요 API의 preflight 경로를 직접 검증하므로 유지한다.

net: -16 lines applied from the previous LAN-104 CORS shape, 0 deps.
