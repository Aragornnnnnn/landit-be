// 설치 푸시 동기화에서 동시 등록 충돌을 재시도한다.

package com.landit.landitbe.feature.notification.token.service;

import com.landit.landitbe.feature.notification.token.dto.PushDeviceUpdateRequest;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** 설치 푸시 동기화에서 동시 등록 충돌을 재시도한다. */
@Service
@RequiredArgsConstructor
public class PushDeviceService {
  private final PushDevicePersistenceService persistence;

  /**
   * 설치 상태를 저장하며 신규 Token·설치의 동시 생성 충돌을 한 번 재시도한다.
   *
   * @param userProfileId 인증된 사용자 ID
   * @param installationId 앱 설치 UUID
   * @param request 현재 설치의 푸시 상태
   */
  public void update(Long userProfileId, UUID installationId, PushDeviceUpdateRequest request) {
    try {
      persistence.update(userProfileId, installationId, request);
    } catch (DataIntegrityViolationException exception) {
      try {
        persistence.update(userProfileId, installationId, request);
      } catch (DataIntegrityViolationException retryFailure) {
        throw new ApiException(ErrorCode.CONFLICT);
      }
    }
  }
}
