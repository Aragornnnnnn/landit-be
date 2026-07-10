# Apple Social Login Nickname Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apple 소셜 로그인 요청의 선택적 `nickname`을 사용자 프로필 이름으로 반영한다.

**Architecture:** 요청 DTO에 nullable `nickname`을 추가한다. 인증 서비스는 ID Token을 기존 방식으로 검증한 후, Apple이고 요청 nickname이 비어 있지 않을 때만 검증 결과의 nickname을 대체하여 기존 사용자 생성·갱신 흐름에 전달한다.

**Tech Stack:** Java 21, Spring Boot 4, JUnit 5, MockMvc, H2.

## Global Constraints

- API 필드명은 기존 도메인 용어와 일치하는 `nickname`을 사용한다.
- `nickname`은 nullable이며 Apple 외 제공자의 기존 ID Token 기반 처리에는 영향을 주지 않는다.
- Apple 요청의 비어 있지 않은 `nickname`만 사용자 프로필 nickname으로 사용한다.
- 신규 Apple 요청의 nickname이 없으면 `Guest`를 저장하고, 기존 Apple 사용자의 nickname은 갱신하지 않는다.
- 새 소스 파일을 만들지 않고 기존 파일을 최소 변경한다.

---

### Task 1: Apple 요청 nickname 우선 처리

**Files:**
- Modify: `src/test/java/com/landit/landitbe/auth/SocialAuthApiIntegrationTests.java`
- Modify: `src/main/java/com/landit/landitbe/auth/api/dto/SocialLoginRequest.java`
- Modify: `src/main/java/com/landit/landitbe/auth/application/AuthService.java`
- Modify: `docs/tasks/LAN-112/checklist.md`
- Modify: `docs/tasks/LAN-112/context-notes.md`

**Interfaces:**
- Consumes: `SocialLoginRequest(String provider, String idToken, String nonce, String nickname)`.
- Produces: Apple의 비어 있지 않은 요청 `nickname`을 포함한 `OidcUserInfo`가 `findOrCreateUser`로 전달된다.

- [x] **Step 1: Apple nickname 우선 처리의 실패 테스트를 작성한다.**

```java
@Test
void socialLoginUsesRequestNicknameForApple() throws Exception {
    mockMvc.perform(post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "provider":"APPLE",
                              "idToken":"apple-sub-2|apple-nickname@example.com|Id Token Name|apple-nonce",
                              "nonce":"apple-nonce",
                              "nickname":"Apple Request Name"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.nickname").value("Apple Request Name"));
}
```

- [x] **Step 2: 대상 테스트가 기능 부재로 실패하는지 확인한다.**

Run: `./gradlew test --tests com.landit.landitbe.auth.SocialAuthApiIntegrationTests.socialLoginUsesRequestNicknameForApple`

Expected: 요청 `nickname`이 아직 DTO에 없으므로 응답 nickname이 `Id Token Name`이라 assertion failure.

- [x] **Step 3: 요청 DTO와 인증 서비스에 최소 구현을 추가한다.**

```java
public record SocialLoginRequest(
        @NotBlank String provider,
        @NotBlank String idToken,
        String nonce,
        String nickname
) {
}
```

```java
OidcUserInfo userInfo = oidcTokenVerifier.verify(provider, request.idToken(), request.nonce());
if (provider == SocialProvider.APPLE && request.nickname() != null && !request.nickname().isBlank()) {
    userInfo = new OidcUserInfo(provider, userInfo.sub(), userInfo.email(), request.nickname());
}
```

- [x] **Step 4: 대상 테스트가 통과하는지 확인한다.**

Run: `./gradlew test --tests com.landit.landitbe.auth.SocialAuthApiIntegrationTests.socialLoginUsesRequestNicknameForApple`

Expected: PASS.

- [x] **Step 5: nullable 및 비-Apple 회귀 테스트를 추가하고 실행한다.**

```java
@Test
void socialLoginKeepsIdTokenNicknameWhenAppleNicknameIsMissing() throws Exception {
    mockMvc.perform(post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "provider":"APPLE",
                              "idToken":"apple-sub-3|apple-fallback@example.com|Id Token Name|apple-nonce",
                              "nonce":"apple-nonce"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.nickname").value("Id Token Name"));
}

@Test
void socialLoginIgnoresRequestNicknameForGoogle() throws Exception {
    mockMvc.perform(post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "provider":"GOOGLE",
                              "idToken":"google-sub-4|google-nickname@example.com|Id Token Name|google-nonce",
                              "nonce":"google-nonce",
                              "nickname":"Ignored Request Name"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.nickname").value("Id Token Name"));
}
```

Run: `./gradlew test --tests com.landit.landitbe.auth.SocialAuthApiIntegrationTests`

Expected: PASS.

- [x] **Step 6: 작업 기록을 완료 상태로 갱신하고 커밋한다.**

```bash
git add src/main/java/com/landit/landitbe/auth/api/dto/SocialLoginRequest.java \
  src/main/java/com/landit/landitbe/auth/application/AuthService.java \
  src/test/java/com/landit/landitbe/auth/SocialAuthApiIntegrationTests.java \
  docs/tasks/LAN-112
git commit -m "feat: Apple 소셜 로그인 nickname 지원"
```
