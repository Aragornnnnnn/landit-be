// 완료된 프리톡 왕복을 기준으로 요약할 연속 원문 구간을 선택한다.

package com.landit.landitbe.feature.learning.freetalk.context.service;

import com.landit.landitbe.feature.learning.conversation.domain.FreeTalkTurnStatus;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

final class FreeTalkSummaryWindow {
  private FreeTalkSummaryWindow() {}

  static List<List<SessionHistoryMessageSnapshot>> rounds(
      List<SessionHistoryMessageSnapshot> messages, int covered) {
    List<List<SessionHistoryMessageSnapshot>> rounds = new ArrayList<>();
    List<SessionHistoryMessageSnapshot> pending = new ArrayList<>();
    boolean waitingForAi = false;
    for (SessionHistoryMessageSnapshot message : messages) {
      if (message.getMessageSequence() <= covered) {
        continue;
      }
      if (message.getRole() == ConversationSpeaker.USER) {
        if (waitingForAi || !completed(message)) {
          break;
        }
        pending.add(message);
        waitingForAi = true;
      } else if (message.getRole() == ConversationSpeaker.AI && waitingForAi) {
        pending.add(message);
        rounds.add(List.copyOf(pending));
        pending.clear();
        waitingForAi = false;
      } else if (covered == 0 && rounds.isEmpty() && pending.isEmpty()) {
        pending.add(message);
      } else {
        break;
      }
    }
    return List.copyOf(rounds);
  }

  private static boolean completed(SessionHistoryMessageSnapshot message) {
    return message.getFreeTalkTurnStatus() == FreeTalkTurnStatus.CONTINUE
        || message.getFreeTalkTurnStatus() == FreeTalkTurnStatus.COMPLETED;
  }

  static List<SessionHistoryMessageSnapshot> source(
      List<List<SessionHistoryMessageSnapshot>> rounds,
      int byteLimit,
      ToIntFunction<List<SessionHistoryMessageSnapshot>> bytes) {
    List<SessionHistoryMessageSnapshot> source = new ArrayList<>();
    for (List<SessionHistoryMessageSnapshot> round : rounds) {
      List<SessionHistoryMessageSnapshot> next = new ArrayList<>(source);
      next.addAll(round);
      if (!source.isEmpty() && bytes.applyAsInt(next) > byteLimit) {
        break;
      }
      source = next;
    }
    return List.copyOf(source);
  }
}
