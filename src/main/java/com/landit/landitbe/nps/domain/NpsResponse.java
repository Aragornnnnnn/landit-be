// 사용자의 NPS 응답을 저장하는 Entity
package com.landit.landitbe.nps.domain;

import com.landit.landitbe.common.domain.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  protected NpsResponse() {}

  public NpsResponse(Long userProfileId, int score, String opinionText) {
    this.userProfileId = userProfileId;
    this.score = score;
    this.opinionText = opinionText;
  }
}
