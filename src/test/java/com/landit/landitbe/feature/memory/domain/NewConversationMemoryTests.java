// 장기기억 입력 도메인의 생성 불변식을 검증한다.

package com.landit.landitbe.feature.memory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 장기기억 입력 도메인의 생성 불변식을 검증한다. */
class NewConversationMemoryTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 25, 12, 0);

  @DisplayName("프로필과 사건 기억이 각각 허용된 범위를 사용하면 생성한다.")
  @Test
  void acceptsProfileAndEventWithExpectedScope() {
    NewConversationMemory profile =
        memory(
            fixture -> {
              fixture.characterId = null;
              fixture.memoryType = ConversationMemoryType.PROFILE;
            });
    NewConversationMemory event =
        memory(
            fixture -> {
              fixture.characterId = " chloe ";
            });

    assertThat(profile.characterId()).isNull();
    assertThat(event.characterId()).isEqualTo("chloe");
  }

  @DisplayName("사용자 프로필 ID가 0 이하이면 기억 생성을 거부한다.")
  @Test
  void rejectsNonPositiveUserProfileId() {
    assertInvalid(fixture -> fixture.userProfileId = 0);
  }

  @DisplayName("프로필 기억에 캐릭터 ID가 있으면 거부한다.")
  @Test
  void rejectsProfileWithCharacterId() {
    assertInvalid(fixture -> fixture.memoryType = ConversationMemoryType.PROFILE);
  }

  @DisplayName("사건 및 에피소드 기억에 캐릭터 ID가 없으면 거부한다.")
  @Test
  void rejectsEventAndEpisodeWithoutCharacterId() {
    assertInvalid(fixture -> fixture.characterId = " ");
    assertInvalid(
        fixture -> {
          fixture.characterId = null;
          fixture.memoryType = ConversationMemoryType.EPISODE;
        });
  }

  @DisplayName("기억 유효 종료 시각을 보존하고 역전된 유효 기간은 거부한다.")
  @Test
  void preservesValidToAndRejectsReversedValidity() {
    LocalDateTime validTo = NOW.plusDays(1);
    NewConversationMemory memory = memory(fixture -> fixture.validTo = validTo);

    assertThat(memory.validTo()).isEqualTo(validTo);
    assertThatThrownBy(() -> memory(fixture -> fixture.validTo = NOW.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("기억 유효 종료 시각이 시작 시각보다 빠릅니다.");
  }

  @DisplayName("기억 본문이 비어 있거나 너무 길면 거부한다.")
  @Test
  void rejectsBlankOrTooLongContent() {
    assertInvalid(fixture -> fixture.content = " ");
    assertInvalid(fixture -> fixture.content = "a".repeat(501));
  }

  @DisplayName("유효 범위를 벗어난 기억 신뢰도를 거부한다.")
  @Test
  void rejectsInvalidConfidence() {
    for (double confidence : new double[] {-0.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY}) {
      assertInvalid(fixture -> fixture.confidence = confidence);
    }
  }

  @DisplayName("필수 시간 정보나 메타데이터가 없는 기억을 거부한다.")
  @Test
  void rejectsMissingTemporalValuesAndMetadata() {
    assertInvalid(fixture -> fixture.validFrom = null);
    assertInvalid(
        fixture -> {
          fixture.validTo = NOW.minusSeconds(1);
        });
    assertInvalid(fixture -> fixture.observedAt = null);
    assertInvalid(fixture -> fixture.recordedAt = null);
    assertInvalid(fixture -> fixture.extractorVersion = " ");
    assertInvalid(fixture -> fixture.embeddingModel = " ");
  }

  @DisplayName("기억의 유효 기간이 올바르면 null 종료 시각을 허용한다.")
  @Test
  void preservesNullableValidToWhenTemporalRangeIsValid() {
    NewConversationMemory memory = memory(fixture -> fixture.validTo = NOW.plusDays(1));

    assertThat(memory.validTo()).isEqualTo(NOW.plusDays(1));
  }

  @DisplayName("임베딩의 차원이나 구성 값이 잘못되면 기억 생성을 거부한다.")
  @Test
  void rejectsInvalidEmbeddingShapeAndComponents() {
    assertInvalid(fixture -> fixture.embedding = List.of(0.1f));
    List<Float> withNull = validEmbedding();
    withNull.set(0, null);
    assertInvalid(fixture -> fixture.embedding = withNull);
    List<Float> withNan = validEmbedding();
    withNan.set(0, Float.NaN);
    assertInvalid(fixture -> fixture.embedding = withNan);
    List<Float> withInfinity = validEmbedding();
    withInfinity.set(0, Float.POSITIVE_INFINITY);
    assertInvalid(fixture -> fixture.embedding = withInfinity);
  }

  @DisplayName("기억 본문의 주변 공백을 제거하고 임베딩을 방어적으로 복사한다.")
  @Test
  void trimsTextAndDefensivelyCopiesEmbedding() {
    List<Float> embedding = validEmbedding();
    NewConversationMemory memory =
        memory(
            fixture -> {
              fixture.characterId = " chloe ";
              fixture.content = " remembered content ";
              fixture.extractorVersion = " extractor-v1 ";
              fixture.embeddingModel = " embedding-v1 ";
              fixture.embedding = embedding;
            });

    embedding.set(0, 0.9f);

    assertThat(memory.characterId()).isEqualTo("chloe");
    assertThat(memory.content()).isEqualTo("remembered content");
    assertThat(memory.extractorVersion()).isEqualTo("extractor-v1");
    assertThat(memory.embeddingModel()).isEqualTo("embedding-v1");
    assertThat(memory.embedding().get(0)).isEqualTo(0.1f);
    assertThatThrownBy(() -> memory.embedding().set(0, 0.9f))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private void assertInvalid(Consumer<MemoryFixture> customize) {
    assertThatThrownBy(() -> memory(customize)).isInstanceOf(IllegalArgumentException.class);
  }

  private NewConversationMemory memory(Consumer<MemoryFixture> customize) {
    MemoryFixture fixture = new MemoryFixture();
    customize.accept(fixture);
    return fixture.build();
  }

  private static final class MemoryFixture {
    private long userProfileId = 1L;
    private String characterId = "chloe";
    private ConversationMemoryType memoryType = ConversationMemoryType.EVENT;
    private String content = "remembered content";
    private Locale contentLocale = Locale.ENGLISH;
    private double confidence = 0.8;
    private LocalDateTime validFrom = NOW;
    private LocalDateTime validTo;
    private LocalDateTime observedAt = NOW;
    private LocalDateTime recordedAt = NOW;
    private String extractorVersion = "extractor-v1";
    private String embeddingModel = "embedding-v1";
    private List<Float> embedding = validEmbedding();

    private NewConversationMemory build() {
      return new NewConversationMemory(
          userProfileId,
          characterId,
          memoryType,
          content,
          contentLocale,
          confidence,
          validFrom,
          validTo,
          observedAt,
          recordedAt,
          extractorVersion,
          embeddingModel,
          embedding);
    }
  }

  private static List<Float> validEmbedding() {
    return new ArrayList<>(java.util.Collections.nCopies(1536, 0.1f));
  }
}
