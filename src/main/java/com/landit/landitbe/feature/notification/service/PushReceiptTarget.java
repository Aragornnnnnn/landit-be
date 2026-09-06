// Expo Receipt를 조회할 발송 이력과 Ticket ID를 정의한다.

package com.landit.landitbe.feature.notification.service;

/**
 * Expo Receipt를 조회할 발송 이력과 Ticket ID를 정의한다.
 *
 * @param pushDeliveryId Push Delivery ID
 * @param ticketId Expo Ticket ID
 */
public record PushReceiptTarget(Long pushDeliveryId, String ticketId) {}
