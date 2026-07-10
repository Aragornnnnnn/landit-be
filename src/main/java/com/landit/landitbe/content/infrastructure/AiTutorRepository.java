// AI 튜터 엔티티를 locale과 상태 기준으로 조회한다.
package com.landit.landitbe.content.infrastructure;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.content.domain.AiTutor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiTutorRepository extends JpaRepository<AiTutor, Long> {

    /** locale과 활성 상태가 일치하는 AI 튜터 후보를 모두 조회한다. */
    List<AiTutor> findAllByAccentLocaleAndTargetLocaleAndStatus(
            String accentLocale,
            String targetLocale,
            ActiveStatus status
    );
}
