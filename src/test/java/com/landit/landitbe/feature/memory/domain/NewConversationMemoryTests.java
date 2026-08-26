// 장기기억 입력 도메인의 생성 불변식을 검증한다.

package com.landit.landitbe.feature.memory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** 장기기억 입력 도메인의 생성 불변식을 검증한다. */
class NewConversationMemoryTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 25, 12, 0);

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

  @Test
  void rejectsNonPositiveUserProfileId() {
    assertInvalid(fixture -> fixture.userProfileId = 0);
  }

  @Test
  void rejectsProfileWithCharacterId() {
    assertInvalid(fixture -> fixture.memoryType = ConversationMemoryType.PROFILE);
  }

  @Test
  void rejectsEventAndEpisodeWithoutCharacterId() {
    assertInvalid(fixture -> fixture.characterId = " ");
    assertInvalid(
        fixture -> {
          fixture.characterId = null;
          fixture.memoryType = ConversationMemoryType.EPISODE;
        });
  }

  @Test
  void preservesValidToAndRejectsReversedValidity() {
    LocalDateTime validTo = NOW.plusDays(1);
    NewConversationMemory memory = memory(fixture -> fixture.validTo = validTo);

    assertThat(memory.validTo()).isEqualTo(validTo);
    assertThatThrownBy(() -> memory(fixture -> fixture.validTo = NOW.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("기억 유효 종료 시각이 시작 시각보다 빠릅니다.");
  }

  @Test
  void rejectsBlankOrTooLongContent() {
    assertInvalid(fixture -> fixture.content = " ");
    assertInvalid(fixture -> fixture.content = "a".repeat(501));
  }

  @Test
  void rejectsInvalidConfidence() {
    for (double confidence : new double[] {-0.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY}) {
      assertInvalid(fixture -> fixture.confidence = confidence);
    }
  }

  @Test
  void rejectsMissingTemporalValuesAndMetadata() {
    assertInvalid(fixture -> fixture.validFrom = null);
    assertInvalid(fixture -> fixture.observedAt = null);
    assertInvalid(fixture -> fixture.recordedAt = null);
    assertInvalid(fixture -> fixture.extractorVersion = " ");
    assertInvalid(fixture -> fixture.embeddingModel = " ");
  }

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
