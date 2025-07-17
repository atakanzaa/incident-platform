package com.incident.notification_service.service;

import com.incident.notification_service.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${rabbitmq.queues.notifications}")
    public void handleNotificationAlert(
            @Payload Alert alert,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(name = "alertId", required = false) String alertId,
            @Header(name = "severity", required = false) String severity,
            @Header(name = "serviceName", required = false) String serviceName,
            Channel channel
    ) {
        log.info("Received notification alert: {} from service: {} with severity: {}", 
            alert.getAlertId(), alert.getServiceName(), alert.getSeverity());

        try {
            // Send notifications through all enabled channels
            notificationService.sendNotification(alert);
            
            // Acknowledge the message
            channel.basicAck(deliveryTag, false);
            
            log.debug("Notification alert processed and acknowledged: {}", alert.getAlertId());
            
        } catch (Exception e) {
            log.error("Error processing notification alert: {}", alert.getAlertId(), e);
            
            try {
                // Reject the message and don't requeue (to avoid infinite loops)
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ackError) {
                log.error("Error acknowledging failed message: {}", alert.getAlertId(), ackError);
            }
        }
    }

    @RabbitListener(queues = "${rabbitmq.queues.critical-alerts}")
    public void handleCriticalAlert(
            @Payload Alert alert,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(name = "urgent", required = false) String urgent,
            Channel channel
    ) {
        log.warn("Received CRITICAL alert: {} from service: {}", 
            alert.getAlertId(), alert.getServiceName());

        try {
            // For critical alerts, we might want additional processing
            // Like immediate escalation, multiple retries, etc.
            notificationService.sendNotification(alert);
            
            // You could add additional logic here for critical alerts:
            // - Immediate phone calls
            // - Escalation to on-call personnel
            // - Integration with incident management systems
            
            channel.basicAck(deliveryTag, false);
            
            log.info("Critical alert processed successfully: {}", alert.getAlertId());
            
        } catch (Exception e) {
            log.error("Error processing critical alert: {}", alert.getAlertId(), e);
            
            try {
                // For critical alerts, you might want to requeue for retry
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ackError) {
                log.error("Error handling failed critical alert: {}", alert.getAlertId(), ackError);
            }
        }
    }
} 