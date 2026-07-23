// 프리톡 세션에만 필요한 정보를 저장한다.

package com.landit.landitbe.feature.session.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 프리톡 세션에만 필요한 정보를 저장한다. */
@Entity
@Table(name = "free_talk_session")
public class FreeTalkSession extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "learning_session_id", nullable = false)
  private Long learningSessionId;

  @Column(length = 255)
  private String title;

  /** JPA에서 사용하는 기본 생성자다. */
  protected FreeTalkSession() {}
}
