// NPS 응답 엔티티를 저장하는 Repository다.

package com.landit.landitbe.feature.nps.repository;

import com.landit.landitbe.feature.nps.domain.NpsResponse;
import org.springframework.data.jpa.repository.JpaRepository;

/** NPS 응답 엔티티를 저장하는 Repository다. */
public interface NpsResponseRepository extends JpaRepository<NpsResponse, Long> {}
