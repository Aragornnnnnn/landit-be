// 관리자 NPS 목록의 페이지 응답 구조를 정의한다.

package com.landit.landitbe.feature.nps.dto;

import com.landit.landitbe.feature.nps.repository.projection.AdminNpsResponseProjection;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Slice;

/**
 * 관리자 NPS 목록의 페이지 응답 구조를 정의한다.
 *
 * @param items NPS 응답 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param hasNext 다음 페이지 존재 여부
 */
public record AdminNpsResponsePage(List<Item> items, int page, int size, boolean hasNext) {

  /**
   * NPS projection 목록을 페이지 응답으로 변환한다.
   *
   * @param responses NPS 조회 결과
   * @param page 현재 페이지 번호
   * @param size 페이지 크기
   * @return 관리자 NPS 목록 페이지 응답
   */
  public static AdminNpsResponsePage from(
      Slice<AdminNpsResponseProjection> responses, int page, int size) {
    List<Item> items = responses.getContent().stream().map(Item::from).toList();

    return new AdminNpsResponsePage(items, page, size, responses.hasNext());
  }

  /**
   * 관리자 NPS 목록의 항목이다.
   *
   * @param npsResponseId NPS 응답 ID
   * @param score 점수
   * @param opinionText 의견
   * @param submittedAt 제출 시각
   * @param userProfileId 사용자 프로필 ID
   * @param userEmail 사용자 이메일
   * @param userNickname 사용자 닉네임
   */
  public record Item(
      Long npsResponseId,
      int score,
      String opinionText,
      LocalDateTime submittedAt,
      Long userProfileId,
      String userEmail,
      String userNickname) {

    /** 조회 projection을 API 항목으로 변환한다. */
    private static Item from(AdminNpsResponseProjection response) {
      return new Item(
          response.npsResponseId(),
          response.score(),
          response.opinionText(),
          response.submittedAt(),
          response.userProfileId(),
          response.userEmail(),
          response.userNickname());
    }
  }
}
