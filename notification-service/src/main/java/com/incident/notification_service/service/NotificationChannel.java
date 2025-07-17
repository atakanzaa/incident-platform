package com.incident.notification_service.service;

import com.incident.notification_service.model.Alert;

public interface NotificationChannel {
    void sendNotification(Alert alert) throws Exception;
    boolean isEnabled();
    String getChannelName();
} 