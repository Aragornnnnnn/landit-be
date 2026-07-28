// 프리톡 추천 주제를 저장하고 조회한다.

package com.landit.landitbe.feature.session.repository;

import com.landit.landitbe.feature.session.domain.FreeTalkTopic;
import com.landit.landitbe.shared.domain.ActiveStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 프리톡 추천 주제를 저장하고 조회한다. */
public interface FreeTalkTopicRepository extends JpaRepository<FreeTalkTopic, Long> {

  /** 노출 순서대로 활성 프리톡 주제를 조회한다. */
  List<FreeTalkTopic> findAllByStatusOrderByDisplayOrderAsc(ActiveStatus status);

  /** 활성 상태인 프리톡 주제를 ID로 조회한다. */
  Optional<FreeTalkTopic> findByIdAndStatus(Long id, ActiveStatus status);
}
