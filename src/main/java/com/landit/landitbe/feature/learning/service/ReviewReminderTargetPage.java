// 복습 리마인더 발송 대상 사용자의 Cursor 페이지를 전달한다.

package com.landit.landitbe.feature.learning.service;

import java.util.List;

/**
 * 복습 리마인더 발송 대상 사용자의 Cursor 페이지를 전달한다.
 *
 * @param userProfileIds 현재 페이지의 활성 사용자 ID
 * @param nextUserProfileId 다음 페이지 조회에 사용할 마지막 후보 사용자 ID. 다음 페이지가 없으면 {@code null}
 */
public record ReviewReminderTargetPage(List<Long> userProfileIds, Long nextUserProfileId) {}
