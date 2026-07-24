// 외부 Push 제공자에 전달할 Token과 사용자 노출 메시지를 정의한다.

package com.landit.landitbe.feature.notification.client;

/**
 * 외부 Push 제공자에 전달할 Token과 사용자 노출 메시지를 정의한다.
 *
 * @param expoPushToken Expo Push Token
 * @param title 알림 제목
 * @param body 알림 본문
 * @param deepLink 앱 이동 경로
 */
public record PushMessage(String expoPushToken, String title, String body, String deepLink) {}
