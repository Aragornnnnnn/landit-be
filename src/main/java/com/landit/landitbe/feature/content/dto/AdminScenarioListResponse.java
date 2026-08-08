// 관리자 시나리오 테스트 목록의 응답 구조를 정의한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.feature.content.dto.ScenarioListResponse.OpeningPreviewResponse;
import com.landit.landitbe.feature.content.repository.projection.ScenarioListProjection;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 시나리오 테스트 목록의 응답 구조를 정의한다.
 *
 * @param categories 카테고리 목록
 */
public record AdminScenarioListResponse(List<CategoryResponse> categories) {

  /**
   * 조회 projection 목록을 카테고리별 관리자 응답으로 변환한다.
   *
   * @param rows 시나리오 목록 조회 결과
   * @return 카테고리별 관리자 시나리오 목록 응답
   */
  public static AdminScenarioListResponse from(List<ScenarioListProjection> rows) {
    // 조회 정렬을 유지하면서 같은 카테고리의 시나리오를
    // 하나의 응답 그룹으로 묶는다.
    Map<Long, CategoryResponseBuilder> builders = new LinkedHashMap<>();
    for (ScenarioListProjection row : rows) {
      CategoryResponseBuilder builder =
          builders.computeIfAbsent(
              row.categoryId(),
              ignored ->
                  new CategoryResponseBuilder(
                      row.categoryId(), row.categoryName(), row.categoryDisplayOrder()));
      builder.scenarios().add(ScenarioResponse.from(row));
    }

    return new AdminScenarioListResponse(
        builders.values().stream().map(CategoryResponseBuilder::build).toList());
  }

  /**
   * 관리자 시나리오 테스트 목록의 카테고리 응답이다.
   *
   * @param categoryId 카테고리 ID
   * @param categoryName 카테고리 이름
   * @param displayOrder 카테고리 노출 순서
   * @param scenarios 시나리오 목록
   */
  public record CategoryResponse(
      Long categoryId, String categoryName, int displayOrder, List<ScenarioResponse> scenarios) {}

  /**
   * 관리자 시나리오 테스트 목록의 시나리오 응답이다.
   *
   * @param scenarioId 시나리오 ID
   * @param displayOrder 시나리오 노출 순서
   * @param scenarioTitle 시나리오 제목
   * @param briefing 시나리오 설명
   * @param conversationGoal 대화 목표
   * @param difficulty 시나리오 난이도
   * @param firstSpeaker 첫 발화자
   * @param thumbnailUrl 썸네일 URL
   * @param openingPreview 시작 화면 미리보기
   */
  public record ScenarioResponse(
      Long scenarioId,
      int displayOrder,
      String scenarioTitle,
      String briefing,
      String conversationGoal,
      String difficulty,
      String firstSpeaker,
      String thumbnailUrl,
      OpeningPreviewResponse openingPreview) {

    /**
     * 시나리오 projection을 관리자 테스트 응답으로 변환한다.
     *
     * @param row 시나리오 목록 조회 결과
     * @return 관리자 시나리오 응답
     */
    public static ScenarioResponse from(ScenarioListProjection row) {
      OpeningPreviewResponse openingPreview =
          row.firstSpeaker() == ConversationSpeaker.AI
              ? OpeningPreviewResponse.fromAi(row)
              : OpeningPreviewResponse.fromUser(row);

      return new ScenarioResponse(
          row.scenarioId(),
          row.scenarioDisplayOrder(),
          row.scenarioTitle(),
          row.briefing(),
          row.conversationGoal(),
          row.difficulty().name(),
          row.firstSpeaker().name(),
          row.thumbnailUrl(),
          openingPreview);
    }
  }

  private record CategoryResponseBuilder(
      Long categoryId, String categoryName, int displayOrder, List<ScenarioResponse> scenarios) {

    private CategoryResponseBuilder(Long categoryId, String categoryName, int displayOrder) {
      this(categoryId, categoryName, displayOrder, new ArrayList<>());
    }

    private CategoryResponse build() {
      return new CategoryResponse(categoryId, categoryName, displayOrder, List.copyOf(scenarios));
    }
  }
}
