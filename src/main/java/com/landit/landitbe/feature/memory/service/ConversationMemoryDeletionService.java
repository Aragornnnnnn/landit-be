// 사용자 탈퇴 시 장기기억과 검색 계보를 제거하고 최소 지표를 기록한다.

package com.landit.landitbe.feature.memory.service;

import com.landit.landitbe.feature.memory.repository.ConversationMemoryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 탈퇴에 필요한 장기기억 삭제 경계를 제공한다. */
@Service
public class ConversationMemoryDeletionService {

  private final ConversationMemoryRepository memoryRepository;
  private final MeterRegistry meterRegistry;

  /**
   * 장기기억 삭제 저장소와 관측성 레지스트리를 주입받는다.
   *
   * @param memoryRepository 장기기억 원문과 lineage를 삭제할 저장소
   * @param meterRegistry 삭제 결과를 기록할 지표 레지스트리
   */
  public ConversationMemoryDeletionService(
      ConversationMemoryRepository memoryRepository, MeterRegistry meterRegistry) {
    this.memoryRepository = memoryRepository;
    this.meterRegistry = meterRegistry;
  }

  /**
   * 사용자의 기억 원문·source·프리톡 검색 trace를 삭제한다.
   *
   * @param userProfileId 삭제할 사용자의 프로필 ID
   */
  @Transactional
  public void deleteAllByUserProfileId(long userProfileId) {
    int deletedCount = memoryRepository.deleteAllByUserProfileId(userProfileId);
    meterRegistry.counter("landit.memory.deleted").increment(deletedCount);
  }
}
