// 새 장기기억 저장 요청의 입력 불변식을 검증한다.

package com.landit.landitbe.feature.memory.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 새 장기기억과 원문 계보 저장에 필요한 입력을 표현한다.
 *
 * @param userProfileId 기억을 소유하는 사용자 프로필 ID
 * @param characterId 기억을 관찰한 캐릭터 ID. PROFILE은 null이다.
 * @param memoryType 기억의 의미 유형
 * @param content 기억으로 저장할 원문
 * @param contentLocale 원문의 언어 지역
 * @param confidence 기억 추출 결과의 신뢰도
 * @param validFrom 기억이 유효해진 시각
 * @param validTo 기억이 유효한 마지막 시각. 현재 유효하면 null이다.
 * @param observedAt 기억 사실을 관찰한 시각
 * @param recordedAt 기억을 저장하는 시각
 * @param extractorVersion 기억 추출기 버전
 * @param embeddingModel 임베딩 모델 식별자
 * @param embedding 1,536차원 임베딩
 */
public record NewConversationMemory(
    long userProfileId,
    String characterId,
    ConversationMemoryType memoryType,
    String content,
    Locale contentLocale,
    double confidence,
    LocalDateTime validFrom,
    LocalDateTime validTo,
    LocalDateTime observedAt,
    LocalDateTime recordedAt,
    String extractorVersion,
    String embeddingModel,
    List<Float> embedding) {

  /** 입력 문자열과 1,536차원 임베딩을 정규화하고 저장 가능한 값인지 검증한다. */
  public NewConversationMemory {
    if (userProfileId <= 0) {
      throw new IllegalArgumentException("사용자 프로필 ID가 유효하지 않습니다.");
    }
    if (memoryType == null || contentLocale == null) {
      throw new IllegalArgumentException("장기기억 유형과 지역은 필수입니다.");
    }

    characterId = characterId == null ? null : characterId.trim();
    content = trimRequired(content, 500, "기억 본문");
    extractorVersion = trimRequired(extractorVersion, Integer.MAX_VALUE, "추출기 버전");
    embeddingModel = trimRequired(embeddingModel, Integer.MAX_VALUE, "임베딩 모델");
    if (memoryType == ConversationMemoryType.PROFILE && characterId != null) {
      throw new IllegalArgumentException("프로필 기억의 범위가 유효하지 않습니다.");
    }
    if (memoryType != ConversationMemoryType.PROFILE
        && (characterId == null || characterId.isBlank())) {
      throw new IllegalArgumentException("이벤트·에피소드 기억에는 캐릭터가 필요합니다.");
    }
    if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
      throw new IllegalArgumentException("기억 신뢰도가 유효하지 않습니다.");
    }
    if ((validFrom == null || observedAt == null || recordedAt == null)
        || (validTo != null && validTo.isBefore(validFrom))) {
      throw new IllegalArgumentException("기억 시각은 필수입니다.");
    }
    if (embedding == null || embedding.size() != 1536) {
      throw new IllegalArgumentException("임베딩 차원이 유효하지 않습니다.");
    }
    for (Float component : embedding) {
      if (component == null || !Float.isFinite(component)) {
        throw new IllegalArgumentException("임베딩 값이 유효하지 않습니다.");
      }
    }
    embedding = List.copyOf(embedding);
  }

  private static String trimRequired(String value, int maxLength, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + "이(가) 필요합니다.");
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty() || trimmed.length() > maxLength) {
      throw new IllegalArgumentException(fieldName + "이(가) 유효하지 않습니다.");
    }
    return trimmed;
  }
}
