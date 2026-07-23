// AiTutorService가 활성 AI 튜터 후보 수를 검증하는지 확인한다.

package com.landit.landitbe.feature.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.content.domain.AiTutor;
import com.landit.landitbe.feature.content.repository.AiTutorRepository;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.domain.ActiveStatus;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** AiTutorService가 활성 AI 튜터 후보 수를 검증하는지 확인한다. */
@ExtendWith(MockitoExtension.class)
class AiTutorServiceTest {

  @Mock private AiTutorRepository aiTutorRepository;

  @InjectMocks private AiTutorService aiTutorService;

  /** 조건에 맞는 활성 튜터가 하나이면 해당 ID를 반환한다. */
  @Test
  void returnsOnlyActiveTutorId() {
    AiTutor aiTutor = mock(AiTutor.class);
    when(aiTutor.getId()).thenReturn(10L);
    when(aiTutorRepository.findAllByAccentLocaleAndTargetLocaleAndStatus(
            AccentLocale.EN_US, Locale.EN, ActiveStatus.ACTIVE))
        .thenReturn(List.of(aiTutor));

    assertThat(aiTutorService.requireSingleActiveTutorId(AccentLocale.EN_US, Locale.EN))
        .isEqualTo(10L);
  }

  /** 활성 튜터 후보가 정확히 하나가 아니면 설정 오류를 반환한다. */
  @Test
  void rejectsMissingActiveTutor() {
    when(aiTutorRepository.findAllByAccentLocaleAndTargetLocaleAndStatus(
            AccentLocale.EN_US, Locale.EN, ActiveStatus.ACTIVE))
        .thenReturn(List.of());

    assertThatThrownBy(
            () -> aiTutorService.requireSingleActiveTutorId(AccentLocale.EN_US, Locale.EN))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DEFAULT_AI_TUTOR_NOT_CONFIGURED);
  }
}
