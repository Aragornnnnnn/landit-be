// OpenAPI 문서 응답의 브라우저 렌더링 인코딩을 검증한다.

package com.landit.landitbe;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** OpenAPI 문서 응답의 브라우저 렌더링 인코딩을 검증한다. */
@ActiveProfiles({"develop", "test"})
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsEncodingTests {

  @Autowired private MockMvc mockMvc;

  @DisplayName("OpenAPI 문서 응답에 UTF-8 문자셋을 명시한다.")
  @Test
  void openApiDocsDeclareUtf8Charset() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", containsString("charset=UTF-8")));
  }

  @DisplayName("프리톡 중첩 응답 스키마는 서로 다른 컴포넌트 이름을 사용한다.")
  @Test
  void freeTalkNestedResponseSchemasHaveDistinctComponentNames() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.components.schemas.FreeTalkSessionStartResponse.properties"
                        + ".currentMessage.$ref")
                .value("#/components/schemas/FreeTalkCurrentMessageResponse"))
        .andExpect(
            jsonPath(
                    "$.components.schemas.FreeTalkMessageSubmitResponse.properties"
                        + ".submittedMessage.$ref")
                .value("#/components/schemas/FreeTalkSubmittedMessageResponse"))
        .andExpect(
            jsonPath(
                    "$.components.schemas.FreeTalkMessageSubmitResponse.properties"
                        + ".nextMessage.$ref")
                .value("#/components/schemas/FreeTalkNextMessageResponse"))
        .andExpect(jsonPath("$.components.schemas.FreeTalkCurrentMessageResponse").exists())
        .andExpect(
            jsonPath("$.components.schemas.FreeTalkCurrentMessageResponse.properties.emotion")
                .exists())
        .andExpect(
            jsonPath("$.components.schemas.FreeTalkCurrentMessageResponse.properties.innerThought")
                .doesNotExist())
        .andExpect(jsonPath("$.components.schemas.FreeTalkSubmittedMessageResponse").exists())
        .andExpect(
            jsonPath(
                    "$.components.schemas.FreeTalkSubmittedMessageResponse.properties"
                        + ".innerThoughtProcessingStatus")
                .exists())
        .andExpect(
            jsonPath(
                    "$.components.schemas.FreeTalkSubmittedMessageResponse.properties"
                        + ".feedbackProcessingStatus")
                .doesNotExist())
        .andExpect(jsonPath("$.components.schemas.FreeTalkNextMessageResponse").exists())
        .andExpect(
            jsonPath("$.components.schemas.FreeTalkNextMessageResponse.properties.emotion")
                .exists());
  }

  @DisplayName("공용 캐릭터 스키마에 nullable TTS 음성 계약을 명시한다.")
  @Test
  void conversationCharacterSchemaDocumentsSharedNullableTtsVoiceContract() throws Exception {
    String schemas = "$.components.schemas.";

    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(schemas + "SessionStartResponse.properties.character.$ref")
                .value("#/components/schemas/ConversationCharacterResponse"))
        .andExpect(
            jsonPath(schemas + "FreeTalkSessionStartResponse.properties.character.$ref")
                .value("#/components/schemas/ConversationCharacterResponse"))
        .andExpect(
            jsonPath(schemas + "ConversationCharacterResponse.properties.ttsVoice.type[0]")
                .value("object"))
        .andExpect(
            jsonPath(schemas + "ConversationCharacterResponse.properties.ttsVoice.type[1]")
                .value("null"));
  }
}
