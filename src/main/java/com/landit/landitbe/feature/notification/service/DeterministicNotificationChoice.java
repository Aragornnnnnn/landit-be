// 예약 푸시 정책에서 재시작 후에도 같은 후보를 고르는 해시 선택기다.

package com.landit.landitbe.feature.notification.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

/** 예약 푸시 정책에서 재시작 후에도 같은 후보를 고르는 해시 선택기다. */
final class DeterministicNotificationChoice {

  private DeterministicNotificationChoice() {}

  /** 날짜와 사용자를 포함한 canonical key에서 균등한 후보 인덱스를 계산한다. */
  static int choose(String decisionScope, LocalDate scheduledDate, Long userProfileId, int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("후보 수는 0보다 커야 합니다.");
    }
    String canonicalKey =
        "v1|" + decisionScope + "|" + scheduledDate + "|" + userProfileId;
    byte[] digest = sha256(canonicalKey);
    long value = ByteBuffer.wrap(digest, 0, Long.BYTES).getLong();
    return (int) Math.floorMod(value, (long) count);
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
    }
  }
}
