// 사용자의 NPS 응답을 저장하는 Entity

package com.landit.landitbe.feature.nps.domain;

import com.landit.landitbe.shared.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 사용자의 NPS 응답을 저장하는 Entity. */
@Entity
@Table(name = "nps_response")
public class NpsResponse extends BaseCreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(nullable = false)
  private int score;

  @Column(name = "opinion_text", columnDefinition = "text")
  private String opinionText;

  /** JPA에서 사용하는 기본 생성자다. */
  protected NpsResponse() {}

  /**
   * 사용자의 NPS 응답을 생성한다.
   *
   * @param userProfileId 응답 사용자 ID
   * @param score NPS 점수
   * @param opinionText 선택 의견
   */
  public NpsResponse(Long userProfileId, int score, String opinionText) {
    this.userProfileId = userProfileId;
    this.score = score;
    this.opinionText = opinionText;
  }
}
