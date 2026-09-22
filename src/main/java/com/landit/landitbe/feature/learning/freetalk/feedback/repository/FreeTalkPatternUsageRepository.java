// 실수 패턴 사용례를 저장하고 조회한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.repository;

import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkPatternUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 실수 패턴 사용례를 저장하고 조회한다. 사용례는 교정과 같은 트랜잭션에서 삽입만 하고 고치지 않는다. */
@Repository
public interface FreeTalkPatternUsageRepository extends JpaRepository<FreeTalkPatternUsage, Long> {}
