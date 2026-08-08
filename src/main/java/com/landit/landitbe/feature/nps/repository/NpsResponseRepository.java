// NPS 응답 엔티티를 저장하는 Repository다.

package com.landit.landitbe.feature.nps.repository;

import com.landit.landitbe.feature.nps.domain.NpsResponse;
import com.landit.landitbe.feature.nps.repository.projection.AdminNpsResponseProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** NPS 응답 엔티티를 저장하는 Repository다. */
public interface NpsResponseRepository extends JpaRepository<NpsResponse, Long> {

  /**
   * NPS 응답과 작성자 정보를 최신순 Slice로 조회한다.
   *
   * @param pageable 페이지 요청
   * @return NPS 응답과 작성자 정보
   */
  @Query(
      """
      SELECT new com.landit.landitbe.feature.nps.repository.projection.AdminNpsResponseProjection(
          response.id,
          response.score,
          response.opinionText,
          response.createdAt,
          profile.id,
          profile.email,
          profile.nickname)
      FROM NpsResponse response
      JOIN UserProfile profile ON profile.id = response.userProfileId
      ORDER BY response.createdAt DESC, response.id DESC
      """)
  Slice<AdminNpsResponseProjection> findAdminResponses(Pageable pageable);
}
