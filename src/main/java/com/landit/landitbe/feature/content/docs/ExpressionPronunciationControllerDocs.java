// 문장 발화 발음 평가 API의 OpenAPI 문서를 정의한다.

package com.landit.landitbe.feature.content.docs;

import com.landit.landitbe.feature.auth.security.AuthUserPrincipal;
import com.landit.landitbe.feature.content.dto.PronunciationAnalysisResponse;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

/** 문장 발화 발음 평가 API의 OpenAPI 문서를 정의한다. */
@Tag(name = "Expression Pronunciation", description = "원어민 표현 발음 평가 API")
public interface ExpressionPronunciationControllerDocs {

  /**
   * 사용자가 대표 예문을 읽은 음성을 분석해 점수와 단어별 발음·강세 판정, 코칭 문구를 반환한다.
   *
   * <p>판정 기준(참조 TTS·발음 기준 데이터)은 사용자가 선택한 AI 튜터의 억양을 따른다. 사용자 음성은 판정 후 저장하지 않는다. "다시 말하기"는 이 API를
   * 동일하게 재호출하며, 재호출 응답으로 화면 전체를 교체한다.
   *
   * @param principal 인증된 사용자
   * @param expressionId Writing 표현 ID
   * @param audio 사용자 발화 녹음 (m4a·wav·mp3·webm, 최대 10MB·30초)
   * @return 점수·통과 여부·단어별 판정
   * @throws ApiException 오디오 형식·크기 위반(400), 표현 없음·발음 데이터 미구축(404), AI 분석 실패(502)
   */
  @Operation(
      summary = "문장 발화 음성파일 제출",
      description =
          "대표 예문을 읽은 녹음을 분석해 점수와 단어별 발음·강세 판정, 코칭 문구를 반환한다." + " 판정 기준은 사용자의 AI 튜터 억양을 따른다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  ApiResponse<PronunciationAnalysisResponse> analyzeSentence(
      AuthUserPrincipal principal,
      @Parameter(description = "Writing 표현 ID", example = "101") Long expressionId,
      @Parameter(description = "사용자 발화 녹음. m4a·wav·mp3·webm, 최대 10MB·30초") MultipartFile audio);
}
