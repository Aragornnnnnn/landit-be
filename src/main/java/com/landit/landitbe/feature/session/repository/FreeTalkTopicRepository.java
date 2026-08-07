// 프리톡 추천 주제를 저장하고 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.FreeTalkTopic;
import com.landit.landitbe.shared.domain.ActiveStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 프리톡 추천 주제를 저장하고 조회한다. */
public interface FreeTalkTopicRepository extends JpaRepository<FreeTalkTopic, Long> {

  /**
   * 상태가 일치하는 프리톡 주제를 노출 순서대로 조회한다.
   *
   * @param status 조회할 주제 상태
   * @return 조건에 맞는 프리톡 주제 목록
   */
  List<FreeTalkTopic> findAllByStatusOrderByDisplayOrderAsc(ActiveStatus status);

  /**
   * 상태가 일치하는 프리톡 주제를 ID로 조회한다.
   *
   * @param id 프리톡 주제 ID
   * @param status 조회할 주제 상태
   * @return 조건에 맞는 프리톡 주제, 없으면 빈 Optional
   */
  Optional<FreeTalkTopic> findByIdAndStatus(Long id, ActiveStatus status);
}
