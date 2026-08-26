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
  void rejectsBlankOrTooLongContent() {
    assertInvalid(fixture -> fixture.content = " ");
    assertInvalid(fixture -> fixture.content = "a".repeat(501));
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
