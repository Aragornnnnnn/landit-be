// 프리톡 AI 요청에서 사용할 주제 정보를 담는다.

package com.landit.landitbe.feature.session.client.ai;

/**
 * 프리톡 AI 요청에서 사용할 주제 정보를 담는다.
 *
 * @param topicId 활성 추천 주제 ID
 * @param title 프리톡 제목
 * @param promptDescription AI 생성에 사용할 주제 설명
 */
public record AiFreeTalkTopic(Long topicId, String title, String promptDescription) {}
