// 사용자당 하나인 무료 시나리오 예약을 저장한다.

package com.landit.landitbe.feature.subscription.repository;

import com.landit.landitbe.feature.subscription.domain.FreeScenarioReservation;
import org.springframework.data.jpa.repository.JpaRepository;

/** 첫 무료 예약 저장소다. */
public interface FreeScenarioReservationRepository
    extends JpaRepository<FreeScenarioReservation, Long> {}
