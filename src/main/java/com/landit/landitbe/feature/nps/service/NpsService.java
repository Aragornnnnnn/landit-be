// 사용자의 NPS 응답을 정규화해 저장하는 Service다.

package com.landit.landitbe.feature.nps.service;

import com.landit.landitbe.feature.nps.domain.NpsResponse;
import com.landit.landitbe.feature.nps.dto.NpsSubmitRequest;
import com.landit.landitbe.feature.nps.repository.NpsResponseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자의 NPS 응답을 정규화해 저장하는 Service다. */
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
}
