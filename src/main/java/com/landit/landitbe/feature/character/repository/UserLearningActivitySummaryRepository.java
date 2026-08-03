// 사용자별 누적 학습 요약을 조회하고 저장한다.

package com.landit.landitbe.feature.character.repository;

import com.landit.landitbe.feature.character.domain.UserLearningActivitySummary;
import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자별 누적 학습 요약을 조회하고 저장한다. */
public interface UserLearningActivitySummaryRepository
    extends JpaRepository<UserLearningActivitySummary, Long> {}
