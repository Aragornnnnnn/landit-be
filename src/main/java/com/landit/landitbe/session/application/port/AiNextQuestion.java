// BE가 지정한 다음 고정 질문 정보를 AI 요청에 담는다.
package com.landit.landitbe.session.application.port;

public record AiNextQuestion(Long questionId, int sequence, String questionEn, String questionKo) {}
