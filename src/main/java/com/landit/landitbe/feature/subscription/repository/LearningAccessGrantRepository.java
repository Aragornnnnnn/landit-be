// 사용자와 학습 대상에 한정된 완료 권한을 조회한다.

package com.landit.landitbe.feature.subscription.repository;

import com.landit.landitbe.feature.subscription.domain.LearningAccessGrant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 학습 완료 권한 저장소다. */
public interface LearningAccessGrantRepository extends JpaRepository<LearningAccessGrant, String> {
  /** 대상의 가장 최근 시도만 조회한다. */
  Optional<LearningAccessGrant> findTopByUserIdAndKindAndTargetIdOrderByStartedAtDesc(
      long userId, String kind, long targetId);
}
