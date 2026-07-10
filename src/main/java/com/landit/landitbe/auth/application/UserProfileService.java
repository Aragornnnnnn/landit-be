// 사용자 프로필의 조회성 보조 기능(학습 locale 등)을 다른 모듈에 제공한다.
package com.landit.landitbe.auth.application;

import com.landit.landitbe.auth.domain.UserProfile;
import com.landit.landitbe.auth.domain.UserProfileStatus;
import com.landit.landitbe.auth.infrastructure.UserProfileRepository;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    /** 활성 사용자의 학습 locale(target/base)을 조회한다. 활성 사용자가 아니면 INVALID_TOKEN 예외를 던진다. */
    @Transactional(readOnly = true)
    public UserLocale getUserLocale(Long userId) {
        UserProfile userProfile = userProfileRepository.findByIdAndStatus(userId, UserProfileStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));

        return new UserLocale(userProfile.getTargetLocale(), userProfile.getBaseLocale());
    }
}
