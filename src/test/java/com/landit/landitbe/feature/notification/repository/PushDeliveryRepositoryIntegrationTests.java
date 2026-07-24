// Push Delivery Repository의 중복 방지 계약을 검증한다.

package com.landit.landitbe.feature.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.notification.domain.NotificationType;
import com.landit.landitbe.feature.notification.domain.PushDelivery;
import com.landit.landitbe.feature.notification.domain.PushDevice;
import com.landit.landitbe.shared.domain.AppPlatform;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Push Delivery Repository의 중복 방지 계약을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PushDeliveryRepositoryIntegrationTests {

  private static final long USER_ID = 994001L;
  private static final UUID INSTALLATION_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
  private static final String EXPO_PUSH_TOKEN = "ExponentPushToken[repository-token]";
  private static final String SENT_EXPO_PUSH_TOKEN = "ExponentPushToken[delivery-snapshot]";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManager entityManager;

  @Autowired private PushDeviceRepository pushDeviceRepository;

  @Autowired private PushDeliveryRepository pushDeliveryRepository;

  /** 같은 중복 방지 키를 가진 발송 이력은 한 건만 저장한다. */
  @Test
  void rejectsDuplicateDeliveryKey() {
    seedUser();
    PushDevice device =
        pushDeviceRepository.saveAndFlush(
            PushDevice.create(USER_ID, INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN));
    PushDelivery first = delivery(device.getId());
    PushDelivery duplicate = delivery(device.getId());

    pushDeliveryRepository.saveAndFlush(first);

    assertThatThrownBy(() -> pushDeliveryRepository.saveAndFlush(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  /** 저장된 발송 이력은 해당 발송에 사용한 Expo Token 원문을 보존한다. */
  @Test
  void retainsSentExpoPushTokenAfterPersistence() {
    seedUser();
    PushDevice device =
        pushDeviceRepository.saveAndFlush(
            PushDevice.create(USER_ID, INSTALLATION_ID, AppPlatform.IOS, true, EXPO_PUSH_TOKEN));
    PushDelivery delivery = delivery(device.getId());

    pushDeliveryRepository.saveAndFlush(delivery);
    entityManager.clear();
    PushDelivery persisted =
        pushDeliveryRepository
            .findByDeduplicationKey("review-reminder:2026-07-24:" + USER_ID + ":" + device.getId())
            .orElseThrow();

    assertThat(SENT_EXPO_PUSH_TOKEN.equals(persisted.getSentExpoPushToken())).isTrue();
  }

  /** 테스트용 푸시 발송 이력을 생성한다. */
  private PushDelivery delivery(Long pushDeviceId) {
    return PushDelivery.requested(
        USER_ID,
        pushDeviceId,
        SENT_EXPO_PUSH_TOKEN,
        NotificationType.REVIEW_REMINDER,
        "review-reminder:2026-07-24:" + USER_ID + ":" + pushDeviceId,
        "복습할 시간이에요",
        "오늘의 표현을 다시 볼까요?",
        "/expressions",
        LocalDateTime.of(2026, 7, 24, 20, 0));
  }

  /** 테스트용 활성 사용자를 저장한다. */
  private void seedUser() {
    jdbcTemplate.update(
        """
        insert into user_profile (
            id,
            nickname,
            target_locale,
            base_locale,
            current_level,
            push_permission_status,
            status,
            created_at,
            updated_at
        )
        values (?, 'push-user', 'EN', 'KR', 1, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        USER_ID);
  }
}
