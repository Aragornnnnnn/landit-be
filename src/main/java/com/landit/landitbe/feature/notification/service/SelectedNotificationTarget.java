// 사용자에게 발행할 단일 학습 알림 유형과 클릭 대상을 전달한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.feature.notification.domain.NotificationType;

record SelectedNotificationTarget(NotificationType notificationType, Long targetId) {}
