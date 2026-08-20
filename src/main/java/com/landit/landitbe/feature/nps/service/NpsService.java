// NPS 응답을 저장하고 관리자 목록 조회를 제공한다.

package com.landit.landitbe.feature.nps.service;

import com.landit.landitbe.feature.nps.domain.NpsResponse;
import com.landit.landitbe.feature.nps.dto.AdminNpsResponsePage;
import com.landit.landitbe.feature.nps.dto.NpsSubmitRequest;
import com.landit.landitbe.feature.nps.repository.NpsResponseRepository;
import com.landit.landitbe.feature.nps.repository.projection.AdminNpsResponseProjection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** NPS 응답을 저장하고 관리자 목록 조회를 제공한다. */
@Service
@Slf4j
public class NpsService {

  private final NpsResponseRepository npsResponseRepository;

  /**
   * NPS 응답 저장소를 주입받는다.
   *
   * @param npsResponseRepository NPS 응답 Repository
   */
  public NpsService(NpsResponseRepository npsResponseRepository) {
    this.npsResponseRepository = npsResponseRepository;
  }

  /**
   * 사용자의 만족도 점수와 선택 의견을 새 응답으로 저장한다.
   *
   * @param userProfileId 응답 사용자 ID
   * @param request NPS 점수와 선택 의견
   */
  @Transactional
  public void submit(Long userProfileId, NpsSubmitRequest request) {
    NpsResponse response = request.toEntity(userProfileId);
    npsResponseRepository.save(response);

    log.info(
        "nps response submitted: userId={}, score={}, opinionPresent={}",
        userProfileId,
        request.score(),
        request.opinionText() != null && !request.opinionText().isBlank());
  }

  /**
   * 관리자용 NPS 응답을 최신순 페이지로 조회한다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 관리자 NPS 목록 페이지
   */
  @Transactional(readOnly = true)
  public AdminNpsResponsePage getAdminResponses(int page, int size) {
    Slice<AdminNpsResponseProjection> responses =
        npsResponseRepository.findAdminResponses(PageRequest.of(page, size));

    return AdminNpsResponsePage.from(responses, page, size);
  }
}
