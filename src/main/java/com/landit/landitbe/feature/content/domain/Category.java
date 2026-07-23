// 시나리오를 묶는 카테고리의 공통 정보를 저장한다.

package com.landit.landitbe.feature.content.domain;

import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 시나리오를 묶는 카테고리의 공통 정보를 저장한다. */
@Entity
@Table(name = "category")
public class Category extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ActiveStatus status;

  /** 동작을 수행한다. */
  protected Category() {}
}
