// 시나리오 엔티티를 PK 기준으로 조회한다.

package com.landit.landitbe.feature.content.infrastructure;

import com.landit.landitbe.feature.content.domain.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

/** 시나리오 엔티티를 PK 기준으로 조회한다. */
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {}
