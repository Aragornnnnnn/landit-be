// 중복 방지 키와 식별자로 푸시 발송 이력을 조회한다.

package com.landit.landitbe.feature.notification.repository;

import com.landit.landitbe.feature.notification.domain.PushDelivery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 중복 방지 키와 식별자로 푸시 발송 이력을 조회한다. */
public interface PushDeliveryRepository extends JpaRepository<PushDelivery, Long> {

  /**
   * 중복 방지 키로 기존 발송 이력을 조회한다.
   *
   * @param deduplicationKey 발송 중복 방지 키
   * @return 기존 발송 이력
   */
  Optional<PushDelivery> findByDeduplicationKey(String deduplicationKey);
}
