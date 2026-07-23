// 사용자의 NPS 응답을 정규화해 저장하는 Service다.

package com.landit.landitbe.feature.nps.application;

import com.landit.landitbe.feature.nps.domain.NpsResponse;
import com.landit.landitbe.feature.nps.infrastructure.NpsResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자의 NPS 응답을 정규화해 저장하는 Service다. */
@Service
public class NpsService {

  private final NpsResponseRepository npsResponseRepository;

  /** 동작을 수행한다. */
  public NpsService(NpsResponseRepository npsResponseRepository) {
    this.npsResponseRepository = npsResponseRepository;
  }

  /** 사용자의 만족도 점수와 선택 의견을 새 응답으로 저장한다. */
  @Transactional
  public void submit(Long userProfileId, int score, String opinionText) {
    npsResponseRepository.save(
        new NpsResponse(userProfileId, score, normalizeOpinionText(opinionText)));
  }

  private String normalizeOpinionText(String opinionText) {
    return opinionText == null || opinionText.isBlank() ? null : opinionText;
  }
}
