// 사용자의 NPS 응답을 정규화해 저장하는 Service다.
package com.landit.landitbe.nps.application;

import com.landit.landitbe.nps.domain.NpsResponse;
import com.landit.landitbe.nps.infrastructure.NpsResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NpsService {

    private final NpsResponseRepository npsResponseRepository;

    public NpsService(NpsResponseRepository npsResponseRepository) {
        this.npsResponseRepository = npsResponseRepository;
    }

    /** 사용자의 만족도 점수와 선택 의견을 새 응답으로 저장한다. */
    @Transactional
    public void submit(Long userProfileId, int score, String opinionText) {
        npsResponseRepository.save(new NpsResponse(userProfileId, score, normalizeOpinionText(opinionText)));
    }

    private String normalizeOpinionText(String opinionText) {
        return opinionText == null || opinionText.isBlank() ? null : opinionText;
    }
}
