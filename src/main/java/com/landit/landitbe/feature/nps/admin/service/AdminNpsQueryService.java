// 관리자용 NPS 응답 목록을 조회한다.

package com.landit.landitbe.feature.nps.admin.service;

import com.landit.landitbe.feature.nps.admin.dto.AdminNpsResponsePage;
import com.landit.landitbe.feature.nps.repository.NpsResponseRepository;
import com.landit.landitbe.feature.nps.repository.projection.AdminNpsResponseProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자용 NPS 응답 목록을 조회한다. */
@Service
public class AdminNpsQueryService {

  private final NpsResponseRepository npsResponseRepository;

  /**
   * NPS 응답 저장소를 주입받는다.
   *
   * @param npsResponseRepository NPS 응답 Repository
   */
  public AdminNpsQueryService(NpsResponseRepository npsResponseRepository) {
    this.npsResponseRepository = npsResponseRepository;
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
