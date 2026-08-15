// 공용 대화 캐릭터와 TTS 음성 연결을 조회한다.

package com.landit.landitbe.feature.content.repository;

import com.landit.landitbe.feature.content.domain.ConversationCharacter;
import com.landit.landitbe.shared.domain.ActiveStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 공용 대화 캐릭터와 TTS 음성 연결 조회를 소유한다. */
public interface ConversationCharacterRepository
    extends JpaRepository<ConversationCharacter, String> {

  /**
   * 공개 식별자와 상태가 일치하는 캐릭터를 조회한다.
   *
   * @param characterId 공개 캐릭터 식별자
   * @param status 조회할 활성 상태
   * @return 조건에 맞는 캐릭터, 없으면 빈 Optional
   */
  Optional<ConversationCharacter> findByCharacterIdAndStatus(
      String characterId, ActiveStatus status);
}
