// 프리톡 대화 핵심 추출과 임베딩 결과를 담는다.

package com.landit.landitbe.feature.session.client.ai;

import java.util.List;

/**
 * 프리톡 대화 핵심 추출과 임베딩 결과를 담는다.
 *
 * @param excerpts 추출된 핵심 발화와 임베딩 목록 (1~4건)
 */
public record AiConversationEmbeddingsResult(List<AiConversationExcerpt> excerpts) {}
