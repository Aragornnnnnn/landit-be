// 프로덕션 프로필에서 OpenAPI 문서 엔드포인트가 비활성화되는지 검증한다.

package com.landit.landitbe;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 프로덕션 프로필에서 OpenAPI 문서와 Swagger UI의 노출 여부를 검증한다. */
@ActiveProfiles({"prod", "test"})
@SpringBootTest
@AutoConfigureMockMvc
class ProductionOpenApiDocsDisabledIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Test
  void openApiDocsRootIsNotFound() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
  }

  @Test
  void openApiDocsSubPathIsNotFound() throws Exception {
    mockMvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isNotFound());
  }

  @Test
  void swaggerUiIsNotFound() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
  }

  @Test
  void actuatorDiscoveryIsNotFound() throws Exception {
    mockMvc.perform(get("/actuator")).andExpect(status().isNotFound());
  }

  @Test
  void actuatorInfoIsNotFound() throws Exception {
    mockMvc.perform(get("/actuator/info")).andExpect(status().isNotFound());
  }

  @Test
  void actuatorHealthRemainsAvailable() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
