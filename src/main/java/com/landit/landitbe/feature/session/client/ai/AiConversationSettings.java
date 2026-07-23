// AI 대화 생성 요청에 필요한 설정 값을 제공한다.

package com.landit.landitbe.feature.session.client.ai;

/** AI 대화 생성 요청에 필요한 설정 값을 제공한다. */
public interface AiConversationSettings {

  /** AI 서버가 대화 생성 기준으로 사용할 서비스 사용자군을 반환한다. */
  String serviceAudience();
}
